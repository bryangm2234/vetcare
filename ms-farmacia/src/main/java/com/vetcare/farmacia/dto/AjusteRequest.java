package com.vetcare.farmacia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AjusteRequest(
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d3") @NotBlank String loteId,
        @Schema(example = "-2", description = "Positivo para sumar y negativo para restar") @NotNull Integer cantidad,
        @Schema(example = "Rotura") @NotBlank String motivo) {
}
