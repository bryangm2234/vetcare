package com.vetcare.historia.dto;

import com.vetcare.historia.model.Consulta;
import com.vetcare.historia.model.EstadoConsulta;
import com.vetcare.historia.model.SignosVitales;
import java.time.Instant;

public record ConsultaResponse(String id, String citaId, String mascotaId, String veterinarioId, EstadoConsulta estado,
                               SignosVitales signosVitales, String anamnesis, String examenFisico, String diagnostico,
                               String planTratamiento, Instant fechaApertura, Instant fechaCierre) {

    public static ConsultaResponse de(Consulta consulta, SignosVitales signos) {
        return new ConsultaResponse(consulta.getId(), consulta.getCitaId(), consulta.getMascotaId(), consulta.getVeterinarioId(),
                consulta.getEstado(), signos, consulta.getAnamnesis(), consulta.getExamenFisico(), consulta.getDiagnostico(),
                consulta.getPlanTratamiento(), consulta.getFechaApertura(), consulta.getFechaCierre());
    }
}
