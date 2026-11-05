package com.vetcare.agenda.service;

import com.vetcare.agenda.dto.CitasAtendidasResponse;
import com.vetcare.agenda.dto.OcupacionResponse;
import com.vetcare.agenda.exception.DatosInvalidosException;
import com.vetcare.agenda.model.EstadoCita;
import com.vetcare.agenda.model.Horario;
import com.vetcare.agenda.model.TipoCita;
import com.vetcare.agenda.repository.CitaRepository;
import com.vetcare.agenda.repository.HorarioRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Indicadores de citas y ocupación de cupos (HU-072 y HU-073).
 */
@Service
public class ReporteAgendaService {

    private final HorarioRepository horarioRepository;
    private final CitaRepository citaRepository;

    public ReporteAgendaService(HorarioRepository horarioRepository, CitaRepository citaRepository) {
        this.horarioRepository = horarioRepository;
        this.citaRepository = citaRepository;
    }

    public List<OcupacionResponse> ocupacion(String veterinarioId, LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        if (veterinarioId != null && !veterinarioId.isBlank()) {
            return List.of(horarioRepository.findById(veterinarioId)
                    .map(h -> ocupacionDe(h, desde, hasta))
                    .orElseGet(() -> new OcupacionResponse(veterinarioId, null, 0, 0, 0)));
        }
        return horarioRepository.findByTipo(TipoCita.VETERINARIA).stream().map(h -> ocupacionDe(h, desde, hasta)).toList();
    }

    public CitasAtendidasResponse citas(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDate antes = desde.minusDays(1);
        LocalDate despues = hasta.plusDays(1);
        return new CitasAtendidasResponse(
                citaRepository.countByTipoAndEstadoAndFechaBetween(TipoCita.VETERINARIA, EstadoCita.ATENDIDA, antes, despues),
                citaRepository.countByTipoAndEstadoAndFechaBetween(TipoCita.VETERINARIA, EstadoCita.CANCELADA, antes, despues));
    }

    private OcupacionResponse ocupacionDe(Horario horario, LocalDate desde, LocalDate hasta) {
        long cupos = desde.datesUntil(hasta.plusDays(1))
                .filter(d -> horario.getDias().contains(d.getDayOfWeek()))
                .count() * horario.getCuposDiarios();
        long citas = citaRepository.findByRecursoIdAndFechaBetween(horario.getId(), desde.minusDays(1), hasta.plusDays(1)).stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .count();
        return new OcupacionResponse(horario.getId(), horario.getNombre(), cupos, citas, cupos == 0 ? 0 : Math.round(citas * 100.0 / cupos));
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("La fecha inicial es posterior a la final");
        }
    }
}
