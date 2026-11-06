package com.vetcare.historia.service;

import com.vetcare.historia.client.PacientesClient;
import com.vetcare.historia.dto.HistoriaResponse;
import com.vetcare.historia.dto.MascotaInfo;
import com.vetcare.historia.dto.OrdenLaboratorioResponse;
import com.vetcare.historia.dto.ResultadosFormulasResponse;
import com.vetcare.historia.exception.ServicioNoDisponibleException;
import com.vetcare.historia.model.EstadoOrdenLaboratorio;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Compone la historia clínica completa de una mascota (D7).
 */
@Service
public class HistoriaService {

    private final PacientesClient pacientesClient;
    private final ConsultaService consultaService;
    private final VacunaService vacunaService;
    private final LaboratorioService laboratorioService;
    private final FormulaService formulaService;
    private final HospitalizacionService hospitalizacionService;
    private final AdjuntoService adjuntoService;

    public HistoriaService(PacientesClient pacientesClient, ConsultaService consultaService, VacunaService vacunaService,
                           LaboratorioService laboratorioService, FormulaService formulaService,
                           HospitalizacionService hospitalizacionService, AdjuntoService adjuntoService) {
        this.pacientesClient = pacientesClient;
        this.consultaService = consultaService;
        this.vacunaService = vacunaService;
        this.laboratorioService = laboratorioService;
        this.formulaService = formulaService;
        this.hospitalizacionService = hospitalizacionService;
        this.adjuntoService = adjuntoService;
    }

    public HistoriaResponse obtener(String mascotaId) {
        List<String> alergias = List.of();
        List<String> condiciones = List.of();
        List<String> avisos = List.of();
        try {
            MascotaInfo mascota = pacientesClient.mascota(mascotaId);
            alergias = mascota.alergias();
            condiciones = mascota.condicionesCronicas();
        } catch (ServicioNoDisponibleException ex) {
            avisos = List.of("Alergias no disponibles");
        }
        return new HistoriaResponse(mascotaId, alergias, condiciones, avisos, consultaService.deMascota(mascotaId),
                vacunaService.deMascota(mascotaId), laboratorioService.deMascota(mascotaId), formulaService.deMascota(mascotaId),
                hospitalizacionService.deMascota(mascotaId), adjuntoService.deMascota(mascotaId));
    }

    /** HU-050: ms-pacientes rechaza con 403 si la mascota no pertenece al propietario del token. */
    public ResultadosFormulasResponse paraPropietario(String mascotaId) {
        pacientesClient.mascota(mascotaId);
        List<OrdenLaboratorioResponse> resultados = laboratorioService.deMascota(mascotaId).stream()
                .filter(o -> o.estado() == EstadoOrdenLaboratorio.COMPLETADA)
                .toList();
        return new ResultadosFormulasResponse(resultados, formulaService.deMascota(mascotaId));
    }
}
