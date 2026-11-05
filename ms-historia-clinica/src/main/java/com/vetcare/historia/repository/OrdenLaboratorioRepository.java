package com.vetcare.historia.repository;

import com.vetcare.historia.model.EstadoOrdenLaboratorio;
import com.vetcare.historia.model.OrdenLaboratorio;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrdenLaboratorioRepository extends MongoRepository<OrdenLaboratorio, String> {

    Page<OrdenLaboratorio> findByEstado(EstadoOrdenLaboratorio estado, Pageable paginacion);

    List<OrdenLaboratorio> findByMascotaIdOrderByFechaOrdenDesc(String mascotaId);

    List<OrdenLaboratorio> findByEstadoAndFacturadoFalse(EstadoOrdenLaboratorio estado);
}
