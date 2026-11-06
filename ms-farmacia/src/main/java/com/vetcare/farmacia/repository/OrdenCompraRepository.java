package com.vetcare.farmacia.repository;

import com.vetcare.farmacia.model.OrdenCompra;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrdenCompraRepository extends MongoRepository<OrdenCompra, String> {

    List<OrdenCompra> findByProveedorIdOrderByFechaDesc(String proveedorId);
}
