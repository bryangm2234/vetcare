package com.vetcare.historia.client;

import com.vetcare.historia.dto.CitaInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Consulta y actualiza citas en ms-agenda con el token del usuario que hace la petición.
 */
@Component
public class AgendaClient {

    private static final String SERVICIO = "ms-agenda";

    private final RestClient rest;

    public AgendaClient(RestClient.Builder builder, @Value("${vetcare.servicios.agenda-url}") String url) {
        this.rest = builder.baseUrl(url).build();
    }

    public CitaInfo cita(String id) {
        return LlamadaRemota.ejecutar(SERVICIO, () -> rest.get().uri("/api/citas/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .retrieve().body(CitaInfo.class));
    }

    public void marcarAtendida(String id) {
        LlamadaRemota.ejecutar(SERVICIO, () -> rest.patch().uri("/api/citas/{id}/atencion", id)
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .retrieve().toBodilessEntity());
    }
}
