package com.vetcare.farmacia.config;

import com.vetcare.farmacia.model.Lote;
import com.vetcare.farmacia.model.Medicamento;
import com.vetcare.farmacia.model.Proveedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Crea los índices únicos sin impedir el arranque si MongoDB no responde.
 */
@Component
public class ArranqueDatos {

    private static final Logger log = LoggerFactory.getLogger(ArranqueDatos.class);

    private final MongoTemplate mongoTemplate;

    public ArranqueDatos(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preparar() {
        try {
            mongoTemplate.indexOps(Medicamento.class).createIndex(new Index().on("codigo", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps(Lote.class).createIndex(
                    new Index().on("medicamentoId", Sort.Direction.ASC).on("numero", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps(Proveedor.class).createIndex(new Index().on("nit", Sort.Direction.ASC).unique());
        } catch (DataAccessException ex) {
            log.warn("No se pudieron crear los índices porque MongoDB no respondió: {}", ex.getMessage());
        }
    }
}
