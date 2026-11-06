package com.vetcare.agenda.service;

import com.vetcare.agenda.dto.PaginaResponse;
import com.vetcare.agenda.model.Auditoria;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Registra los accesos y las operaciones críticas en la base de datos del microservicio (HU-009).
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final MongoTemplate mongoTemplate;

    public AuditoriaService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void registrar(String accion, String recurso, String resultado) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        registrar(autenticacion != null ? autenticacion.getName() : "sistema", accion, recurso, resultado);
    }

    public void registrar(String usuario, String accion, String recurso, String resultado) {
        try {
            mongoTemplate.save(new Auditoria(Instant.now(), usuario, accion, recurso, resultado));
        } catch (DataAccessException ex) {
            log.error("No se pudo registrar la auditoría de {} sobre {} por {}", accion, recurso, usuario, ex);
        }
    }

    public PaginaResponse<Auditoria> consultar(String usuario, String accion, Instant desde, Instant hasta,
                                               int pagina, int tamano) {
        Query query = new Query();
        if (usuario != null && !usuario.isBlank()) {
            query.addCriteria(Criteria.where("usuario").is(usuario));
        }
        if (accion != null && !accion.isBlank()) {
            query.addCriteria(Criteria.where("accion").is(accion));
        }
        if (desde != null || hasta != null) {
            Criteria fecha = Criteria.where("fecha");
            if (desde != null) {
                fecha = fecha.gte(desde);
            }
            if (hasta != null) {
                fecha = fecha.lte(hasta);
            }
            query.addCriteria(fecha);
        }
        long total = mongoTemplate.count(query, Auditoria.class);
        int paginaValida = PaginaResponse.paginaValida(pagina);
        int tamanoValido = PaginaResponse.tamanoValido(tamano);
        query.with(PageRequest.of(paginaValida, tamanoValido, Sort.by(Sort.Direction.DESC, "fecha")));
        return PaginaResponse.de(mongoTemplate.find(query, Auditoria.class), a -> a, paginaValida, tamanoValido, total);
    }
}
