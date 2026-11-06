package com.vetcare.historia.service;

import com.vetcare.historia.client.PacientesClient;
import com.vetcare.historia.dto.HistoriaResponse;
import com.vetcare.historia.dto.MascotaInfo;
import com.vetcare.historia.exception.ServicioNoDisponibleException;
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
    private final AdjuntoService adjuntoService;

    public HistoriaService(PacientesClient pacientesClient, ConsultaService consultaService, VacunaService vacunaService,
                           AdjuntoService adjuntoService) {
        this.pacientesClient = pacientesClient;
        this.consultaService = consultaService;
        this.vacunaService = vacunaService;
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
                vacunaService.deMascota(mascotaId), adjuntoService.deMascota(mascotaId));
    }
}
