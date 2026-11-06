package com.vetcare.agenda.service;

import com.vetcare.agenda.exception.ConflictoException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Marca ítems como facturados de forma atómica para impedir cobros dobles (HU-061) y los libera al anular (HU-065).
 */
@Service
public class MarcadorFacturacion {

    private final MongoTemplate mongoTemplate;

    public MarcadorFacturacion(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void marcar(Class<?> tipo, List<String> ids, boolean facturado) {
        List<String> marcados = new ArrayList<>();
        for (String id : ids) {
            if (cambiar(tipo, id, facturado)) {
                marcados.add(id);
            } else {
                marcados.forEach(m -> cambiar(tipo, m, !facturado));
                throw new ConflictoException("El ítem " + id + " no se puede marcar: no existe o ya cambió de estado");
            }
        }
    }

    private boolean cambiar(Class<?> tipo, String id, boolean facturado) {
        Query query = new Query(Criteria.where("_id").is(id).and("facturado").is(!facturado));
        return mongoTemplate.updateFirst(query, new Update().set("facturado", facturado), tipo).getModifiedCount() == 1;
    }
}
