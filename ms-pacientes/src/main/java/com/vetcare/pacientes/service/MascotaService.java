package com.vetcare.pacientes.service;

import com.vetcare.pacientes.dto.AntecedentesRequest;
import com.vetcare.pacientes.dto.EstadoMascotaRequest;
import com.vetcare.pacientes.dto.MascotaRequest;
import com.vetcare.pacientes.dto.MascotaResponse;
import com.vetcare.pacientes.dto.ReporteMascotaItem;
import com.vetcare.pacientes.exception.RecursoNoEncontradoException;
import com.vetcare.pacientes.exception.ReglaNegocioException;
import com.vetcare.pacientes.model.EstadoMascota;
import com.vetcare.pacientes.model.Mascota;
import com.vetcare.pacientes.model.TransferenciaMascota;
import com.vetcare.pacientes.repository.MascotaRepository;
import com.vetcare.pacientes.repository.PropietarioRepository;
import com.vetcare.pacientes.security.Rol;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final PropietarioRepository propietarioRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoriaService;

    public MascotaService(MascotaRepository mascotaRepository, PropietarioRepository propietarioRepository,
                          MongoTemplate mongoTemplate, AuditoriaService auditoriaService) {
        this.mascotaRepository = mascotaRepository;
        this.propietarioRepository = propietarioRepository;
        this.mongoTemplate = mongoTemplate;
        this.auditoriaService = auditoriaService;
    }

    public MascotaResponse registrar(MascotaRequest solicitud) {
        validarPropietario(solicitud.propietarioId());
        Mascota mascota = new Mascota();
        mascota.setNombre(solicitud.nombre().trim());
        mascota.setEspecie(solicitud.especie().trim());
        mascota.setRaza(solicitud.raza().trim());
        mascota.setSexo(solicitud.sexo());
        mascota.setFechaNacimiento(solicitud.fechaNacimiento());
        mascota.setPesoKg(solicitud.pesoKg());
        mascota.setPropietarioId(solicitud.propietarioId());
        Mascota guardada = mascotaRepository.save(mascota);
        auditoriaService.registrar("REGISTRAR_MASCOTA", "mascotas/" + guardada.getId(), "EXITOSO");
        return MascotaResponse.de(guardada);
    }

    public MascotaResponse obtener(String id, Rol rol, String propietarioIdToken) {
        Mascota mascota = buscar(id);
        if (rol == Rol.PROPIETARIO && !mascota.getPropietarioId().equals(propietarioIdToken)) {
            throw new AccessDeniedException("La mascota pertenece a otro propietario");
        }
        return MascotaResponse.de(mascota);
    }

    public MascotaResponse registrarAntecedentes(String id, AntecedentesRequest solicitud) {
        Mascota mascota = buscar(id);
        mascota.setAlergias(solicitud.alergias());
        mascota.setCondicionesCronicas(solicitud.condicionesCronicas());
        Mascota guardada = mascotaRepository.save(mascota);
        auditoriaService.registrar("REGISTRAR_ANTECEDENTES", "mascotas/" + id, "EXITOSO");
        return MascotaResponse.de(guardada);
    }

    public MascotaResponse cambiarEstado(String id, EstadoMascotaRequest solicitud) {
        Mascota mascota = buscar(id);
        if (mascota.getEstado() == EstadoMascota.FALLECIDA) {
            throw new ReglaNegocioException("El fallecimiento de una mascota no se puede revertir");
        }
        if (solicitud.estado() == EstadoMascota.FALLECIDA) {
            if (solicitud.fechaFallecimiento() == null) {
                throw new ReglaNegocioException("Debe indicar la fecha de fallecimiento");
            }
            mascota.setFechaFallecimiento(solicitud.fechaFallecimiento());
        }
        mascota.setEstado(solicitud.estado());
        Mascota guardada = mascotaRepository.save(mascota);
        auditoriaService.registrar("CAMBIAR_ESTADO_MASCOTA", "mascotas/" + id, solicitud.estado().name());
        return MascotaResponse.de(guardada);
    }

    public MascotaResponse transferir(String id, String nuevoPropietarioId) {
        Mascota mascota = buscar(id);
        if (mascota.getEstado() == EstadoMascota.FALLECIDA) {
            throw new ReglaNegocioException("No se puede transferir una mascota fallecida");
        }
        validarPropietario(nuevoPropietarioId);
        mascota.getTransferencias().add(new TransferenciaMascota(mascota.getPropietarioId(), nuevoPropietarioId, Instant.now()));
        mascota.setPropietarioId(nuevoPropietarioId);
        Mascota guardada = mascotaRepository.save(mascota);
        auditoriaService.registrar("TRANSFERIR_MASCOTA", "mascotas/" + id, "EXITOSO");
        return MascotaResponse.de(guardada);
    }

    public List<ReporteMascotaItem> reportePorEspecieYRaza() {
        Aggregation agregacion = Aggregation.newAggregation(
                Aggregation.group("especie", "raza").count().as("total"),
                Aggregation.project("total").and("_id.especie").as("especie").and("_id.raza").as("raza").andExclude("_id"),
                Aggregation.sort(Sort.by("especie", "raza")));
        return mongoTemplate.aggregate(agregacion, Mascota.class, ReporteMascotaItem.class).getMappedResults();
    }

    private void validarPropietario(String propietarioId) {
        if (!propietarioRepository.existsById(propietarioId)) {
            throw new RecursoNoEncontradoException("El propietario no existe");
        }
    }

    private Mascota buscar(String id) {
        return mascotaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La mascota no existe"));
    }
}
