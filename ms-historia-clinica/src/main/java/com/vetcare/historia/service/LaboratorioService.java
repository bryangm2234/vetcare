package com.vetcare.historia.service;

import com.vetcare.historia.dto.ItemPendienteResponse;
import com.vetcare.historia.dto.OrdenLaboratorioRequest;
import com.vetcare.historia.dto.OrdenLaboratorioResponse;
import com.vetcare.historia.dto.PaginaResponse;
import com.vetcare.historia.exception.ConflictoException;
import com.vetcare.historia.exception.RecursoNoEncontradoException;
import com.vetcare.historia.model.Consulta;
import com.vetcare.historia.model.EstadoOrdenLaboratorio;
import com.vetcare.historia.model.OrdenLaboratorio;
import com.vetcare.historia.repository.OrdenLaboratorioRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class LaboratorioService {

    private final OrdenLaboratorioRepository ordenRepository;
    private final ConsultaService consultaService;
    private final AuditoriaService auditoriaService;

    public LaboratorioService(OrdenLaboratorioRepository ordenRepository, ConsultaService consultaService, AuditoriaService auditoriaService) {
        this.ordenRepository = ordenRepository;
        this.consultaService = consultaService;
        this.auditoriaService = auditoriaService;
    }

    public OrdenLaboratorioResponse crear(String consultaId, OrdenLaboratorioRequest solicitud) {
        Consulta consulta = consultaService.abierta(consultaId);
        OrdenLaboratorio orden = new OrdenLaboratorio();
        orden.setConsultaId(consultaId);
        orden.setMascotaId(consulta.getMascotaId());
        orden.setExamen(solicitud.examen().trim());
        orden.setIndicaciones(solicitud.indicaciones());
        OrdenLaboratorio guardada = ordenRepository.save(orden);
        auditoriaService.registrar("ORDENAR_LABORATORIO", "ordenes-laboratorio/" + guardada.getId(), "EXITOSO");
        return OrdenLaboratorioResponse.de(guardada);
    }

    public PaginaResponse<OrdenLaboratorioResponse> pendientes(EstadoOrdenLaboratorio estado, int pagina, int tamano) {
        int paginaValida = PaginaResponse.paginaValida(pagina);
        int tamanoValido = PaginaResponse.tamanoValido(tamano);
        Page<OrdenLaboratorio> ordenes = ordenRepository.findByEstado(estado, PageRequest.of(paginaValida, tamanoValido, Sort.by(Sort.Direction.ASC, "fechaOrden")));
        return PaginaResponse.de(ordenes.getContent(), OrdenLaboratorioResponse::de, paginaValida, tamanoValido, ordenes.getTotalElements());
    }

    public OrdenLaboratorioResponse registrarResultado(String id, String resultado, String responsable) {
        OrdenLaboratorio orden = ordenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La orden de laboratorio no existe"));
        if (orden.getEstado() == EstadoOrdenLaboratorio.COMPLETADA) {
            throw new ConflictoException("La orden ya tiene resultado");
        }
        orden.setEstado(EstadoOrdenLaboratorio.COMPLETADA);
        orden.setResultado(resultado.trim());
        orden.setResponsable(responsable);
        orden.setFechaResultado(Instant.now());
        OrdenLaboratorio guardada = ordenRepository.save(orden);
        auditoriaService.registrar("REGISTRAR_RESULTADO", "ordenes-laboratorio/" + id, "EXITOSO");
        return OrdenLaboratorioResponse.de(guardada);
    }

    public List<OrdenLaboratorioResponse> deMascota(String mascotaId) {
        return ordenRepository.findByMascotaIdOrderByFechaOrdenDesc(mascotaId).stream().map(OrdenLaboratorioResponse::de).toList();
    }

    public List<ItemPendienteResponse> itemsPendientes() {
        return ordenRepository.findByEstadoAndFacturadoFalse(EstadoOrdenLaboratorio.COMPLETADA).stream()
                .map(o -> new ItemPendienteResponse(o.getId(), "LABORATORIO", "Examen: " + o.getExamen(), o.getMascotaId(),
                        o.getFechaResultado().atZone(ZoneId.systemDefault()).toLocalDate()))
                .toList();
    }
}
