package com.vetcare.pacientes.repository;

import com.vetcare.pacientes.model.Usuario;
import com.vetcare.pacientes.security.Rol;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByRol(Rol rol);
}
