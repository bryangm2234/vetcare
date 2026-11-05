package com.vetcare.agenda.config;

import com.vetcare.agenda.model.Cita;
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
 * Crea el índice que impide reservar dos veces la misma franja (RN-03), sin impedir el arranque si MongoDB no responde.
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
            mongoTemplate.indexOps(Cita.class).createIndex(new Index().on("claveFranja", Sort.Direction.ASC).unique().sparse());
        } catch (DataAccessException ex) {
            log.warn("No se pudieron crear los índices porque MongoDB no respondió: {}", ex.getMessage());
        }
    }
}
