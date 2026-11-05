package com.vetcare.historia.repository;

import com.vetcare.historia.model.Formula;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FormulaRepository extends MongoRepository<Formula, String> {

    List<Formula> findByMascotaIdOrderByFechaDesc(String mascotaId);
}
