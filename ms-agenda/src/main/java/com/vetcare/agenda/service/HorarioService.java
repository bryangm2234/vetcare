package com.vetcare.agenda.service;

import com.vetcare.agenda.dto.DisponibilidadResponse;
import com.vetcare.agenda.dto.HorarioRequest;
import com.vetcare.agenda.exception.DatosInvalidosException;
import com.vetcare.agenda.exception.RecursoNoEncontradoException;
import com.vetcare.agenda.exception.ReglaNegocioException;
import com.vetcare.agenda.model.Cita;
import com.vetcare.agenda.model.CupoDiario;
import com.vetcare.agenda.model.EstadoCita;
import com.vetcare.agenda.model.Horario;
import com.vetcare.agenda.model.TipoCita;
import com.vetcare.agenda.repository.CitaRepository;
import com.vetcare.agenda.repository.HorarioRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class HorarioService {

    public static final String RECURSO_PELUQUERIA = "peluqueria";

    private final HorarioRepository horarioRepository;
    private final CitaRepository citaRepository;
    private final CupoService cupoService;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoriaService;

    public HorarioService(HorarioRepository horarioRepository, CitaRepository citaRepository, CupoService cupoService,
                          MongoTemplate mongoTemplate, AuditoriaService auditoriaService) {
        this.horarioRepository = horarioRepository;
        this.citaRepository = citaRepository;
        this.cupoService = cupoService;
        this.mongoTemplate = mongoTemplate;
        this.auditoriaService = auditoriaService;
    }

    public Horario configurar(String recursoId, TipoCita tipo, HorarioRequest solicitud) {
        if (!solicitud.horaFin().isAfter(solicitud.horaInicio())) {
            throw new DatosInvalidosException("La hora de fin debe ser posterior a la hora de inicio");
        }
        validarCuposContraCitas(recursoId, solicitud.cuposDiarios());

        Horario horario = new Horario();
        horario.setId(recursoId);
        horario.setTipo(tipo);
        horario.setNombre(solicitud.nombre().trim());
        horario.setDias(solicitud.dias());
        horario.setHoraInicio(solicitud.horaInicio());
        horario.setHoraFin(solicitud.horaFin());
        horario.setDuracionMinutos(solicitud.duracionOPorDefecto());
        horario.setCuposDiarios(solicitud.cuposDiarios());
        Horario guardado = horarioRepository.save(horario);
        auditoriaService.registrar("CONFIGURAR_HORARIO", "horarios/" + recursoId, "EXITOSO");
        return guardado;
    }

    public List<Horario> veterinarios() {
        return horarioRepository.findByTipo(TipoCita.VETERINARIA);
    }

    public Horario obtener(String recursoId) {
        return horarioRepository.findById(recursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El recurso no tiene horario configurado"));
    }

    /** Valida que la hora sea el inicio de una franja dentro del horario (RN-03). */
    public void validarFranja(Horario horario, LocalDate fecha, LocalTime hora) {
        if (!franjas(horario, fecha).contains(hora)) {
            throw new ReglaNegocioException("La fecha y la hora están fuera del horario de atención");
        }
    }

    public List<DisponibilidadResponse> disponibilidad(LocalDate fecha, String recursoId) {
        if (fecha.isBefore(LocalDate.now())) {
            throw new DatosInvalidosException("La fecha ya pasó");
        }
        List<Horario> horarios = recursoId != null && !recursoId.isBlank()
                ? List.of(obtener(recursoId))
                : horarioRepository.findByTipo(TipoCita.VETERINARIA);
        return horarios.stream().map(h -> disponibilidadDe(h, fecha)).toList();
    }

    private DisponibilidadResponse disponibilidadDe(Horario horario, LocalDate fecha) {
        List<LocalTime> franjas = franjas(horario, fecha);
        if (franjas.isEmpty()) {
            return new DisponibilidadResponse(horario.getId(), horario.getNombre(), 0, List.of());
        }
        int restantes = Math.max(horario.getCuposDiarios() - cupoService.usados(horario.getId(), fecha), 0);
        if (restantes == 0) {
            return new DisponibilidadResponse(horario.getId(), horario.getNombre(), 0, List.of());
        }
        Set<LocalTime> ocupadas = citaRepository.findByRecursoIdAndFechaOrderByHora(horario.getId(), fecha).stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .map(Cita::getHora)
                .collect(Collectors.toSet());
        List<LocalTime> libres = franjas.stream().filter(f -> !ocupadas.contains(f)).toList();
        return new DisponibilidadResponse(horario.getId(), horario.getNombre(), restantes, libres);
    }

    private List<LocalTime> franjas(Horario horario, LocalDate fecha) {
        List<LocalTime> franjas = new ArrayList<>();
        if (!horario.getDias().contains(fecha.getDayOfWeek())) {
            return franjas;
        }
        LocalTime hora = horario.getHoraInicio();
        while (!hora.plusMinutes(horario.getDuracionMinutos()).isAfter(horario.getHoraFin())
                && hora.plusMinutes(horario.getDuracionMinutos()).isAfter(hora)) {
            franjas.add(hora);
            hora = hora.plusMinutes(horario.getDuracionMinutos());
        }
        return franjas;
    }

    private void validarCuposContraCitas(String recursoId, int cuposDiarios) {
        Query query = new Query(Criteria.where("_id").regex("^" + java.util.regex.Pattern.quote(recursoId + "|"))
                .and("usados").gt(cuposDiarios));
        for (CupoDiario cupo : mongoTemplate.find(query, CupoDiario.class)) {
            LocalDate fecha = LocalDate.parse(cupo.getId().substring(recursoId.length() + 1));
            if (!fecha.isBefore(LocalDate.now())) {
                throw new ReglaNegocioException("Ya hay " + cupo.getUsados() + " citas agendadas el " + fecha);
            }
        }
    }
}
