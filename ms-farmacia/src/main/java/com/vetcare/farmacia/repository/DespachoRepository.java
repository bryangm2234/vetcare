package com.vetcare.farmacia.repository;

import com.vetcare.farmacia.model.Despacho;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DespachoRepository extends MongoRepository<Despacho, String> {

    List<Despacho> findByFacturadoFalse();
}
