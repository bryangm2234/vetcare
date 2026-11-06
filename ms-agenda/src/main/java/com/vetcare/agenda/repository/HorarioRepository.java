package com.vetcare.agenda.repository;

import com.vetcare.agenda.model.Horario;
import com.vetcare.agenda.model.TipoCita;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HorarioRepository extends MongoRepository<Horario, String> {

    List<Horario> findByTipo(TipoCita tipo);
}
