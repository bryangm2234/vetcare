package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OrdenLaboratorioRequest(
        @Schema(example = "Hemograma") @NotBlank String examen,
        @Schema(example = "En ayunas") String indicaciones) {
}
