package com.vetcare.historia.dto;

import java.util.List;

public record HistoriaResponse(String mascotaId, List<String> alergias, List<String> condicionesCronicas, List<String> avisos,
                               List<ConsultaResponse> consultas, List<VacunaResponse> vacunas, List<AdjuntoResponse> adjuntos) {
}
