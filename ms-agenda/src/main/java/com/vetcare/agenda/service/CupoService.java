package com.vetcare.agenda.service;

import com.vetcare.agenda.exception.ConflictoException;
import com.vetcare.agenda.model.CupoDiario;
import java.time.LocalDate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Reserva y libera cupos de forma atómica (RN-03).
 */
@Service
public class CupoService {

    private final MongoTemplate mongoTemplate;

    public CupoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void reservar(String recursoId, LocalDate fecha, int cuposDiarios) {
        Query disponible = new Query(Criteria.where("_id").is(clave(recursoId, fecha)).and("usados").lt(cuposDiarios));
        try {
            CupoDiario cupo = mongoTemplate.findAndModify(disponible, new Update().inc("usados", 1),
                    FindAndModifyOptions.options().upsert(true).returnNew(true), CupoDiario.class);
            if (cupo == null) {
                throw new ConflictoException("No hay cupos disponibles");
            }
        } catch (DuplicateKeyException ex) {
            // El documento existe pero no cumple el filtro: los cupos del día ya están agotados.
            throw new ConflictoException("No hay cupos disponibles");
        }
    }

    public void liberar(String recursoId, LocalDate fecha) {
        Query usado = new Query(Criteria.where("_id").is(clave(recursoId, fecha)).and("usados").gt(0));
        mongoTemplate.updateFirst(usado, new Update().inc("usados", -1), CupoDiario.class);
    }

    public int usados(String recursoId, LocalDate fecha) {
        CupoDiario cupo = mongoTemplate.findById(clave(recursoId, fecha), CupoDiario.class);
        return cupo != null ? cupo.getUsados() : 0;
    }

    private String clave(String recursoId, LocalDate fecha) {
        return recursoId + "|" + fecha;
    }
}
