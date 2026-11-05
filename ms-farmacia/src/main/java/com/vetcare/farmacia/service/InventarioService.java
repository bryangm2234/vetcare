package com.vetcare.farmacia.service;

import com.vetcare.farmacia.dto.AjusteRequest;
import com.vetcare.farmacia.exception.ConflictoException;
import com.vetcare.farmacia.exception.DatosInvalidosException;
import com.vetcare.farmacia.exception.RecursoNoEncontradoException;
import com.vetcare.farmacia.exception.ReglaNegocioException;
import com.vetcare.farmacia.model.Lote;
import com.vetcare.farmacia.model.MovimientoKardex;
import com.vetcare.farmacia.model.TipoMovimiento;
import com.vetcare.farmacia.repository.KardexRepository;
import com.vetcare.farmacia.repository.LoteRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Único punto que modifica el stock por lote y registra el kardex (RN-04).
 * Los descuentos son atómicos y condicionados, así que el stock nunca queda negativo.
 */
@Service
public class InventarioService {

    private static final String STOCK_INSUFICIENTE = "Stock insuficiente";

    /** Cantidad descontada de un lote, para poder revertirla. */
    public record Consumo(String medicamentoId, String loteId, int cantidad) {
    }

    private final LoteRepository loteRepository;
    private final KardexRepository kardexRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoriaService;

    public InventarioService(LoteRepository loteRepository, KardexRepository kardexRepository, MongoTemplate mongoTemplate,
                             AuditoriaService auditoriaService) {
        this.loteRepository = loteRepository;
        this.kardexRepository = kardexRepository;
        this.mongoTemplate = mongoTemplate;
        this.auditoriaService = auditoriaService;
    }

    public Lote registrarLote(String medicamentoId, String numero, LocalDate vencimiento, int cantidad, String responsable, String referencia) {
        String numeroLimpio = numero.trim();
        if (vencimiento.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de vencimiento ya pasó");
        }
        if (loteRepository.existsByMedicamentoIdAndNumero(medicamentoId, numeroLimpio)) {
            throw new ConflictoException("El lote ya existe para ese medicamento");
        }
        Lote lote = new Lote();
        lote.setMedicamentoId(medicamentoId);
        lote.setNumero(numeroLimpio);
        lote.setVencimiento(vencimiento);
        lote.setCantidad(cantidad);
        Lote guardado = loteRepository.save(lote);
        registrarMovimiento(guardado, TipoMovimiento.ENTRADA, cantidad, responsable, referencia);
        return guardado;
    }

    /** Stock que se puede entregar: no cuenta lotes vencidos. */
    public long stockDisponible(String medicamentoId) {
        return lotesDespachables(medicamentoId).stream().mapToLong(Lote::getCantidad).sum();
    }

    /** Descuenta aplicando FEFO. Si no alcanza, no deja ningún cambio. */
    public List<Consumo> descontarFefo(String medicamentoId, int cantidad, TipoMovimiento tipo, String responsable, String referencia) {
        List<Consumo> consumos = new ArrayList<>();
        int restante = cantidad;
        for (Lote lote : lotesDespachables(medicamentoId)) {
            if (restante == 0) {
                break;
            }
            int aDescontar = Math.min(restante, lote.getCantidad());
            Lote actualizado = restar(lote.getId(), aDescontar);
            if (actualizado == null) {
                // Otra petición usó este lote entre la lectura y el descuento: se pasa al siguiente.
                continue;
            }
            consumos.add(new Consumo(medicamentoId, lote.getId(), aDescontar));
            registrarMovimiento(actualizado, tipo, -aDescontar, responsable, referencia);
            restante -= aDescontar;
        }
        if (restante > 0) {
            revertir(consumos, responsable, referencia);
            throw new ConflictoException(STOCK_INSUFICIENTE);
        }
        return consumos;
    }

    public void revertir(List<Consumo> consumos, String responsable, String referencia) {
        for (Consumo consumo : consumos) {
            Lote actualizado = mongoTemplate.findAndModify(new Query(Criteria.where("_id").is(consumo.loteId())),
                    new Update().inc("cantidad", consumo.cantidad()), FindAndModifyOptions.options().returnNew(true), Lote.class);
            if (actualizado != null) {
                registrarMovimiento(actualizado, TipoMovimiento.REVERSION, consumo.cantidad(), responsable, referencia);
            }
        }
    }

    /** D11: la vacuna aplicada descuenta 1 unidad del lote indicado por su número. */
    public Lote consumirPorNumero(String numero, int cantidad, String responsable, String referencia) {
        List<Lote> lotes = loteRepository.findByNumero(numero.trim());
        if (lotes.isEmpty()) {
            throw new RecursoNoEncontradoException("El lote no existe");
        }
        if (lotes.size() > 1) {
            throw new ConflictoException("El número de lote existe en más de un medicamento");
        }
        Lote lote = lotes.get(0);
        if (lote.getVencimiento().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("El lote está vencido");
        }
        Lote actualizado = restar(lote.getId(), cantidad);
        if (actualizado == null) {
            throw new ConflictoException(STOCK_INSUFICIENTE);
        }
        registrarMovimiento(actualizado, TipoMovimiento.CONSUMO_VACUNA, -cantidad, responsable, referencia);
        return actualizado;
    }

    public Lote ajustar(String medicamentoId, AjusteRequest solicitud, String responsable) {
        if (solicitud.cantidad() == 0) {
            throw new DatosInvalidosException("La cantidad del ajuste no puede ser cero");
        }
        Lote lote = loteRepository.findById(solicitud.loteId())
                .filter(l -> l.getMedicamentoId().equals(medicamentoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("El lote no existe para ese medicamento"));
        Lote actualizado;
        if (solicitud.cantidad() < 0) {
            actualizado = restar(lote.getId(), -solicitud.cantidad());
            if (actualizado == null) {
                throw new ConflictoException(STOCK_INSUFICIENTE);
            }
        } else {
            actualizado = mongoTemplate.findAndModify(new Query(Criteria.where("_id").is(lote.getId())),
                    new Update().inc("cantidad", solicitud.cantidad()), FindAndModifyOptions.options().returnNew(true), Lote.class);
        }
        registrarMovimiento(actualizado, TipoMovimiento.AJUSTE, solicitud.cantidad(), responsable, solicitud.motivo().trim());
        auditoriaService.registrar("AJUSTAR_INVENTARIO", "lotes/" + lote.getId(), solicitud.motivo().trim());
        return actualizado;
    }

    private List<Lote> lotesDespachables(String medicamentoId) {
        return loteRepository.findByMedicamentoIdAndCantidadGreaterThanAndVencimientoGreaterThanEqualOrderByVencimiento(
                medicamentoId, 0, LocalDate.now());
    }

    private Lote restar(String loteId, int cantidad) {
        Query query = new Query(Criteria.where("_id").is(loteId).and("cantidad").gte(cantidad));
        return mongoTemplate.findAndModify(query, new Update().inc("cantidad", -cantidad),
                FindAndModifyOptions.options().returnNew(true), Lote.class);
    }

    private void registrarMovimiento(Lote lote, TipoMovimiento tipo, int cantidad, String responsable, String referencia) {
        MovimientoKardex movimiento = new MovimientoKardex();
        movimiento.setMedicamentoId(lote.getMedicamentoId());
        movimiento.setLoteId(lote.getId());
        movimiento.setLoteNumero(lote.getNumero());
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setSaldoLote(lote.getCantidad());
        movimiento.setResponsable(responsable);
        movimiento.setReferencia(referencia);
        kardexRepository.save(movimiento);
    }
}
