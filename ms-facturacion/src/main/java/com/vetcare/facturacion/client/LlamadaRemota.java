package com.vetcare.facturacion.client;

import com.vetcare.facturacion.exception.ConflictoException;
import com.vetcare.facturacion.exception.RecursoNoEncontradoException;
import com.vetcare.facturacion.exception.ReglaNegocioException;
import com.vetcare.facturacion.exception.ServicioNoDisponibleException;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Traduce las fallas de otro microservicio a errores de la API y reintenta una vez si no hubo conexión (D14).
 */
final class LlamadaRemota {

    private static final int INTENTOS = 2;

    private LlamadaRemota() {
    }

    static <T> T ejecutar(String servicio, Supplier<T> llamada) {
        for (int intento = 1; ; intento++) {
            try {
                return llamada.get();
            } catch (ResourceAccessException ex) {
                if (intento == INTENTOS) {
                    throw noDisponible(servicio);
                }
            } catch (HttpServerErrorException ex) {
                throw noDisponible(servicio);
            } catch (HttpClientErrorException ex) {
                throw traducir(servicio, ex);
            }
        }
    }

    static String autorizacion() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            return "Bearer " + jwt.getToken().getTokenValue();
        }
        throw new IllegalStateException("No hay un usuario autenticado para llamar a otro microservicio");
    }

    private static RuntimeException traducir(String servicio, HttpClientErrorException ex) {
        HttpStatus estado = HttpStatus.resolve(ex.getStatusCode().value());
        if (estado == HttpStatus.NOT_FOUND) {
            return new RecursoNoEncontradoException("El recurso no existe en " + servicio);
        }
        if (estado == HttpStatus.FORBIDDEN) {
            return new AccessDeniedException("Acceso denegado por " + servicio);
        }
        if (estado == HttpStatus.CONFLICT) {
            return new ConflictoException("La operación fue rechazada por conflicto en " + servicio);
        }
        if (estado == HttpStatus.UNPROCESSABLE_ENTITY) {
            return new ReglaNegocioException("La operación fue rechazada por una regla de " + servicio);
        }
        return noDisponible(servicio);
    }

    private static ServicioNoDisponibleException noDisponible(String servicio) {
        return new ServicioNoDisponibleException("El servicio " + servicio + " no está disponible");
    }
}
