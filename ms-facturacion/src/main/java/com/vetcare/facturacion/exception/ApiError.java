package com.vetcare.facturacion.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Formato estándar de error de la API (HU-079).
 */
public record ApiError(Instant timestamp, int status, String error, String mensaje, String ruta,
                       Map<String, String> detalles) {

    public static ApiError de(HttpStatus estado, String mensaje, String ruta, Map<String, String> detalles) {
        return new ApiError(Instant.now(), estado.value(), estado.getReasonPhrase(), mensaje, ruta, detalles);
    }

    public static ApiError de(HttpStatus estado, String mensaje, String ruta) {
        return de(estado, mensaje, ruta, Map.of());
    }
}
