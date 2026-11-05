package com.vetcare.pacientes.config;

import com.vetcare.pacientes.model.Propietario;
import com.vetcare.pacientes.model.Usuario;
import com.vetcare.pacientes.repository.UsuarioRepository;
import com.vetcare.pacientes.security.Rol;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea los índices únicos y el primer administrador (HU-001) sin impedir el arranque si MongoDB no responde.
 */
@Component
public class ArranqueDatos {

    private static final Logger log = LoggerFactory.getLogger(ArranqueDatos.class);

    private final MongoTemplate mongoTemplate;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String correoAdmin;
    private final String contrasenaAdmin;

    public ArranqueDatos(MongoTemplate mongoTemplate, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                         @Value("${vetcare.admin.correo:}") String correoAdmin,
                         @Value("${vetcare.admin.contrasena:}") String contrasenaAdmin) {
        this.mongoTemplate = mongoTemplate;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.correoAdmin = correoAdmin;
        this.contrasenaAdmin = contrasenaAdmin;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preparar() {
        try {
            mongoTemplate.indexOps(Usuario.class).createIndex(new Index().on("correo", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps(Propietario.class).createIndex(new Index().on("documento", Sort.Direction.ASC).unique());
            crearAdministradorInicial();
        } catch (DataAccessException ex) {
            log.warn("No se pudieron preparar los datos iniciales porque MongoDB no respondió: {}", ex.getMessage());
        }
    }

    void crearAdministradorInicial() {
        if (correoAdmin.isBlank() || contrasenaAdmin.isBlank() || usuarioRepository.existsByRol(Rol.ADMINISTRADOR)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setCorreo(correoAdmin.trim().toLowerCase(Locale.ROOT));
        admin.setContrasenaHash(passwordEncoder.encode(contrasenaAdmin));
        admin.setRol(Rol.ADMINISTRADOR);
        usuarioRepository.save(admin);
        log.info("Se creó el administrador inicial");
    }
}
