package com.vetcare.historia.dto;

import java.util.List;

public record ResultadosFormulasResponse(List<OrdenLaboratorioResponse> resultados, List<FormulaResponse> formulas) {
}
