package com.vetcare.facturacion.service;

import com.vetcare.facturacion.dto.IngresosResponse;
import com.vetcare.facturacion.exception.DatosInvalidosException;
import com.vetcare.facturacion.model.Factura;
import com.vetcare.facturacion.model.ItemFactura;
import com.vetcare.facturacion.model.MedioPago;
import com.vetcare.facturacion.model.Pago;
import com.vetcare.facturacion.model.TipoServicio;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Ingresos por período, servicio y medio de pago (HU-069).
 */
@Service
public class ReporteIngresosService {

    private final MongoTemplate mongoTemplate;

    public ReporteIngresosService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public IngresosResponse ingresos(LocalDate desde, LocalDate hasta, TipoServicio servicio, MedioPago medioPago) {
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("La fecha inicial es posterior a la final");
        }
        Instant inicio = desde.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant fin = hasta.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Factura> facturas = mongoTemplate.find(new Query(Criteria.where("pagos.fecha").gte(inicio).lt(fin)), Factura.class);

        Map<String, Long> porServicio = new TreeMap<>();
        Map<String, Long> porMedio = new TreeMap<>();
        long total = 0;
        for (Factura factura : facturas) {
            for (Pago pago : factura.getPagos()) {
                boolean cuenta = !pago.isRevertido() && !pago.getFecha().isBefore(inicio) && pago.getFecha().isBefore(fin)
                        && (medioPago == null || pago.getMedio() == medioPago);
                if (!cuenta) {
                    continue;
                }
                long aportado = 0;
                for (ItemFactura item : factura.getItems()) {
                    if (servicio == null || item.tipo() == servicio) {
                        long parte = parteDe(pago.getValor(), item.subtotal(), factura.getSubtotal());
                        porServicio.merge(item.tipo().name(), parte, Long::sum);
                        aportado += parte;
                    }
                }
                long valor = servicio == null ? pago.getValor() : aportado;
                porMedio.merge(pago.getMedio().name(), valor, Long::sum);
                total += valor;
            }
        }
        return new IngresosResponse(total, porServicio, porMedio);
    }

    /** D19: reparte el pago en proporción al valor de los ítems. */
    static long parteDe(long pago, long subtotalItem, long subtotalFactura) {
        return subtotalFactura == 0 ? 0 : Math.round((double) pago * subtotalItem / subtotalFactura);
    }
}
