package com.vetcare.historia.repository;

import com.vetcare.historia.model.EsquemaVacunacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EsquemaVacunacionRepository extends MongoRepository<EsquemaVacunacion, String> {

    boolean existsByEspecieAndVacuna(String especie, String vacuna);

    Optional<EsquemaVacunacion> findByEspecieAndVacuna(String especie, String vacuna);

    List<EsquemaVacunacion> findByEspecie(String especie);
}
