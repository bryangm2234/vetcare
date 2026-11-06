package com.vetcare.historia.dto;

import com.vetcare.historia.model.EstadoFormula;
import java.util.List;

public record VigenciaResponse(String id, String mascotaId, EstadoFormula estado, boolean despachable, List<ItemPendiente> items) {

    public record ItemPendiente(String medicamentoId, String medicamento, int pendiente) {
    }
}
