package com.vetcare.facturacion.service;

import com.vetcare.facturacion.dto.TarifaRequest;
import com.vetcare.facturacion.exception.ConflictoException;
import com.vetcare.facturacion.exception.RecursoNoEncontradoException;
import com.vetcare.facturacion.exception.ReglaNegocioException;
import com.vetcare.facturacion.model.Tarifa;
import com.vetcare.facturacion.model.TipoServicio;
import com.vetcare.facturacion.repository.TarifaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TarifaService {

    private final TarifaRepository tarifaRepository;
    private final AuditoriaService auditoriaService;

    public TarifaService(TarifaRepository tarifaRepository, AuditoriaService auditoriaService) {
        this.tarifaRepository = tarifaRepository;
        this.auditoriaService = auditoriaService;
    }

    public Tarifa crear(TarifaRequest solicitud) {
        validarServicio(solicitud.servicio());
        if (tarifaRepository.existsByServicio(solicitud.servicio())) {
            throw new ConflictoException("Ya existe una tarifa para ese servicio");
        }
        Tarifa tarifa = new Tarifa();
        tarifa.setServicio(solicitud.servicio());
        return guardar(tarifa, solicitud);
    }

    public Tarifa actualizar(String id, TarifaRequest solicitud) {
        Tarifa tarifa = tarifaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La tarifa no existe"));
        if (tarifa.getServicio() != solicitud.servicio()) {
            throw new ReglaNegocioException("No se puede cambiar el servicio de una tarifa");
        }
        return guardar(tarifa, solicitud);
    }

    public List<Tarifa> listar() {
        return tarifaRepository.findAll();
    }

    private Tarifa guardar(Tarifa tarifa, TarifaRequest solicitud) {
        tarifa.setNombre(solicitud.nombre().trim());
        tarifa.setPrecio(solicitud.precio());
        tarifa.setActualizadaEn(Instant.now());
        Tarifa guardada = tarifaRepository.save(tarifa);
        auditoriaService.registrar("CONFIGURAR_TARIFA", "tarifas/" + guardada.getId(), String.valueOf(solicitud.precio()));
        return guardada;
    }

    private void validarServicio(TipoServicio servicio) {
        if (servicio == TipoServicio.MEDICAMENTOS) {
            throw new ReglaNegocioException("Los medicamentos se cobran al precio de farmacia");
        }
    }
}
