package com.vetcare.historia.client;

import com.vetcare.historia.dto.MascotaInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Consulta ms-pacientes con el token del usuario que hace la petición.
 */
@Component
public class PacientesClient {

    private final RestClient rest;

    public PacientesClient(RestClient.Builder builder, @Value("${vetcare.servicios.pacientes-url}") String url) {
        this.rest = builder.baseUrl(url).build();
    }

    public MascotaInfo[] mascotasPropias() {
        return LlamadaRemota.ejecutar("ms-pacientes", () -> rest.get().uri("/api/portal/mascotas")
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .retrieve().body(MascotaInfo[].class));
    }

    public MascotaInfo mascota(String id) {
        return LlamadaRemota.ejecutar("ms-pacientes", () -> rest.get().uri("/api/mascotas/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .retrieve().body(MascotaInfo.class));
    }
}
