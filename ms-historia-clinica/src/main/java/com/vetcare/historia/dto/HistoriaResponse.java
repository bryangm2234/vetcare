package com.vetcare.historia.dto;

import com.vetcare.historia.model.Hospitalizacion;
import java.util.List;

public record HistoriaResponse(String mascotaId, List<String> alergias, List<String> condicionesCronicas, List<String> avisos,
                               List<ConsultaResponse> consultas, List<VacunaResponse> vacunas, List<OrdenLaboratorioResponse> laboratorio,
                               List<FormulaResponse> formulas, List<Hospitalizacion> hospitalizaciones, List<AdjuntoResponse> adjuntos) {
}
