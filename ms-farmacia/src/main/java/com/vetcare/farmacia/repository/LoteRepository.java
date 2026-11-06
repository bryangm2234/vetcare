package com.vetcare.farmacia.repository;

import com.vetcare.farmacia.model.Lote;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LoteRepository extends MongoRepository<Lote, String> {

    boolean existsByMedicamentoIdAndNumero(String medicamentoId, String numero);

    List<Lote> findByNumero(String numero);

    List<Lote> findByMedicamentoIdOrderByVencimiento(String medicamentoId);

    /** Lotes con stock que aún no vencen, primero el que vence antes (FEFO). */
    List<Lote> findByMedicamentoIdAndCantidadGreaterThanAndVencimientoGreaterThanEqualOrderByVencimiento(
            String medicamentoId, int cantidad, LocalDate desde);

    List<Lote> findByCantidadGreaterThanAndVencimientoBetweenOrderByVencimiento(int cantidad, LocalDate desde, LocalDate hasta);
}
