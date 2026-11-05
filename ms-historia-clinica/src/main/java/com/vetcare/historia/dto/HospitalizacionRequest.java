package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record HospitalizacionRequest(
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d0") @NotBlank String mascotaId,
        @Schema(example = "Deshidratación severa") @NotBlank String motivo,
        @Schema(example = "C-01") @NotBlank String cubiculo) {
}
