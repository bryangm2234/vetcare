package com.vetcare.historia.repository;

import com.vetcare.historia.model.EstadoHospitalizacion;
import com.vetcare.historia.model.Hospitalizacion;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HospitalizacionRepository extends MongoRepository<Hospitalizacion, String> {

    boolean existsByCubiculoOcupado(String cubiculo);

    boolean existsByMascotaInternada(String mascotaId);

    List<Hospitalizacion> findByMascotaIdOrderByFechaIngresoDesc(String mascotaId);

    List<Hospitalizacion> findByEstadoAndFacturadoFalse(EstadoHospitalizacion estado);
}
