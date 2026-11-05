package com.vetcare.pacientes.repository;

import com.vetcare.pacientes.model.Mascota;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MascotaRepository extends MongoRepository<Mascota, String> {

    List<Mascota> findByPropietarioId(String propietarioId);
}
