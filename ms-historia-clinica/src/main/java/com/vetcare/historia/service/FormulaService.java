package com.vetcare.historia.service;

import com.vetcare.historia.client.FarmaciaClient;
import com.vetcare.historia.dto.FormulaRequest;
import com.vetcare.historia.dto.FormulaResponse;
import com.vetcare.historia.dto.ItemDespachadoRequest;
import com.vetcare.historia.dto.ItemFormulaRequest;
import com.vetcare.historia.dto.MedicamentoDespachadoResponse;
import com.vetcare.historia.dto.VigenciaResponse;
import com.vetcare.historia.exception.ConflictoException;
import com.vetcare.historia.exception.DatosInvalidosException;
import com.vetcare.historia.exception.RecursoNoEncontradoException;
import com.vetcare.historia.exception.ReglaNegocioException;
import com.vetcare.historia.model.Consulta;
import com.vetcare.historia.model.EstadoFormula;
import com.vetcare.historia.model.Formula;
import com.vetcare.historia.model.ItemFormula;
import com.vetcare.historia.repository.FormulaRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Fórmulas médicas con vigencia y cantidades pendientes (RN-02).
 */
@Service
public class FormulaService {

    private final FormulaRepository formulaRepository;
    private final ConsultaService consultaService;
    private final FarmaciaClient farmaciaClient;
    private final AuditoriaService auditoriaService;

    public FormulaService(FormulaRepository formulaRepository, ConsultaService consultaService, FarmaciaClient farmaciaClient,
                          AuditoriaService auditoriaService) {
        this.formulaRepository = formulaRepository;
        this.consultaService = consultaService;
        this.farmaciaClient = farmaciaClient;
        this.auditoriaService = auditoriaService;
    }

    public FormulaResponse emitir(FormulaRequest solicitud, String veterinarioId) {
        if (new HashSet<>(solicitud.items().stream().map(ItemFormulaRequest::medicamentoId).toList()).size() != solicitud.items().size()) {
            throw new DatosInvalidosException("Un medicamento aparece más de una vez en la fórmula");
        }
        Consulta consulta = consultaService.abierta(solicitud.consultaId());
        Formula formula = new Formula();
        formula.setConsultaId(consulta.getId());
        formula.setMascotaId(consulta.getMascotaId());
        formula.setVeterinarioId(veterinarioId);
        formula.setVigenteHasta(solicitud.vigenteHasta());
        formula.setItems(solicitud.items().stream().map(this::item).toList());
        Formula guardada = formulaRepository.save(formula);
        auditoriaService.registrar("EMITIR_FORMULA", "formulas/" + guardada.getId(), "EXITOSO");
        return respuesta(guardada);
    }

    public FormulaResponse anular(String id, String motivo) {
        Formula formula = buscar(id);
        EstadoFormula estado = estadoEfectivo(formula);
        if (estado != EstadoFormula.VIGENTE && estado != EstadoFormula.PARCIAL) {
            throw new ReglaNegocioException("La fórmula no se puede anular");
        }
        formula.setEstado(EstadoFormula.ANULADA);
        formula.setMotivoAnulacion(motivo.trim());
        Formula guardada = guardar(formula);
        auditoriaService.registrar("ANULAR_FORMULA", "formulas/" + id, motivo.trim());
        return respuesta(guardada);
    }

    public VigenciaResponse vigencia(String id) {
        Formula formula = buscar(id);
        EstadoFormula estado = estadoEfectivo(formula);
        List<VigenciaResponse.ItemPendiente> items = formula.getItems().stream()
                .map(i -> new VigenciaResponse.ItemPendiente(i.getMedicamentoId(), i.getMedicamento(), i.getPendiente()))
                .toList();
        return new VigenciaResponse(id, formula.getMascotaId(), estado, despachable(estado), items);
    }

