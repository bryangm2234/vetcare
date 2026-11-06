package com.vetcare.facturacion.repository;

import com.vetcare.facturacion.model.Factura;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FacturaRepository extends MongoRepository<Factura, String> {

    List<Factura> findByPropietarioIdOrderByFechaDesc(String propietarioId);

    List<Factura> findByPagosCajaId(String cajaId);
}
