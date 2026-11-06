package com.vetcare.farmacia.service;

import com.vetcare.farmacia.dto.AlertaStockResponse;
import com.vetcare.farmacia.dto.LoteProximoResponse;
import com.vetcare.farmacia.dto.LoteRequest;
import com.vetcare.farmacia.dto.LoteResponse;
import com.vetcare.farmacia.dto.MedicamentoRequest;
import com.vetcare.farmacia.dto.MedicamentoResponse;
import com.vetcare.farmacia.dto.PaginaResponse;
import com.vetcare.farmacia.exception.ConflictoException;
import com.vetcare.farmacia.exception.RecursoNoEncontradoException;
import com.vetcare.farmacia.model.Lote;
import com.vetcare.farmacia.model.Medicamento;
import com.vetcare.farmacia.model.MovimientoKardex;
import com.vetcare.farmacia.repository.LoteRepository;
import com.vetcare.farmacia.repository.MedicamentoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;
    private final LoteRepository loteRepository;
    private final InventarioService inventarioService;
    private final MongoTemplate mongoTemplate;
    private final AuditoriaService auditoriaService;

    public MedicamentoService(MedicamentoRepository medicamentoRepository, LoteRepository loteRepository,
                              InventarioService inventarioService, MongoTemplate mongoTemplate, AuditoriaService auditoriaService) {
        this.medicamentoRepository = medicamentoRepository;
        this.loteRepository = loteRepository;
        this.inventarioService = inventarioService;
        this.mongoTemplate = mongoTemplate;
        this.auditoriaService = auditoriaService;
    }

    public MedicamentoResponse registrar(MedicamentoRequest solicitud) {
        String codigo = solicitud.codigo().trim();
        if (medicamentoRepository.existsByCodigo(codigo)) {
            throw new ConflictoException("El código ya existe");
        }
        Medicamento medicamento = new Medicamento();
        medicamento.setCodigo(codigo);
        medicamento.setNombre(solicitud.nombre().trim());
        medicamento.setTipo(solicitud.tipo());
        medicamento.setPresentacion(solicitud.presentacion().trim());
        medicamento.setPrecio(solicitud.precio());
        medicamento.setStockMinimo(solicitud.stockMinimo());
        Medicamento guardado = medicamentoRepository.save(medicamento);
        auditoriaService.registrar("REGISTRAR_MEDICAMENTO", "medicamentos/" + guardado.getId(), "EXITOSO");
        return MedicamentoResponse.de(guardado, 0);
    }

    public MedicamentoResponse obtener(String id) {
        return MedicamentoResponse.de(buscar(id), inventarioService.stockDisponible(id));
    }

    public PaginaResponse<MedicamentoResponse> buscarPorTexto(String texto, int pagina, int tamano) {
        Query query = new Query();
        if (texto != null && !texto.isBlank()) {
            String patron = Pattern.quote(texto.trim());
            query.addCriteria(new Criteria().orOperator(Criteria.where("nombre").regex(patron, "i"), Criteria.where("codigo").regex(patron, "i")));
        }
        long total = mongoTemplate.count(query, Medicamento.class);
        int paginaValida = PaginaResponse.paginaValida(pagina);
        int tamanoValido = PaginaResponse.tamanoValido(tamano);
        query.with(PageRequest.of(paginaValida, tamanoValido, Sort.by("nombre")));
        return PaginaResponse.de(mongoTemplate.find(query, Medicamento.class),
                m -> MedicamentoResponse.de(m, inventarioService.stockDisponible(m.getId())), paginaValida, tamanoValido, total);
    }

    public LoteResponse registrarLote(String medicamentoId, LoteRequest solicitud, String responsable) {
        buscar(medicamentoId);
        Lote lote = inventarioService.registrarLote(medicamentoId, solicitud.numero(), solicitud.vencimiento(), solicitud.cantidad(), responsable, "Registro de lote");
        return LoteResponse.de(lote);
    }

    public List<LoteResponse> lotesDe(String medicamentoId) {
        buscar(medicamentoId);
        return loteRepository.findByMedicamentoIdOrderByVencimiento(medicamentoId).stream().map(LoteResponse::de).toList();
    }

    public List<AlertaStockResponse> alertasStockMinimo() {
        return medicamentoRepository.findAll(Sort.by("nombre")).stream()
                .map(m -> new AlertaStockResponse(m.getId(), m.getCodigo(), m.getNombre(), inventarioService.stockDisponible(m.getId()), m.getStockMinimo()))
                .filter(a -> a.stockDisponible() <= a.stockMinimo())
                .toList();
    }

    public List<LoteProximoResponse> lotesProximosAVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        List<Lote> lotes = loteRepository.findByCantidadGreaterThanAndVencimientoBetweenOrderByVencimiento(0, hoy.minusDays(1), hoy.plusDays(Math.max(dias, 0) + 1L));
        Map<String, String> nombres = medicamentoRepository.findAll().stream().collect(Collectors.toMap(Medicamento::getId, Medicamento::getNombre, (a, b) -> a));
        return lotes.stream()
                .map(l -> new LoteProximoResponse(l.getId(), l.getNumero(), l.getMedicamentoId(), nombres.get(l.getMedicamentoId()), l.getCantidad(), l.getVencimiento()))
                .toList();
    }

    public PaginaResponse<MovimientoKardex> kardex(String medicamentoId, Instant desde, Instant hasta, int pagina, int tamano) {
        buscar(medicamentoId);
        Query query = new Query(Criteria.where("medicamentoId").is(medicamentoId));
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
        long total = mongoTemplate.count(query, MovimientoKardex.class);
        int paginaValida = PaginaResponse.paginaValida(pagina);
        int tamanoValido = PaginaResponse.tamanoValido(tamano);
        query.with(PageRequest.of(paginaValida, tamanoValido, Sort.by(Sort.Direction.ASC, "fecha")));
        return PaginaResponse.de(mongoTemplate.find(query, MovimientoKardex.class), Function.identity(), paginaValida, tamanoValido, total);
    }

    public Medicamento buscar(String id) {
        return medicamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El medicamento no existe"));
    }
}
