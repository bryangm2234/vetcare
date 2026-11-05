package com.vetcare.historia.service;

import com.vetcare.historia.client.AgendaClient;
import com.vetcare.historia.dto.CitaInfo;
import com.vetcare.historia.dto.ConsultaResponse;
import com.vetcare.historia.dto.ItemPendienteResponse;
import com.vetcare.historia.dto.RegistroClinicoRequest;
import com.vetcare.historia.dto.SignosVitalesRequest;
import com.vetcare.historia.exception.ConflictoException;
import com.vetcare.historia.exception.RecursoNoEncontradoException;
import com.vetcare.historia.exception.ReglaNegocioException;
import com.vetcare.historia.model.Consulta;
import com.vetcare.historia.model.EstadoConsulta;
import com.vetcare.historia.model.SignosVitales;
import com.vetcare.historia.repository.ConsultaRepository;
import com.vetcare.historia.repository.SignosVitalesRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConsultaService {

    private static final String EN_ESPERA = "EN_ESPERA";

    private final ConsultaRepository consultaRepository;
    private final SignosVitalesRepository signosRepository;
    private final AgendaClient agendaClient;
    private final AuditoriaService auditoriaService;

    public ConsultaService(ConsultaRepository consultaRepository, SignosVitalesRepository signosRepository,
                           AgendaClient agendaClient, AuditoriaService auditoriaService) {
        this.consultaRepository = consultaRepository;
        this.signosRepository = signosRepository;
        this.agendaClient = agendaClient;
        this.auditoriaService = auditoriaService;
    }

    public ConsultaResponse abrir(String citaId, String veterinarioId) {
        if (consultaRepository.existsByCitaId(citaId)) {
            throw new ConflictoException("La cita ya tiene una consulta");
        }
        CitaInfo cita = agendaClient.cita(citaId);
        if (!EN_ESPERA.equals(cita.estado())) {
            throw new ReglaNegocioException("La cita no está en espera");
        }
        Consulta consulta = new Consulta();
        consulta.setCitaId(citaId);
        consulta.setMascotaId(cita.mascotaId());
        consulta.setVeterinarioId(veterinarioId);
        Consulta guardada = consultaRepository.save(consulta);
        auditoriaService.registrar("ABRIR_CONSULTA", "consultas/" + guardada.getId(), "EXITOSO");
        return respuesta(guardada);
    }

    public SignosVitales registrarSignos(String citaId, SignosVitalesRequest solicitud, String usuario) {
        boolean cerrada = consultaRepository.findByCitaId(citaId)
                .filter(c -> c.getEstado() == EstadoConsulta.CERRADA)
                .isPresent();
        if (cerrada) {
            throw new ReglaNegocioException("La consulta está cerrada");
        }
        if (!EN_ESPERA.equals(agendaClient.cita(citaId).estado())) {
            throw new ReglaNegocioException("La cita no está en espera");
        }
        SignosVitales signos = new SignosVitales();
        signos.setCitaId(citaId);
        signos.setPesoKg(solicitud.pesoKg());
        signos.setTemperatura(solicitud.temperatura());
        signos.setFrecuenciaCardiaca(solicitud.frecuenciaCardiaca());
        signos.setFrecuenciaRespiratoria(solicitud.frecuenciaRespiratoria());
        signos.setRegistradoPor(usuario);
        return signosRepository.save(signos);
    }

    public ConsultaResponse registrarClinico(String id, RegistroClinicoRequest solicitud) {
        Consulta consulta = abierta(id);
        consulta.setAnamnesis(solicitud.anamnesis().trim());
        consulta.setExamenFisico(solicitud.examenFisico().trim());
        consulta.setDiagnostico(solicitud.diagnostico().trim());
        consulta.setPlanTratamiento(solicitud.planTratamiento().trim());
        return respuesta(consultaRepository.save(consulta));
    }

    public ConsultaResponse cerrar(String id) {
        Consulta consulta = abierta(id);
        if (consulta.getDiagnostico() == null || consulta.getDiagnostico().isBlank()) {
            throw new ReglaNegocioException("La consulta no tiene diagnóstico");
        }
        // Primero ms-agenda: si no responde, la consulta sigue abierta y se puede reintentar.
        agendaClient.marcarAtendida(consulta.getCitaId());
        consulta.setEstado(EstadoConsulta.CERRADA);
        consulta.setFechaCierre(Instant.now());
        Consulta guardada = consultaRepository.save(consulta);
        auditoriaService.registrar("CERRAR_CONSULTA", "consultas/" + id, "EXITOSO");
        return respuesta(guardada);
    }

    public ConsultaResponse obtener(String id) {
        return respuesta(buscar(id));
    }

    public List<ConsultaResponse> deMascota(String mascotaId) {
        return consultaRepository.findByMascotaIdOrderByFechaAperturaDesc(mascotaId).stream().map(this::respuesta).toList();
    }

    public List<ItemPendienteResponse> itemsPendientes() {
        return consultaRepository.findByEstadoAndFacturadoFalse(EstadoConsulta.CERRADA).stream()
                .map(c -> new ItemPendienteResponse(c.getId(), "CONSULTA", "Consulta: " + c.getDiagnostico(), c.getMascotaId(),
                        c.getFechaCierre().atZone(ZoneId.systemDefault()).toLocalDate(), 1))
                .toList();
    }

    public Consulta abierta(String id) {
        Consulta consulta = buscar(id);
        if (consulta.getEstado() == EstadoConsulta.CERRADA) {
            throw new ReglaNegocioException("La consulta está cerrada");
        }
        return consulta;
    }

    private Consulta buscar(String id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La consulta no existe"));
    }

    private ConsultaResponse respuesta(Consulta consulta) {
        return ConsultaResponse.de(consulta, signosRepository.findById(consulta.getCitaId()).orElse(null));
    }
}
