package com.vetcare.farmacia.dto;

import java.util.List;

/**
 * Respuesta de ms-historia-clinica sobre si una fórmula se puede despachar.
 */
public record FormulaVigencia(String id, String mascotaId, String estado, boolean despachable, List<ItemPendiente> items) {

    public record ItemPendiente(String medicamentoId, String medicamento, int pendiente) {
    }
}