    /** ms-farmacia lo invoca tras descontar el stock. Vuelve a validar para no despachar de más con peticiones simultáneas. */
    public FormulaResponse registrarDespacho(String id, List<ItemDespachadoRequest> despachados) {
        Formula formula = buscar(id);
        if (!despachable(estadoEfectivo(formula))) {
            throw new ReglaNegocioException("No se puede despachar sin fórmula vigente");
        }
        for (ItemDespachadoRequest despachado : despachados) {
            ItemFormula item = formula.getItems().stream()
                    .filter(i -> i.getMedicamentoId().equals(despachado.medicamentoId()))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("El medicamento no está en la fórmula"));
            if (despachado.cantidad() > item.getPendiente()) {
                throw new ReglaNegocioException("La cantidad supera lo pendiente en la fórmula");
            }
            item.setPendiente(item.getPendiente() - despachado.cantidad());
        }
        boolean completa = formula.getItems().stream().allMatch(i -> i.getPendiente() == 0);
        formula.setEstado(completa ? EstadoFormula.DESPACHADA : EstadoFormula.PARCIAL);
        return respuesta(guardar(formula));
    }

    public List<FormulaResponse> despachables() {
        return formulaRepository.findByEstadoInOrderByFechaDesc(List.of(EstadoFormula.VIGENTE, EstadoFormula.PARCIAL)).stream()
                .filter(f -> despachable(estadoEfectivo(f)))
                .map(this::respuesta)
                .toList();
    }

    public List<FormulaResponse> deMascota(String mascotaId) {
        return formulaRepository.findByMascotaIdOrderByFechaDesc(mascotaId).stream().map(this::respuesta).toList();
    }

    /** D11: en hospitalización solo se administra lo que ya se despachó para la mascota. */
    public boolean fueDespachado(String mascotaId, String medicamentoId) {
        return formulaRepository.findByMascotaIdOrderByFechaDesc(mascotaId).stream()
                .flatMap(f -> f.getItems().stream())
                .anyMatch(i -> i.getMedicamentoId().equals(medicamentoId) && i.getPendiente() < i.getCantidad());
    }

    public List<MedicamentoDespachadoResponse> despachadosDe(String mascotaId) {
        return formulaRepository.findByMascotaIdOrderByFechaDesc(mascotaId).stream()
                .flatMap(f -> f.getItems().stream())
                .filter(i -> i.getPendiente() < i.getCantidad())
                .map(i -> new MedicamentoDespachadoResponse(i.getMedicamentoId(), i.getMedicamento()))
                .distinct()
                .toList();
    }

    static EstadoFormula estadoEfectivo(Formula formula, LocalDate hoy) {
        boolean abierta = formula.getEstado() == EstadoFormula.VIGENTE || formula.getEstado() == EstadoFormula.PARCIAL;
        return abierta && formula.getVigenteHasta().isBefore(hoy) ? EstadoFormula.VENCIDA : formula.getEstado();
    }

    private EstadoFormula estadoEfectivo(Formula formula) {
        return estadoEfectivo(formula, LocalDate.now());
    }

    private boolean despachable(EstadoFormula estado) {
        return estado == EstadoFormula.VIGENTE || estado == EstadoFormula.PARCIAL;
    }

    private ItemFormula item(ItemFormulaRequest solicitud) {
        ItemFormula item = new ItemFormula();
        item.setMedicamentoId(solicitud.medicamentoId());
        item.setMedicamento(nombreMedicamento(solicitud.medicamentoId()));
        item.setDosis(solicitud.dosis().trim());
        item.setFrecuencia(solicitud.frecuencia().trim());
        item.setDuracion(solicitud.duracion().trim());
        item.setCantidad(solicitud.cantidad());
        item.setPendiente(solicitud.cantidad());
        return item;
    }

    private String nombreMedicamento(String medicamentoId) {
        try {
            return farmaciaClient.medicamento(medicamentoId).nombre();
        } catch (RecursoNoEncontradoException ex) {
            throw new ReglaNegocioException("El medicamento no existe en farmacia");
        }
    }

    private Formula guardar(Formula formula) {
        try {
            return formulaRepository.save(formula);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictoException("La fórmula cambió mientras se procesaba; intente de nuevo");
        }
    }

    private Formula buscar(String id) {
        return formulaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La fórmula no existe"));
    }

    private FormulaResponse respuesta(Formula formula) {
        return FormulaResponse.de(formula, estadoEfectivo(formula));
    }
}
