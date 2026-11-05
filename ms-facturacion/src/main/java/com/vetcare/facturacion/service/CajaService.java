package com.vetcare.facturacion.service;

import com.vetcare.facturacion.exception.ConflictoException;
import com.vetcare.facturacion.exception.RecursoNoEncontradoException;
import com.vetcare.facturacion.exception.ReglaNegocioException;
import com.vetcare.facturacion.model.Caja;
import com.vetcare.facturacion.model.EstadoCaja;
import com.vetcare.facturacion.model.Factura;
import com.vetcare.facturacion.model.MedioPago;
import com.vetcare.facturacion.repository.CajaRepository;
import com.vetcare.facturacion.repository.FacturaRepository;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CajaService {

    private final CajaRepository cajaRepository;
    private final FacturaRepository facturaRepository;
    private final AuditoriaService auditoriaService;

    public CajaService(CajaRepository cajaRepository, FacturaRepository facturaRepository, AuditoriaService auditoriaService) {
        this.cajaRepository = cajaRepository;
        this.facturaRepository = facturaRepository;
        this.auditoriaService = auditoriaService;
    }

    public Caja abrir(long base, String cajeroId, String cajero) {
        Caja caja = new Caja();
        caja.setCajeroId(cajeroId);
        caja.setCajero(cajero);
        caja.setCajeroConCajaAbierta(cajeroId);
        caja.setBase(base);
        try {
            Caja guardada = cajaRepository.save(caja);
            auditoriaService.registrar("ABRIR_CAJA", "cajas/" + guardada.getId(), String.valueOf(base));
            return guardada;
        } catch (DuplicateKeyException ex) {
            throw new ConflictoException("Ya tiene una caja abierta");
        }
    }

    public Caja cerrar(String id, long efectivoContado, String cajeroId) {
        Caja caja = cajaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La caja no existe"));
        if (!caja.getCajeroId().equals(cajeroId)) {
            throw new AccessDeniedException("La caja es de otro cajero");
        }
        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new ReglaNegocioException("La caja ya está cerrada");
        }
        long esperado = caja.getBase() + efectivoRecibido(id);
        caja.setEstado(EstadoCaja.CERRADA);
        caja.setCajeroConCajaAbierta(null);
        caja.setEfectivoEsperado(esperado);
        caja.setEfectivoContado(efectivoContado);
        caja.setDiferencia(efectivoContado - esperado);
        caja.setCierre(Instant.now());
        Caja guardada = cajaRepository.save(caja);
        auditoriaService.registrar("CERRAR_CAJA", "cajas/" + id, "Diferencia " + guardada.getDiferencia());
        return guardada;
    }

    public Caja abiertaDe(String cajeroId) {
        return cajaRepository.findByCajeroConCajaAbierta(cajeroId)
                .orElseThrow(() -> new ReglaNegocioException("Debe abrir la caja"));
    }

    private long efectivoRecibido(String cajaId) {
        long total = 0;
        for (Factura factura : facturaRepository.findByPagosCajaId(cajaId)) {
            total += factura.getPagos().stream()
                    .filter(p -> cajaId.equals(p.getCajaId()) && p.getMedio() == MedioPago.EFECTIVO && !p.isRevertido())
                    .mapToLong(p -> p.getValor())
                    .sum();
        }
        return total;
    }
}
