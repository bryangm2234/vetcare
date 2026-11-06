package com.vetcare.farmacia.client;

import com.vetcare.farmacia.dto.FormulaVigencia;
import com.vetcare.farmacia.dto.ItemDespachoRequest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Consulta y actualiza fórmulas en ms-historia-clinica con el token del usuario (RN-02).
 */
@Component
public class HistoriaClient {

    private static final String SERVICIO = "ms-historia-clinica";

    private final RestClient rest;

    public HistoriaClient(RestClient.Builder builder, @Value("${vetcare.servicios.historia-url}") String url) {
        this.rest = builder.baseUrl(url).build();
    }

    public FormulaVigencia vigencia(String formulaId) {
        return LlamadaRemota.ejecutar(SERVICIO, () -> rest.get().uri("/api/formulas/{id}/vigencia", formulaId)
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .retrieve().body(FormulaVigencia.class));
    }

    public void registrarDespacho(String formulaId, List<ItemDespachoRequest> items) {
        LlamadaRemota.ejecutar(SERVICIO, () -> rest.patch().uri("/api/formulas/{id}/despachos", formulaId)
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("items", items))
                .retrieve().toBodilessEntity());
    }
}
