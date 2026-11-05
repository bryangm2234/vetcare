package com.vetcare.agenda.client;

import com.vetcare.agenda.dto.MascotaInfo;
import com.vetcare.agenda.dto.PropietarioInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Consulta ms-pacientes con el token del usuario que hace la petición.
 */
@Component
public class PacientesClient {

    private static final String SERVICIO = "ms-pacientes";

    private final RestClient rest;

    public PacientesClient(RestClient.Builder builder, @Value("${vetcare.servicios.pacientes-url}") String url) {
        this.rest = builder.baseUrl(url).build();
    }

    public MascotaInfo mascota(String id) {
        return LlamadaRemota.ejecutar(SERVICIO, () -> rest.get().uri("/api/mascotas/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .retrieve().body(MascotaInfo.class));
    }

    public MascotaInfo[] mascotasPropias() {
        return LlamadaRemota.ejecutar(SERVICIO, () -> rest.get().uri("/api/portal/mascotas")
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .retrieve().body(MascotaInfo[].class));
    }

    public PropietarioInfo propietario(String id) {
        return LlamadaRemota.ejecutar(SERVICIO, () -> rest.get().uri("/api/propietarios/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .retrieve().body(PropietarioInfo.class));
    }
}
