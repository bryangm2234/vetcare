package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SignosVitalesRequest(
        @Schema(example = "12.5") @NotNull @Positive Double pesoKg,
        @Schema(example = "38.6") @NotNull @Positive Double temperatura,
        @Schema(example = "90") @NotNull @Positive Integer frecuenciaCardiaca,
        @Schema(example = "24") @NotNull @Positive Integer frecuenciaRespiratoria) {
}
