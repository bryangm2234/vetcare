package com.vetcare.pacientes.repository;

import com.vetcare.pacientes.model.Propietario;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PropietarioRepository extends MongoRepository<Propietario, String> {

    boolean existsByDocumento(String documento);
}
