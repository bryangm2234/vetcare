package com.vetcare.facturacion.client;

import com.vetcare.facturacion.dto.ItemPendiente;
import com.vetcare.facturacion.model.ItemFactura;
import com.vetcare.facturacion.model.TipoServicio;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Recoge los ítems pendientes de los demás microservicios y los marca como facturados.
 */
@Component
public class ItemsClient {

    private record Origen(String servicio, RestClient rest, String ruta) {
    }

    private final Map<TipoServicio, Origen> origenes = new EnumMap<>(TipoServicio.class);

    public ItemsClient(RestClient.Builder builder,
                       @Value("${vetcare.servicios.agenda-url}") String agendaUrl,
                       @Value("${vetcare.servicios.historia-url}") String historiaUrl,
                       @Value("${vetcare.servicios.farmacia-url}") String farmaciaUrl) {
        RestClient agenda = builder.clone().baseUrl(agendaUrl).build();
        RestClient historia = builder.clone().baseUrl(historiaUrl).build();
        RestClient farmacia = builder.clone().baseUrl(farmaciaUrl).build();
        origenes.put(TipoServicio.CONSULTA, new Origen("ms-historia-clinica", historia, "/api/consultas"));
        origenes.put(TipoServicio.LABORATORIO, new Origen("ms-historia-clinica", historia, "/api/ordenes-laboratorio"));
        origenes.put(TipoServicio.HOSPITALIZACION, new Origen("ms-historia-clinica", historia, "/api/hospitalizaciones"));
        origenes.put(TipoServicio.MEDICAMENTOS, new Origen("ms-farmacia", farmacia, "/api/despachos"));
        origenes.put(TipoServicio.PELUQUERIA, new Origen("ms-agenda", agenda, "/api/peluqueria"));
    }

    /** Si algún servicio no responde, la excepción impide generar la factura. */
    public List<ItemPendiente> pendientesDe(String mascotaId) {
        List<ItemPendiente> items = new ArrayList<>();
        origenes.values().forEach(origen -> {
            ItemPendiente[] recibidos = LlamadaRemota.ejecutar(origen.servicio(), () -> origen.rest().get().uri(origen.ruta() + "/items-pendientes")
                    .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                    .retrieve().body(ItemPendiente[].class));
            Arrays.stream(recibidos).filter(i -> mascotaId.equals(i.mascotaId())).forEach(items::add);
        });
        return items;
    }

    /** Marca o desmarca por servicio; si uno falla, deshace los ya aplicados. */
    public void marcar(List<ItemFactura> items, boolean facturado) {
        Map<TipoServicio, List<String>> porTipo = items.stream()
                .collect(Collectors.groupingBy(ItemFactura::tipo, Collectors.mapping(ItemFactura::origenId, Collectors.toList())));
        List<Map.Entry<TipoServicio, List<String>>> aplicados = new ArrayList<>();
        try {
            for (Map.Entry<TipoServicio, List<String>> grupo : porTipo.entrySet()) {
                enviar(grupo.getKey(), grupo.getValue(), facturado);
                aplicados.add(grupo);
            }
        } catch (RuntimeException ex) {
            aplicados.forEach(g -> intentarDeshacer(g.getKey(), g.getValue(), !facturado));
            throw ex;
        }
    }

    private void intentarDeshacer(TipoServicio tipo, List<String> ids, boolean facturado) {
        try {
            enviar(tipo, ids, facturado);
        } catch (RuntimeException ignorada) {
            // Se conserva el error original; el servicio que falló no quedó con cambios.
        }
    }

    private void enviar(TipoServicio tipo, List<String> ids, boolean facturado) {
        Origen origen = origenes.get(tipo);
        LlamadaRemota.ejecutar(origen.servicio(), () -> origen.rest().patch().uri(origen.ruta() + "/items-facturados")
                .header(HttpHeaders.AUTHORIZATION, LlamadaRemota.autorizacion())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("ids", ids, "facturado", facturado))
                .retrieve().toBodilessEntity());
    }
}
