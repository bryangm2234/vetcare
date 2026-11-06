package com.vetcare.farmacia.dto;

import java.time.LocalDate;

public record LoteProximoResponse(String loteId, String numero, String medicamentoId, String medicamento, int cantidad, LocalDate vencimiento) {
}
