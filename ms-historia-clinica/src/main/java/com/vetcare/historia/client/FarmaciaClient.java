package com.vetcare.historia.client;

import com.vetcare.historia.dto.MedicamentoInfo;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Consulta el catálogo y descuenta las vacunas aplicadas en ms-farmacia (D11).
 */
@Component
public class FarmaciaClient {

    private static final String SERVICIO = "ms-farmacia";

    private final RestClient rest;

    public FarmaciaClient(RestClient.Builder builder, @Value("${vetcare.servicios.farmacia-url}") String url) {
        this.rest = builder.baseUrl(url).build();
    }

    public MedicamentoInfo medicamento(String id) {
        return LlamadaRemota.ejecutar(SERVICIO, () -> rest.get().uri("/api/medicamentos/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .retrieve().body(MedicamentoInfo.class));
    }

    public void descontarVacuna(String lote) {
        LlamadaRemota.ejecutar(SERVICIO, () -> rest.post().uri("/api/lotes/{lote}/consumos", lote)
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("cantidad", 1))
                .retrieve().toBodilessEntity());
    }
}
