package com.vetcare.historia.repository;

import com.vetcare.historia.model.Vacuna;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VacunaRepository extends MongoRepository<Vacuna, String> {

    List<Vacuna> findByMascotaIdAndVacunaOrderByDosisDesc(String mascotaId, String vacuna);

    List<Vacuna> findByMascotaIdOrderByFechaDesc(String mascotaId);

    List<Vacuna> findByVigenteTrueAndProximaDosisLessThanEqualOrderByProximaDosis(LocalDate limite);
}
