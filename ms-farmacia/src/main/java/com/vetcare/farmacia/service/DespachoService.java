package com.vetcare.farmacia.service;

import com.vetcare.farmacia.client.HistoriaClient;
import com.vetcare.farmacia.dto.DespachoRequest;
import com.vetcare.farmacia.dto.FormulaVigencia;
import com.vetcare.farmacia.dto.ItemDespachoRequest;
import com.vetcare.farmacia.dto.ItemPendienteResponse;
import com.vetcare.farmacia.exception.ConflictoException;
import com.vetcare.farmacia.exception.DatosInvalidosException;
import com.vetcare.farmacia.exception.ReglaNegocioException;
import com.vetcare.farmacia.model.Despacho;
import com.vetcare.farmacia.model.ItemDespacho;
import com.vetcare.farmacia.model.Medicamento;
import com.vetcare.farmacia.model.TipoMovimiento;
import com.vetcare.farmacia.repository.DespachoRepository;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Despacho respaldado por una fórmula vigente (RN-02), con descuento FEFO del inventario (RN-04).
 */
@Service
public class DespachoService {

    private final HistoriaClient historiaClient;
    private final InventarioService inventarioService;
    private final MedicamentoService medicamentoService;
    private final DespachoRepository despachoRepository;
    private final AuditoriaService auditoriaService;

    public DespachoService(HistoriaClient historiaClient, InventarioService inventarioService, MedicamentoService medicamentoService,
                           DespachoRepository despachoRepository, AuditoriaService auditoriaService) {
        this.historiaClient = historiaClient;
        this.inventarioService = inventarioService;
        this.medicamentoService = medicamentoService;
        this.despachoRepository = despachoRepository;
        this.auditoriaService = auditoriaService;
    }

    public Despacho despachar(DespachoRequest solicitud, String responsable) {
        if (new HashSet<>(solicitud.items().stream().map(ItemDespachoRequest::medicamentoId).toList()).size() != solicitud.items().size()) {
            throw new DatosInvalidosException("Un medicamento aparece más de una vez en el despacho");
        }
        FormulaVigencia formula = historiaClient.vigencia(solicitud.formulaId());
        if (!formula.despachable()) {
            auditoriaService.registrar("DESPACHAR", "formulas/" + solicitud.formulaId(), "RECHAZADO: " + formula.estado());
            throw new ReglaNegocioException("No se puede despachar sin fórmula vigente");
        }
        validarContraFormula(solicitud, formula);
        for (ItemDespachoRequest item : solicitud.items()) {
            if (inventarioService.stockDisponible(item.medicamentoId()) < item.cantidad()) {
                throw new ConflictoException("Stock insuficiente");
            }
        }

        String referencia = "Fórmula " + solicitud.formulaId();
        List<InventarioService.Consumo> consumos = new ArrayList<>();
        try {
            for (ItemDespachoRequest item : solicitud.items()) {
                consumos.addAll(inventarioService.descontarFefo(item.medicamentoId(), item.cantidad(), TipoMovimiento.DESPACHO, responsable, referencia));
            }
            historiaClient.registrarDespacho(solicitud.formulaId(), solicitud.items());
        } catch (RuntimeException ex) {
            // Sin transacciones entre microservicios: se compensa para no perder inventario.
            inventarioService.revertir(consumos, responsable, referencia);
            throw ex;
        }

        Despacho despacho = new Despacho();
        despacho.setFormulaId(solicitud.formulaId());
        despacho.setMascotaId(formula.mascotaId());
        despacho.setResponsable(responsable);
        despacho.setItems(solicitud.items().stream().map(this::itemConPrecio).toList());
        Despacho guardado = despachoRepository.save(despacho);
        auditoriaService.registrar("DESPACHAR", "despachos/" + guardado.getId(), "EXITOSO");
        return guardado;
    }

    public List<ItemPendienteResponse> itemsPendientes() {
        return despachoRepository.findByFacturadoFalse().stream()
                .map(d -> new ItemPendienteResponse(d.getId(), "MEDICAMENTOS", descripcion(d), d.getMascotaId(),
                        d.getFecha().atZone(ZoneId.systemDefault()).toLocalDate(),
                        d.getItems().stream().mapToLong(i -> i.cantidad() * i.precioUnitario()).sum()))
                .toList();
    }

    private void validarContraFormula(DespachoRequest solicitud, FormulaVigencia formula) {
        Map<String, Integer> pendientes = formula.items().stream()
                .collect(Collectors.toMap(FormulaVigencia.ItemPendiente::medicamentoId, FormulaVigencia.ItemPendiente::pendiente));
        for (ItemDespachoRequest item : solicitud.items()) {
            Integer pendiente = pendientes.get(item.medicamentoId());
            if (pendiente == null) {
                throw new ReglaNegocioException("El medicamento no está en la fórmula");
            }
            if (item.cantidad() > pendiente) {
                throw new ReglaNegocioException("La cantidad supera lo pendiente en la fórmula");
            }
        }
    }

    private ItemDespacho itemConPrecio(ItemDespachoRequest item) {
        Medicamento medicamento = medicamentoService.buscar(item.medicamentoId());
        return new ItemDespacho(item.medicamentoId(), medicamento.getNombre(), item.cantidad(), medicamento.getPrecio());
    }

    private String descripcion(Despacho despacho) {
        return despacho.getItems().stream().map(i -> i.nombre() + " x " + i.cantidad()).collect(Collectors.joining(", "));
    }
}
