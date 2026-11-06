package com.vetcare.facturacion.repository;

import com.vetcare.facturacion.model.Tarifa;
import com.vetcare.facturacion.model.TipoServicio;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TarifaRepository extends MongoRepository<Tarifa, String> {

    boolean existsByServicio(TipoServicio servicio);

    Optional<Tarifa> findByServicio(TipoServicio servicio);
}
