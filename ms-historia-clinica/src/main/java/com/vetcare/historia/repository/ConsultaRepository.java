package com.vetcare.historia.repository;

import com.vetcare.historia.model.Consulta;
import com.vetcare.historia.model.EstadoConsulta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConsultaRepository extends MongoRepository<Consulta, String> {

    boolean existsByCitaId(String citaId);

    Optional<Consulta> findByCitaId(String citaId);

    List<Consulta> findByMascotaIdOrderByFechaAperturaDesc(String mascotaId);

    List<Consulta> findByEstadoAndFacturadoFalse(EstadoConsulta estado);
}
