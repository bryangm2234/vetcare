package com.vetcare.agenda.dto;

import com.vetcare.agenda.model.Cita;
import com.vetcare.agenda.model.EstadoCita;
import com.vetcare.agenda.model.TipoCita;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CitaResponse(String id, TipoCita tipo, String recursoId, String mascotaId, String mascotaNombre,
                           String propietarioId, LocalDate fecha, LocalTime hora, String motivo, EstadoCita estado,
                           Instant horaLlegada, String observaciones) {

    public static CitaResponse de(Cita cita) {
        return new CitaResponse(cita.getId(), cita.getTipo(), cita.getRecursoId(), cita.getMascotaId(), cita.getMascotaNombre(),
                cita.getPropietarioId(), cita.getFecha(), cita.getHora(), cita.getMotivo(), cita.getEstado(),
                cita.getHoraLlegada(), cita.getObservaciones());
    }
}
