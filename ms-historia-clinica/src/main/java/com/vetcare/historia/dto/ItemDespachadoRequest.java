package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemDespachadoRequest(
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d4") @NotBlank String medicamentoId,
        @Schema(example = "14") @NotNull @Positive Integer cantidad) {
}
