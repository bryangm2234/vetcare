package com.vetcare.facturacion.config;

import com.vetcare.facturacion.model.Caja;
import com.vetcare.facturacion.model.Tarifa;
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
            mongoTemplate.indexOps(Tarifa.class).createIndex(new Index().on("servicio", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps(Caja.class).createIndex(new Index().on("cajeroConCajaAbierta", Sort.Direction.ASC).unique().sparse());
        } catch (DataAccessException ex) {
            log.warn("No se pudieron crear los índices porque MongoDB no respondió: {}", ex.getMessage());
        }
    }
}
