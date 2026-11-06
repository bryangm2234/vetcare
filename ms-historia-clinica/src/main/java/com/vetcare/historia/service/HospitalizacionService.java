package com.vetcare.historia.service;

import com.vetcare.historia.client.PacientesClient;
import com.vetcare.historia.dto.EvolucionRequest;
import com.vetcare.historia.dto.HospitalizacionRequest;
import com.vetcare.historia.dto.ItemPendienteResponse;
import com.vetcare.historia.exception.ConflictoException;
import com.vetcare.historia.exception.RecursoNoEncontradoException;
import com.vetcare.historia.exception.ReglaNegocioException;
import com.vetcare.historia.model.EstadoHospitalizacion;
import com.vetcare.historia.model.Evolucion;
import com.vetcare.historia.model.Hospitalizacion;
import com.vetcare.historia.repository.HospitalizacionRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class HospitalizacionService {

    private final HospitalizacionRepository hospitalizacionRepository;
    private final PacientesClient pacientesClient;
    private final FormulaService formulaService;
    private final AuditoriaService auditoriaService;

    public HospitalizacionService(HospitalizacionRepository hospitalizacionRepository, PacientesClient pacientesClient,
                                  FormulaService formulaService, AuditoriaService auditoriaService) {
        this.hospitalizacionRepository = hospitalizacionRepository;
        this.pacientesClient = pacientesClient;
        this.formulaService = formulaService;
        this.auditoriaService = auditoriaService;
    }

    public Hospitalizacion ordenar(HospitalizacionRequest solicitud, String veterinarioId) {
        pacientesClient.mascota(solicitud.mascotaId());
        String cubiculo = solicitud.cubiculo().trim();
        if (hospitalizacionRepository.existsByCubiculoOcupado(cubiculo)) {
            throw new ConflictoException("El cubículo está ocupado");
        }
        if (hospitalizacionRepository.existsByMascotaInternada(solicitud.mascotaId())) {
            throw new ConflictoException("La mascota ya está hospitalizada");
        }
        Hospitalizacion hospitalizacion = new Hospitalizacion();
        hospitalizacion.setMascotaId(solicitud.mascotaId());
        hospitalizacion.setVeterinarioId(veterinarioId);
        hospitalizacion.setMotivo(solicitud.motivo().trim());
        hospitalizacion.setCubiculo(cubiculo);
        hospitalizacion.setCubiculoOcupado(cubiculo);
        hospitalizacion.setMascotaInternada(solicitud.mascotaId());
        try {
            Hospitalizacion guardada = hospitalizacionRepository.save(hospitalizacion);
            auditoriaService.registrar("HOSPITALIZAR", "hospitalizaciones/" + guardada.getId(), "EXITOSO");
            return guardada;
        } catch (DuplicateKeyException ex) {
            throw new ConflictoException("El cubículo está ocupado o la mascota ya está hospitalizada");
        }
    }

    public Hospitalizacion registrarEvolucion(String id, EvolucionRequest solicitud, String responsable) {
        Hospitalizacion hospitalizacion = activa(id);
        List<String> medicamentos = solicitud.medicamentosAdministrados() != null ? solicitud.medicamentosAdministrados() : List.of();
        for (String medicamentoId : medicamentos) {
            if (!formulaService.fueDespachado(hospitalizacion.getMascotaId(), medicamentoId)) {
                throw new ReglaNegocioException("El medicamento no ha sido despachado para la mascota");
            }
        }
        hospitalizacion.getEvoluciones().add(new Evolucion(solicitud.observaciones().trim(), solicitud.temperatura(), medicamentos, responsable, Instant.now()));
        return hospitalizacionRepository.save(hospitalizacion);
    }

    public Hospitalizacion darDeAlta(String id, String indicaciones) {
        Hospitalizacion hospitalizacion = activa(id);
        Instant ahora = Instant.now();
        hospitalizacion.setEstado(EstadoHospitalizacion.FINALIZADA);
        hospitalizacion.setIndicacionesAlta(indicaciones.trim());
        hospitalizacion.setFechaAlta(ahora);
        hospitalizacion.setDiasEstancia(diasEstancia(hospitalizacion.getFechaIngreso(), ahora));
        hospitalizacion.setCubiculoOcupado(null);
        hospitalizacion.setMascotaInternada(null);
        Hospitalizacion guardada = hospitalizacionRepository.save(hospitalizacion);
        auditoriaService.registrar("DAR_DE_ALTA", "hospitalizaciones/" + id, "EXITOSO");
        return guardada;
    }

    public List<Hospitalizacion> deMascota(String mascotaId) {
        return hospitalizacionRepository.findByMascotaIdOrderByFechaIngresoDesc(mascotaId);
    }

    public List<ItemPendienteResponse> itemsPendientes() {
        return hospitalizacionRepository.findByEstadoAndFacturadoFalse(EstadoHospitalizacion.FINALIZADA).stream()
                .map(h -> new ItemPendienteResponse(h.getId(), "HOSPITALIZACION", "Estancia de " + h.getDiasEstancia() + " días", h.getMascotaId(),
                        h.getFechaAlta().atZone(ZoneId.systemDefault()).toLocalDate()))
                .toList();
    }

    /** Se cobra al menos un día, aunque el alta ocurra el día del ingreso. */
    static long diasEstancia(Instant ingreso, Instant alta) {
        return Math.max(ChronoUnit.DAYS.between(ingreso, alta), 1);
    }

    private Hospitalizacion activa(String id) {
        Hospitalizacion hospitalizacion = hospitalizacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La hospitalización no existe"));
        if (hospitalizacion.getEstado() != EstadoHospitalizacion.ACTIVA) {
            throw new ReglaNegocioException("La mascota ya fue dada de alta");
        }
        return hospitalizacion;
    }
}
