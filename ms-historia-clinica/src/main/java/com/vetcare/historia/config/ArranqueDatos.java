package com.vetcare.historia.config;

import com.vetcare.historia.model.Consulta;
import com.vetcare.historia.model.EsquemaVacunacion;
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
            mongoTemplate.indexOps(Consulta.class).createIndex(new Index().on("citaId", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps(EsquemaVacunacion.class).createIndex(
                    new Index().on("especie", Sort.Direction.ASC).on("vacuna", Sort.Direction.ASC).unique());
        } catch (DataAccessException ex) {
            log.warn("No se pudieron crear los índices porque MongoDB no respondió: {}", ex.getMessage());
        }
    }
}
