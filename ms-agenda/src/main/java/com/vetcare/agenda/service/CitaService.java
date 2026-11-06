package com.vetcare.agenda.service;

import com.vetcare.agenda.client.PacientesClient;
import com.vetcare.agenda.dto.CitaRequest;
import com.vetcare.agenda.dto.MascotaInfo;
import com.vetcare.agenda.dto.ReprogramacionRequest;
import com.vetcare.agenda.exception.ConflictoException;
import com.vetcare.agenda.exception.RecursoNoEncontradoException;
import com.vetcare.agenda.exception.ReglaNegocioException;
import com.vetcare.agenda.model.Cita;
import com.vetcare.agenda.model.EstadoCita;
import com.vetcare.agenda.model.Horario;
import com.vetcare.agenda.model.TipoCita;
import com.vetcare.agenda.repository.CitaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final HorarioService horarioService;
    private final CupoService cupoService;
    private final PacientesClient pacientesClient;
    private final AuditoriaService auditoriaService;

    public CitaService(CitaRepository citaRepository, HorarioService horarioService, CupoService cupoService,
                       PacientesClient pacientesClient, AuditoriaService auditoriaService) {
        this.citaRepository = citaRepository;
        this.horarioService = horarioService;
        this.cupoService = cupoService;
        this.pacientesClient = pacientesClient;
        this.auditoriaService = auditoriaService;
    }

    public Cita agendar(CitaRequest solicitud) {
        MascotaInfo mascota = mascotaActiva(solicitud.mascotaId());
        String correo = pacientesClient.propietario(mascota.propietarioId()).correo();
        return reservar(TipoCita.VETERINARIA, solicitud.veterinarioId(), mascota, correo,
                solicitud.fecha(), solicitud.hora(), solicitud.motivo());
    }

    /** HU-024: ms-pacientes rechaza con 403 si la mascota no pertenece al propietario del token. */
    public Cita agendarDesdePortal(CitaRequest solicitud, String correoPropietario) {
        MascotaInfo mascota = mascotaActiva(solicitud.mascotaId());
        return reservar(TipoCita.VETERINARIA, solicitud.veterinarioId(), mascota, correoPropietario,
                solicitud.fecha(), solicitud.hora(), solicitud.motivo());
    }

    public Cita agendarPeluqueria(String mascotaId, LocalDate fecha, LocalTime hora, String servicio) {
        MascotaInfo mascota = mascotaActiva(mascotaId);
        String correo = pacientesClient.propietario(mascota.propietarioId()).correo();
        return reservar(TipoCita.PELUQUERIA, HorarioService.RECURSO_PELUQUERIA, mascota, correo, fecha, hora, servicio);
    }

    public Cita reprogramar(String id, ReprogramacionRequest solicitud) {
        Cita cita = buscar(id);
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new ReglaNegocioException("Solo se puede reprogramar una cita programada");
        }
        Horario horario = horarioService.obtener(cita.getRecursoId());
        validarFecha(solicitud.fecha());
        horarioService.validarFranja(horario, solicitud.fecha(), solicitud.hora());

        LocalDate fechaAnterior = cita.getFecha();
        boolean cambiaDeDia = !fechaAnterior.equals(solicitud.fecha());
        String claveNueva = clave(cita.getRecursoId(), solicitud.fecha(), solicitud.hora());
        if (citaRepository.existsByClaveFranja(claveNueva)) {
            throw new ConflictoException("La franja ya está ocupada");
        }
        if (cambiaDeDia) {
            cupoService.reservar(cita.getRecursoId(), solicitud.fecha(), horario.getCuposDiarios());
        }
        cita.setFecha(solicitud.fecha());
        cita.setHora(solicitud.hora());
        cita.setClaveFranja(claveNueva);
        cita.setRecordatorioEnviado(false);
        try {
            Cita guardada = citaRepository.save(cita);
            if (cambiaDeDia) {
                cupoService.liberar(cita.getRecursoId(), fechaAnterior);
            }
            auditoriaService.registrar("REPROGRAMAR_CITA", "citas/" + id, "EXITOSO");
            return guardada;
        } catch (DuplicateKeyException ex) {
            if (cambiaDeDia) {
                cupoService.liberar(cita.getRecursoId(), solicitud.fecha());
            }
            throw new ConflictoException("La franja ya está ocupada");
        }
    }

    public Cita cancelar(String id, String motivo) {
        Cita cita = buscar(id);
        if (cita.getEstado() != EstadoCita.PROGRAMADA && cita.getEstado() != EstadoCita.EN_ESPERA) {
            throw new ReglaNegocioException("La cita ya no se puede cancelar");
        }
        cita.setEstado(EstadoCita.CANCELADA);
        cita.setMotivoCancelacion(motivo.trim());
        cita.setClaveFranja(null);
        Cita guardada = citaRepository.save(cita);
        cupoService.liberar(cita.getRecursoId(), cita.getFecha());
        auditoriaService.registrar("CANCELAR_CITA", "citas/" + id, "EXITOSO");
        return guardada;
    }

    public Cita registrarLlegada(String id) {
        Cita cita = buscar(id);
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new ReglaNegocioException("Solo se puede registrar la llegada de una cita programada");
        }
        if (!cita.getFecha().equals(LocalDate.now())) {
            throw new ReglaNegocioException("La cita no está programada para hoy");
        }
        cita.setEstado(EstadoCita.EN_ESPERA);
        cita.setHoraLlegada(Instant.now());
        return citaRepository.save(cita);
    }

    /** HU-035: ms-historia-clinica la invoca al cerrar la consulta. */
    public Cita marcarAtendida(String id) {
        Cita cita = buscar(id);
        if (cita.getEstado() != EstadoCita.EN_ESPERA) {
            throw new ReglaNegocioException("Solo se puede atender una cita en espera");
        }
        cita.setEstado(EstadoCita.ATENDIDA);
        return citaRepository.save(cita);
    }

    /** HU-071: parte de las mascotas actuales del propietario, por si alguna fue transferida. */
    public List<Cita> proximasDelPropietario() {
        List<String> mascotas = java.util.Arrays.stream(pacientesClient.mascotasPropias()).map(MascotaInfo::id).toList();
        return citaRepository.findByMascotaIdInAndEstadoInAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(
                mascotas, List.of(EstadoCita.PROGRAMADA, EstadoCita.EN_ESPERA), LocalDate.now());
    }

    public List<Cita> delDia(TipoCita tipo, LocalDate fecha) {
        return citaRepository.findByTipoAndFechaOrderByHora(tipo, fecha);
    }

    public List<Cita> agendaDe(String veterinarioId, LocalDate fecha) {
        return citaRepository.findByRecursoIdAndFechaOrderByHora(veterinarioId, fecha);
    }

    public Cita buscar(String id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cita no existe"));
    }

    private Cita reservar(TipoCita tipo, String recursoId, MascotaInfo mascota, String correo, LocalDate fecha,
                          LocalTime hora, String motivo) {
        validarFecha(fecha);
        Horario horario = horarioService.obtener(recursoId);
        horarioService.validarFranja(horario, fecha, hora);
        String clave = clave(recursoId, fecha, hora);
        if (citaRepository.existsByClaveFranja(clave)) {
            throw new ConflictoException("La franja ya está ocupada");
        }
        cupoService.reservar(recursoId, fecha, horario.getCuposDiarios());

        Cita cita = new Cita();
        cita.setTipo(tipo);
        cita.setRecursoId(recursoId);
        cita.setMascotaId(mascota.id());
        cita.setMascotaNombre(mascota.nombre());
        cita.setPropietarioId(mascota.propietarioId());
        cita.setPropietarioCorreo(correo);
        cita.setFecha(fecha);
        cita.setHora(hora);
        cita.setMotivo(motivo.trim());
        cita.setClaveFranja(clave);
        try {
            Cita guardada = citaRepository.save(cita);
            auditoriaService.registrar("AGENDAR_CITA", "citas/" + guardada.getId(), "EXITOSO");
            return guardada;
        } catch (DuplicateKeyException ex) {
            cupoService.liberar(recursoId, fecha);
            throw new ConflictoException("La franja ya está ocupada");
        }
    }

    private MascotaInfo mascotaActiva(String mascotaId) {
        MascotaInfo mascota = pacientesClient.mascota(mascotaId);
        if (!"ACTIVA".equals(mascota.estado())) {
            throw new ReglaNegocioException("La mascota no está activa");
        }
        return mascota;
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de la cita ya pasó");
        }
    }

    private String clave(String recursoId, LocalDate fecha, LocalTime hora) {
        return recursoId + "|" + fecha + "|" + hora;
    }
}
