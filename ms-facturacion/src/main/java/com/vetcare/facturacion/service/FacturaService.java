package com.vetcare.facturacion.service;

import com.vetcare.facturacion.client.ItemsClient;
import com.vetcare.facturacion.client.PacientesClient;
import com.vetcare.facturacion.dto.AjustesFacturaRequest;
import com.vetcare.facturacion.dto.FacturaRequest;
import com.vetcare.facturacion.dto.ItemPagoRequest;
import com.vetcare.facturacion.dto.ItemPendiente;
import com.vetcare.facturacion.dto.MascotaInfo;
import com.vetcare.facturacion.dto.PaginaResponse;
import com.vetcare.facturacion.exception.ConflictoException;
import com.vetcare.facturacion.exception.DatosInvalidosException;
import com.vetcare.facturacion.exception.RecursoNoEncontradoException;
import com.vetcare.facturacion.exception.ReglaNegocioException;
import com.vetcare.facturacion.model.Caja;
import com.vetcare.facturacion.model.EstadoCaja;
import com.vetcare.facturacion.model.EstadoFactura;
import com.vetcare.facturacion.model.Factura;
import com.vetcare.facturacion.model.ItemFactura;
import com.vetcare.facturacion.model.MedioPago;
import com.vetcare.facturacion.model.Pago;
import com.vetcare.facturacion.model.Tarifa;
import com.vetcare.facturacion.model.TipoServicio;
import com.vetcare.facturacion.repository.CajaRepository;
import com.vetcare.facturacion.repository.FacturaRepository;
import com.vetcare.facturacion.repository.TarifaRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final TarifaRepository tarifaRepository;
    private final CajaRepository cajaRepository;
    private final CajaService cajaService;
    private final ItemsClient itemsClient;
    private final PacientesClient pacientesClient;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoriaService;
    private final int tasaImpuesto;

    public FacturaService(FacturaRepository facturaRepository, TarifaRepository tarifaRepository, CajaRepository cajaRepository,
                          CajaService cajaService, ItemsClient itemsClient, PacientesClient pacientesClient, MongoTemplate mongoTemplate,
                          AuditoriaService auditoriaService, @Value("${vetcare.facturacion.tasa-impuesto:19}") int tasaImpuesto) {
        this.facturaRepository = facturaRepository;
        this.tarifaRepository = tarifaRepository;
        this.cajaRepository = cajaRepository;
        this.cajaService = cajaService;
        this.itemsClient = itemsClient;
        this.pacientesClient = pacientesClient;
        this.mongoTemplate = mongoTemplate;
        this.auditoriaService = auditoriaService;
        this.tasaImpuesto = tasaImpuesto;
    }

    public Factura generar(FacturaRequest solicitud, String cajero) {
        MascotaInfo mascota = pacientesClient.mascota(solicitud.mascotaId());
        String correo = pacientesClient.propietario(mascota.propietarioId()).correo();
        List<ItemPendiente> pendientes = itemsClient.pendientesDe(mascota.id());
        if (pendientes.isEmpty()) {
            throw new ReglaNegocioException("La mascota no tiene ítems pendientes de facturar");
        }
        List<ItemFactura> items = pendientes.stream().map(this::valorar).toList();

        Factura factura = new Factura();
        factura.setNumero(siguienteNumero());
        factura.setMascotaId(mascota.id());
        factura.setMascotaNombre(mascota.nombre());
        factura.setPropietarioId(mascota.propietarioId());
        factura.setPropietarioCorreo(correo);
        factura.setItems(items);
        factura.setCajero(cajero);
        factura.setEstado(Boolean.TRUE.equals(solicitud.credito()) ? EstadoFactura.CREDITO : EstadoFactura.PENDIENTE);
        factura.setTasaImpuesto(tasaImpuesto);
        factura.setSubtotal(items.stream().mapToLong(ItemFactura::subtotal).sum());
        recalcular(factura, 0);

        // Primero se marcan: si otro cajero facturó un ítem, responde 409 y no queda factura.
        itemsClient.marcar(items, true);
        try {
            Factura guardada = facturaRepository.save(factura);
            auditoriaService.registrar("GENERAR_FACTURA", "facturas/" + guardada.getId(), guardada.getNumero());
            return guardada;
        } catch (RuntimeException ex) {
            itemsClient.marcar(items, false);
            throw ex;
        }
    }

    public Factura aplicarAjustes(String id, AjustesFacturaRequest solicitud) {
        Factura factura = buscar(id);
        if (factura.getEstado() == EstadoFactura.ANULADA || !factura.getPagos().isEmpty()) {
            throw new ReglaNegocioException("La factura ya tiene pagos o está anulada");
        }
        long descuento = descuento(factura.getSubtotal(), solicitud);
        if (descuento > factura.getSubtotal()) {
            throw new ReglaNegocioException("El descuento supera el subtotal");
        }
        recalcular(factura, descuento);
        Factura guardada = guardar(factura);
        auditoriaService.registrar("APLICAR_DESCUENTO", "facturas/" + id, String.valueOf(descuento));
        return guardada;
    }

    public Factura pagar(String id, List<ItemPagoRequest> pagos, String cajeroId, String cajero) {
        Factura factura = buscar(id);
        if (factura.getEstado() != EstadoFactura.PENDIENTE) {
            throw new ReglaNegocioException("La factura no admite pagos en su estado actual");
        }
        Caja caja = cajaService.abiertaDe(cajeroId);
        long suma = pagos.stream().mapToLong(ItemPagoRequest::valor).sum();
        if (suma > factura.getSaldo()) {
            throw new ReglaNegocioException("El pago supera el saldo de la factura");
        }
        pagos.forEach(p -> factura.getPagos().add(pago(p.medio(), p.valor(), caja, cajero, false)));
        factura.setSaldo(factura.getSaldo() - suma);
        if (factura.getSaldo() == 0) {
            factura.setEstado(EstadoFactura.PAGADA);
        }
        Factura guardada = guardar(factura);
        auditoriaService.registrar("REGISTRAR_PAGO", "facturas/" + id, String.valueOf(suma));
        return guardada;
    }

    public Factura abonar(String id, ItemPagoRequest abono, String cajeroId, String cajero) {
        Factura factura = buscar(id);
        if (factura.getEstado() != EstadoFactura.CREDITO) {
            throw new ReglaNegocioException("La factura no es a crédito");
        }
        if (abono.valor() > factura.getSaldo()) {
            throw new ReglaNegocioException("El abono supera el saldo de la factura");
        }
        Caja caja = cajaService.abiertaDe(cajeroId);
        factura.getPagos().add(pago(abono.medio(), abono.valor(), caja, cajero, true));
        factura.setSaldo(factura.getSaldo() - abono.valor());
        if (factura.getSaldo() == 0) {
            factura.setEstado(EstadoFactura.PAGADA);
        }
        Factura guardada = guardar(factura);
        auditoriaService.registrar("REGISTRAR_ABONO", "facturas/" + id, String.valueOf(abono.valor()));
        return guardada;
    }

    public Factura anular(String id, String motivo) {
        Factura factura = buscar(id);
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new ReglaNegocioException("La factura ya está anulada");
        }
        for (Pago pago : factura.getPagos()) {
            boolean abierta = cajaRepository.findById(pago.getCajaId()).filter(c -> c.getEstado() == EstadoCaja.ABIERTA).isPresent();
            if (!abierta) {
                throw new ReglaNegocioException("La factura tiene pagos en una caja cerrada");
            }
        }
        // Primero se liberan los ítems: si un servicio no responde, la factura sigue vigente.
        itemsClient.marcar(factura.getItems(), false);
        factura.getPagos().forEach(p -> p.setRevertido(true));
        factura.setEstado(EstadoFactura.ANULADA);
        factura.setMotivoAnulacion(motivo.trim());
        factura.setSaldo(0);
        Factura guardada = guardar(factura);
        auditoriaService.registrar("ANULAR_FACTURA", "facturas/" + id, motivo.trim());
        return guardada;
    }

    public Factura buscar(String id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La factura no existe"));
    }

    public PaginaResponse<Factura> consultar(EstadoFactura estado, int pagina, int tamano) {
        Query query = new Query();
        if (estado != null) {
            query.addCriteria(Criteria.where("estado").is(estado));
        }
        long total = mongoTemplate.count(query, Factura.class);
        int paginaValida = PaginaResponse.paginaValida(pagina);
        int tamanoValido = PaginaResponse.tamanoValido(tamano);
        query.with(PageRequest.of(paginaValida, tamanoValido, Sort.by(Sort.Direction.DESC, "fecha")));
        return PaginaResponse.de(mongoTemplate.find(query, Factura.class), f -> f, paginaValida, tamanoValido, total);
    }

    public List<Factura> dePropietario(String propietarioId) {
        return facturaRepository.findByPropietarioIdOrderByFechaDesc(propietarioId);
    }

    public Factura propiaDe(String id, String propietarioId) {
        Factura factura = buscar(id);
        if (!factura.getPropietarioId().equals(propietarioId)) {
            throw new AccessDeniedException("La factura es de otro propietario");
        }
        return factura;
    }

    /** RN de facturación: el impuesto se calcula sobre el valor con descuento (D18). */
    static long impuesto(long subtotal, long descuento, int tasa) {
        return Math.round((subtotal - descuento) * tasa / 100.0);
    }

    static long descuento(long subtotal, AjustesFacturaRequest solicitud) {
        if (solicitud.descuentoValor() != null) {
            return solicitud.descuentoValor();
        }
        if (solicitud.descuentoPorcentaje() != null) {
            return Math.round(subtotal * solicitud.descuentoPorcentaje() / 100.0);
        }
        throw new DatosInvalidosException("Debe indicar el descuento en valor o en porcentaje");
    }

    private void recalcular(Factura factura, long descuento) {
        long impuesto = impuesto(factura.getSubtotal(), descuento, factura.getTasaImpuesto());
        long total = factura.getSubtotal() - descuento + impuesto;
        factura.setDescuento(descuento);
        factura.setImpuesto(impuesto);
        factura.setTotal(total);
        factura.setSaldo(total);
    }

    private ItemFactura valorar(ItemPendiente item) {
        TipoServicio tipo = TipoServicio.valueOf(item.tipo());
        if (tipo == TipoServicio.MEDICAMENTOS) {
            long valor = item.valor() != null ? item.valor() : 0;
            return new ItemFactura(item.id(), tipo, item.descripcion(), 1, valor, valor);
        }
        Tarifa tarifa = tarifaRepository.findByServicio(tipo)
                .orElseThrow(() -> new ReglaNegocioException("No hay tarifa vigente para el servicio " + tipo));
        int cantidad = item.cantidad() != null ? item.cantidad() : 1;
        return new ItemFactura(item.id(), tipo, item.descripcion(), cantidad, tarifa.getPrecio(), cantidad * tarifa.getPrecio());
    }

    private Pago pago(MedioPago medio, long valor, Caja caja, String cajero, boolean abono) {
        Pago pago = new Pago();
        pago.setMedio(medio);
        pago.setValor(valor);
        pago.setCajaId(caja.getId());
        pago.setCajero(cajero);
        pago.setAbono(abono);
        return pago;
    }

    private String siguienteNumero() {
        org.bson.Document contador = mongoTemplate.findAndModify(new Query(Criteria.where("_id").is("factura")), new Update().inc("valor", 1),
                FindAndModifyOptions.options().upsert(true).returnNew(true), org.bson.Document.class, "contadores");
        return String.format("FAC-%06d", contador.getInteger("valor"));
    }

    private Factura guardar(Factura factura) {
        try {
            return facturaRepository.save(factura);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictoException("La factura cambió mientras se procesaba; intente de nuevo");
        }
    }
}
