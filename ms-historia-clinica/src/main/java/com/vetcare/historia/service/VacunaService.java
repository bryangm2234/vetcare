package com.vetcare.historia.service;

import com.vetcare.historia.client.FarmaciaClient;
import com.vetcare.historia.client.PacientesClient;
import com.vetcare.historia.dto.AlertaVacunaResponse;
import com.vetcare.historia.dto.EsquemaVacunacionRequest;
import com.vetcare.historia.dto.MascotaInfo;
import com.vetcare.historia.dto.VacunaRequest;
import com.vetcare.historia.dto.VacunaResponse;
import com.vetcare.historia.exception.ConflictoException;
import com.vetcare.historia.exception.ReglaNegocioException;
import com.vetcare.historia.model.EsquemaVacunacion;
import com.vetcare.historia.model.Vacuna;
import com.vetcare.historia.repository.EsquemaVacunacionRepository;
import com.vetcare.historia.repository.VacunaRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Esquemas, dosis aplicadas y alertas de vacunación (RN-01).
 */
@Service
public class VacunaService {

    private final EsquemaVacunacionRepository esquemaRepository;
    private final VacunaRepository vacunaRepository;
    private final PacientesClient pacientesClient;
    private final FarmaciaClient farmaciaClient;
    private final AuditoriaService auditoriaService;

    public VacunaService(EsquemaVacunacionRepository esquemaRepository, VacunaRepository vacunaRepository,
                         PacientesClient pacientesClient, FarmaciaClient farmaciaClient, AuditoriaService auditoriaService) {
        this.esquemaRepository = esquemaRepository;
        this.vacunaRepository = vacunaRepository;
        this.pacientesClient = pacientesClient;
        this.farmaciaClient = farmaciaClient;
        this.auditoriaService = auditoriaService;
    }

    public EsquemaVacunacion crearEsquema(EsquemaVacunacionRequest solicitud) {
        String especie = solicitud.especie().trim();
        String vacuna = solicitud.vacuna().trim();
        if (esquemaRepository.existsByEspecieAndVacuna(especie, vacuna)) {
            throw new ConflictoException("Ya existe esa vacuna para la especie");
        }
        EsquemaVacunacion esquema = new EsquemaVacunacion();
        esquema.setEspecie(especie);
        esquema.setVacuna(vacuna);
        esquema.setDosis(solicitud.dosis());
        esquema.setIntervaloDias(solicitud.intervaloDias());
        return esquemaRepository.save(esquema);
    }

    public List<EsquemaVacunacion> esquemasDe(String especie) {
        return especie != null && !especie.isBlank() ? esquemaRepository.findByEspecie(especie.trim()) : esquemaRepository.findAll();
    }

    public VacunaResponse registrar(VacunaRequest solicitud, String veterinarioId) {
        MascotaInfo mascota = pacientesClient.mascota(solicitud.mascotaId());
        String nombreVacuna = solicitud.vacuna().trim();
        EsquemaVacunacion esquema = esquemaRepository.findByEspecieAndVacuna(mascota.especie(), nombreVacuna)
                .orElseThrow(() -> new ReglaNegocioException("La vacuna no tiene esquema para la especie"));

        List<Vacuna> aplicadas = vacunaRepository.findByMascotaIdAndVacunaOrderByDosisDesc(mascota.id(), nombreVacuna);
        int dosis = aplicadas.isEmpty() ? 1 : aplicadas.get(0).getDosis() + 1;
        if (dosis > esquema.getDosis()) {
            throw new ReglaNegocioException("La mascota ya recibió todas las dosis del esquema");
        }

        boolean descontado = farmaciaClient.descontarVacuna(solicitud.lote().trim());

        Vacuna vacuna = new Vacuna();
        vacuna.setMascotaId(mascota.id());
        vacuna.setMascotaNombre(mascota.nombre());
        vacuna.setPropietarioId(mascota.propietarioId());
        vacuna.setVacuna(nombreVacuna);
        vacuna.setDosis(dosis);
        vacuna.setLote(solicitud.lote().trim());
        vacuna.setFecha(solicitud.fecha());
        vacuna.setProximaDosis(proximaDosis(solicitud.fecha(), dosis, esquema));
        vacuna.setInventarioDescontado(descontado);
        vacuna.setVeterinarioId(veterinarioId);
        Vacuna guardada = vacunaRepository.save(vacuna);

        // La nueva dosis atiende la alerta de la anterior.
        if (!aplicadas.isEmpty()) {
            Vacuna anterior = aplicadas.get(0);
            anterior.setVigente(false);
            vacunaRepository.save(anterior);
        }
        auditoriaService.registrar("REGISTRAR_VACUNA", "vacunas/" + guardada.getId(), "EXITOSO");
        return VacunaResponse.de(guardada);
    }

    /** RN-01: la próxima dosis es la fecha aplicada más el intervalo; la última dosis no tiene siguiente. */
    static LocalDate proximaDosis(LocalDate fecha, int dosis, EsquemaVacunacion esquema) {
        return dosis < esquema.getDosis() ? fecha.plusDays(esquema.getIntervaloDias()) : null;
    }

    public List<AlertaVacunaResponse> alertas(int dias) {
        LocalDate hoy = LocalDate.now();
        return vacunaRepository.findByVigenteTrueAndProximaDosisLessThanEqualOrderByProximaDosis(hoy.plusDays(Math.max(dias, 0))).stream()
                .map(v -> new AlertaVacunaResponse(v.getMascotaId(), v.getMascotaNombre(), v.getPropietarioId(), v.getVacuna(), v.getDosis(),
                        v.getProximaDosis(), v.getProximaDosis().isBefore(hoy) ? "VENCIDA" : "PROXIMA"))
                .toList();
    }

    public List<VacunaResponse> deMascota(String mascotaId) {
        return vacunaRepository.findByMascotaIdOrderByFechaDesc(mascotaId).stream().map(VacunaResponse::de).toList();
    }

    /** HU-039: ms-pacientes rechaza con 403 si la mascota no pertenece al propietario del token. */
    public List<VacunaResponse> carne(String mascotaId) {
        pacientesClient.mascota(mascotaId);
        return deMascota(mascotaId);
    }
}
