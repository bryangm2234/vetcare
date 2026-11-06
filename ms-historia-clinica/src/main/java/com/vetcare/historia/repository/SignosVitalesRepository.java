package com.vetcare.historia.repository;

import com.vetcare.historia.model.SignosVitales;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SignosVitalesRepository extends MongoRepository<SignosVitales, String> {
}
