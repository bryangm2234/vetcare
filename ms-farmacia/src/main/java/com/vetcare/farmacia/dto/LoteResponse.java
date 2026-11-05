package com.vetcare.farmacia.dto;

import com.vetcare.farmacia.model.Lote;
import java.time.LocalDate;

public record LoteResponse(String id, String medicamentoId, String numero, LocalDate vencimiento, int cantidad) {

    public static LoteResponse de(Lote lote) {
        return new LoteResponse(lote.getId(), lote.getMedicamentoId(), lote.getNumero(), lote.getVencimiento(), lote.getCantidad());
    }
}
