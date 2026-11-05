package com.vetcare.farmacia.repository;

import com.vetcare.farmacia.model.Proveedor;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProveedorRepository extends MongoRepository<Proveedor, String> {

    boolean existsByNit(String nit);
}
