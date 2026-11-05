package com.vetcare.farmacia.repository;

import com.vetcare.farmacia.model.Medicamento;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MedicamentoRepository extends MongoRepository<Medicamento, String> {

    boolean existsByCodigo(String codigo);
}
