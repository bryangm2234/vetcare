package com.vetcare.pacientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TransferenciaRequest(
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d1") @NotBlank String nuevoPropietarioId) {
}
