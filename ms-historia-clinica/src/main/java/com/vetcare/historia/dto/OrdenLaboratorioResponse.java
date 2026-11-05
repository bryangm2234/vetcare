package com.vetcare.historia.dto;

import com.vetcare.historia.model.EstadoOrdenLaboratorio;
import com.vetcare.historia.model.OrdenLaboratorio;
import java.time.Instant;

public record OrdenLaboratorioResponse(String id, String consultaId, String mascotaId, String examen, String indicaciones,
                                       EstadoOrdenLaboratorio estado, String resultado, String responsable, Instant fechaOrden,
                                       Instant fechaResultado) {

    public static OrdenLaboratorioResponse de(OrdenLaboratorio orden) {
        return new OrdenLaboratorioResponse(orden.getId(), orden.getConsultaId(), orden.getMascotaId(), orden.getExamen(),
                orden.getIndicaciones(), orden.getEstado(), orden.getResultado(), orden.getResponsable(), orden.getFechaOrden(),
                orden.getFechaResultado());
    }
}
