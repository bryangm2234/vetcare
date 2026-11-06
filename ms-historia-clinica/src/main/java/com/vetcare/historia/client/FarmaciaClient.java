package com.vetcare.historia.client;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Descuenta del lote las vacunas aplicadas (D11). Hasta que exista ms-farmacia, sin FARMACIA_URL no descuenta.
 */
@Component
public class FarmaciaClient {

    private static final Logger log = LoggerFactory.getLogger(FarmaciaClient.class);

    private final RestClient rest;
    private final boolean configurado;

    public FarmaciaClient(RestClient.Builder builder, @Value("${vetcare.servicios.farmacia-url:}") String url) {
        this.configurado = !url.isBlank();
        this.rest = configurado ? builder.baseUrl(url).build() : null;
    }

    /** Devuelve true si el inventario se descontó. */
    public boolean descontarVacuna(String lote) {
        if (!configurado) {
            log.warn("FARMACIA_URL no está configurada: no se descontó el lote {}", lote);
            return false;
        }
        LlamadaRemota.ejecutar("ms-farmacia", () -> rest.post().uri("/api/lotes/{lote}/consumos", lote)
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("cantidad", 1))
                .retrieve().toBodilessEntity());
        return true;
    }
}
