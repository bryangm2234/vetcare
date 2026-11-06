package com.vetcare.farmacia.repository;

import com.vetcare.farmacia.model.MovimientoKardex;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface KardexRepository extends MongoRepository<MovimientoKardex, String> {
}
