package com.vetcare.agenda.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vetcare.agenda.dto.MascotaInfo;
import com.vetcare.agenda.exception.RecursoNoEncontradoException;
import com.vetcare.agenda.exception.ServicioNoDisponibleException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PacientesClientTest {

    private static final String URL = "http://pacientes.test/api/mascotas/m1";

    private MockRestServiceServer servidor;
    private PacientesClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new PacientesClient(builder, "http://pacientes.test");
        Jwt jwt = new Jwt("token-usuario", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "RS256"), Map.of("sub", "u1"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reenviaElTokenYLeeLaMascota() {
        servidor.expect(requestTo(URL))
                .andExpect(header("Authorization", "Bearer token-usuario"))
                .andRespond(withSuccess("""
                        {"id":"m1","nombre":"Max","propietarioId":"p1","estado":"ACTIVA","especie":"Canino"}""", MediaType.APPLICATION_JSON));

        MascotaInfo mascota = cliente.mascota("m1");

        assertThat(mascota.nombre()).isEqualTo("Max");
        assertThat(mascota.estado()).isEqualTo("ACTIVA");
        servidor.verify();
    }

    @Test
    void mascotaInexistenteResponde404() {
        servidor.expect(requestTo(URL)).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> cliente.mascota("m1")).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void conservaElMensajeDelServicioRemoto() {
        servidor.expect(requestTo(URL)).andRespond(withResourceNotFound()
                .contentType(MediaType.APPLICATION_JSON).body("""
                        {"status":404,"mensaje":"La mascota no existe"}"""));

        assertThatThrownBy(() -> cliente.mascota("m1"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La mascota no existe");
    }

    @Test
    void mascotaAjenaResponde403() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> cliente.mascota("m1")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void servicioCaidoResponde503() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> cliente.mascota("m1")).isInstanceOf(ServicioNoDisponibleException.class);
    }
}
