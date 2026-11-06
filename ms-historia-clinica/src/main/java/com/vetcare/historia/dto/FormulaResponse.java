package com.vetcare.historia.dto;

import com.vetcare.historia.model.EstadoFormula;
import com.vetcare.historia.model.Formula;
import com.vetcare.historia.model.ItemFormula;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FormulaResponse(String id, String consultaId, String mascotaId, String veterinarioId, List<ItemFormula> items,
                              LocalDate vigenteHasta, EstadoFormula estado, String motivoAnulacion, Instant fecha) {

    public static FormulaResponse de(Formula formula, EstadoFormula estadoEfectivo) {
        return new FormulaResponse(formula.getId(), formula.getConsultaId(), formula.getMascotaId(), formula.getVeterinarioId(),
                formula.getItems(), formula.getVigenteHasta(), estadoEfectivo, formula.getMotivoAnulacion(), formula.getFecha());
    }
}
