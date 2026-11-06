package com.vetcare.facturacion.repository;

import com.vetcare.facturacion.model.Caja;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CajaRepository extends MongoRepository<Caja, String> {

    Optional<Caja> findByCajeroConCajaAbierta(String cajeroId);
}
