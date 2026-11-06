package com.vetcare.pacientes.service;

import com.vetcare.pacientes.dto.CambioContrasenaRequest;
import com.vetcare.pacientes.dto.PaginaResponse;
import com.vetcare.pacientes.dto.UsuarioRequest;
import com.vetcare.pacientes.dto.UsuarioResponse;
import com.vetcare.pacientes.exception.ConflictoException;
import com.vetcare.pacientes.exception.RecursoNoEncontradoException;
import com.vetcare.pacientes.exception.ReglaNegocioException;
import com.vetcare.pacientes.model.EstadoUsuario;
import com.vetcare.pacientes.model.Usuario;
import com.vetcare.pacientes.repository.PropietarioRepository;
import com.vetcare.pacientes.repository.UsuarioRepository;
import com.vetcare.pacientes.security.Rol;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PropietarioRepository propietarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository, PropietarioRepository propietarioRepository,
                          PasswordEncoder passwordEncoder, MongoTemplate mongoTemplate,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.propietarioRepository = propietarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
        this.auditoriaService = auditoriaService;
    }

    public UsuarioResponse crear(UsuarioRequest solicitud) {
        String correo = normalizarCorreo(solicitud.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException("El correo ya está registrado");
        }
        validarVinculo(solicitud);

        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(passwordEncoder.encode(solicitud.contrasena()));
        usuario.setRol(solicitud.rol());
        usuario.setPropietarioId(solicitud.rol() == Rol.PROPIETARIO ? solicitud.propietarioId() : null);
        usuario.setProveedorId(solicitud.rol() == Rol.PROVEEDOR ? solicitud.proveedorId() : null);
        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaService.registrar("CREAR_USUARIO", "usuarios/" + guardado.getId(), "EXITOSO");
        return UsuarioResponse.de(guardado);
    }

    public UsuarioResponse desactivar(String id, String administradorId) {
        if (id.equals(administradorId)) {
            throw new ReglaNegocioException("No puede desactivar su propio usuario");
        }
        Usuario usuario = buscar(id);
        usuario.setEstado(EstadoUsuario.INACTIVO);
        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaService.registrar("DESACTIVAR_USUARIO", "usuarios/" + id, "EXITOSO");
        return UsuarioResponse.de(guardado);
    }

    public void cambiarContrasena(String usuarioId, CambioContrasenaRequest solicitud) {
        Usuario usuario = buscar(usuarioId);
        if (!passwordEncoder.matches(solicitud.contrasenaActual(), usuario.getContrasenaHash())) {
            auditoriaService.registrar("CAMBIAR_CONTRASENA", "usuarios/" + usuarioId, "FALLIDO");
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        usuario.setContrasenaHash(passwordEncoder.encode(solicitud.contrasenaNueva()));
        usuarioRepository.save(usuario);
        auditoriaService.registrar("CAMBIAR_CONTRASENA", "usuarios/" + usuarioId, "EXITOSO");
    }

    public PaginaResponse<UsuarioResponse> consultar(Rol rol, EstadoUsuario estado, int pagina, int tamano) {
        Query query = new Query();
        if (rol != null) {
            query.addCriteria(Criteria.where("rol").is(rol));
        }
        if (estado != null) {
            query.addCriteria(Criteria.where("estado").is(estado));
        }
        long total = mongoTemplate.count(query, Usuario.class);
        int paginaValida = PaginaResponse.paginaValida(pagina);
        int tamanoValido = PaginaResponse.tamanoValido(tamano);
        query.with(PageRequest.of(paginaValida, tamanoValido, Sort.by("nombre")));
        return PaginaResponse.de(mongoTemplate.find(query, Usuario.class), UsuarioResponse::de, paginaValida, tamanoValido, total);
    }

    private void validarVinculo(UsuarioRequest solicitud) {
        if (solicitud.rol() == Rol.PROPIETARIO) {
            if (solicitud.propietarioId() == null || solicitud.propietarioId().isBlank()) {
                throw new ReglaNegocioException("El usuario debe estar vinculado a un propietario");
            }
            if (!propietarioRepository.existsById(solicitud.propietarioId())) {
                throw new RecursoNoEncontradoException("El propietario no existe");
            }
        }
        if (solicitud.rol() == Rol.PROVEEDOR && (solicitud.proveedorId() == null || solicitud.proveedorId().isBlank())) {
            throw new ReglaNegocioException("El usuario debe estar vinculado a un proveedor");
        }
    }

    private Usuario buscar(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
