package com.vetcare.agenda.repository;

import com.vetcare.agenda.model.Cita;
import com.vetcare.agenda.model.EstadoCita;
import com.vetcare.agenda.model.TipoCita;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CitaRepository extends MongoRepository<Cita, String> {

    boolean existsByClaveFranja(String claveFranja);

    List<Cita> findByRecursoIdAndFechaOrderByHora(String recursoId, LocalDate fecha);

    List<Cita> findByFechaAndEstadoAndRecordatorioEnviadoFalse(LocalDate fecha, EstadoCita estado);

    List<Cita> findByTipoAndEstadoAndFacturadoFalse(TipoCita tipo, EstadoCita estado);
}
