package com.vetcare.pacientes.service;

import com.vetcare.pacientes.dto.ContactoRequest;
import com.vetcare.pacientes.dto.MascotaResponse;
import com.vetcare.pacientes.dto.PaginaResponse;
import com.vetcare.pacientes.dto.PropietarioRequest;
import com.vetcare.pacientes.dto.PropietarioResponse;
import com.vetcare.pacientes.exception.ConflictoException;
import com.vetcare.pacientes.exception.RecursoNoEncontradoException;
import com.vetcare.pacientes.model.Propietario;
import com.vetcare.pacientes.repository.MascotaRepository;
import com.vetcare.pacientes.repository.PropietarioRepository;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class PropietarioService {

    private final PropietarioRepository propietarioRepository;
    private final MascotaRepository mascotaRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoriaService;

    public PropietarioService(PropietarioRepository propietarioRepository, MascotaRepository mascotaRepository,
                              MongoTemplate mongoTemplate, AuditoriaService auditoriaService) {
        this.propietarioRepository = propietarioRepository;
        this.mascotaRepository = mascotaRepository;
        this.mongoTemplate = mongoTemplate;
        this.auditoriaService = auditoriaService;
    }

    public PropietarioResponse registrar(PropietarioRequest solicitud) {
        String documento = solicitud.documento().trim();
        if (propietarioRepository.existsByDocumento(documento)) {
            throw new ConflictoException("El documento ya está registrado");
        }
        Propietario propietario = new Propietario();
        propietario.setDocumento(documento);
        propietario.setNombre(solicitud.nombre().trim());
        propietario.setNombreNormalizado(TextoNormalizado.de(solicitud.nombre()));
        propietario.setTelefono(solicitud.telefono().trim());
        propietario.setCorreo(solicitud.correo().trim());
        propietario.setDireccion(solicitud.direccion().trim());
        Propietario guardado = propietarioRepository.save(propietario);
        auditoriaService.registrar("REGISTRAR_PROPIETARIO", "propietarios/" + guardado.getId(), "EXITOSO");
        return PropietarioResponse.de(guardado);
    }

    public PaginaResponse<PropietarioResponse> buscar(String documento, String nombre, int pagina, int tamano) {
        Query query = new Query();
        if (documento != null && !documento.isBlank()) {
            query.addCriteria(Criteria.where("documento").is(documento.trim()));
        }
        if (nombre != null && !nombre.isBlank()) {
            query.addCriteria(Criteria.where("nombreNormalizado").regex(Pattern.quote(TextoNormalizado.de(nombre))));
        }
        long total = mongoTemplate.count(query, Propietario.class);
        int paginaValida = PaginaResponse.paginaValida(pagina);
        int tamanoValido = PaginaResponse.tamanoValido(tamano);
        query.with(PageRequest.of(paginaValida, tamanoValido, Sort.by("nombreNormalizado")));
        return PaginaResponse.de(mongoTemplate.find(query, Propietario.class), PropietarioResponse::de, paginaValida, tamanoValido, total);
    }

    public PropietarioResponse obtener(String id) {
        return PropietarioResponse.de(buscarPropietario(id));
    }

    public PropietarioResponse actualizarContacto(String id, ContactoRequest solicitud) {
        Propietario propietario = buscarPropietario(id);
        propietario.setTelefono(solicitud.telefono().trim());
        propietario.setCorreo(solicitud.correo().trim());
        propietario.setDireccion(solicitud.direccion().trim());
        Propietario guardado = propietarioRepository.save(propietario);
        auditoriaService.registrar("ACTUALIZAR_CONTACTO", "propietarios/" + id, "EXITOSO");
        return PropietarioResponse.de(guardado);
    }

    public List<MascotaResponse> mascotasDe(String id) {
        buscarPropietario(id);
        return mascotaRepository.findByPropietarioId(id).stream().map(MascotaResponse::de).toList();
    }

    private Propietario buscarPropietario(String id) {
        return propietarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El propietario no existe"));
    }
}
