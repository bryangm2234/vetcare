package com.vetcare.agenda.service;

import com.vetcare.agenda.dto.ItemPendienteResponse;
import com.vetcare.agenda.exception.ConflictoException;
import com.vetcare.agenda.exception.ReglaNegocioException;
import com.vetcare.agenda.model.Cita;
import com.vetcare.agenda.model.EstadoCita;
import com.vetcare.agenda.model.TipoCita;
import com.vetcare.agenda.repository.CitaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PeluqueriaService {

    private final CitaRepository citaRepository;
    private final CitaService citaService;
    private final AuditoriaService auditoriaService;

    public PeluqueriaService(CitaRepository citaRepository, CitaService citaService, AuditoriaService auditoriaService) {
        this.citaRepository = citaRepository;
        this.citaService = citaService;
        this.auditoriaService = auditoriaService;
    }

    public Cita registrarServicio(String id, String observaciones) {
        Cita cita = citaService.buscar(id);
        if (cita.getTipo() != TipoCita.PELUQUERIA) {
            throw new ReglaNegocioException("La cita no es de peluquería");
        }
        if (cita.getEstado() == EstadoCita.REALIZADA) {
            throw new ConflictoException("El servicio ya está registrado");
        }
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new ReglaNegocioException("La cita no admite registrar el servicio");
        }
        cita.setEstado(EstadoCita.REALIZADA);
        cita.setObservaciones(observaciones.trim());
        Cita guardada = citaRepository.save(cita);
        auditoriaService.registrar("REGISTRAR_SERVICIO_PELUQUERIA", "citas/" + id, "EXITOSO");
        return guardada;
    }

    public List<ItemPendienteResponse> itemsPendientes() {
        return citaRepository.findByTipoAndEstadoAndFacturadoFalse(TipoCita.PELUQUERIA, EstadoCita.REALIZADA).stream()
                .map(c -> new ItemPendienteResponse(c.getId(), "PELUQUERIA", c.getMotivo(), c.getMascotaId(), c.getFecha()))
                .toList();
    }
}
