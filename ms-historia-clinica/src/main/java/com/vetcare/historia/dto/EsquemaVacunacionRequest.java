package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EsquemaVacunacionRequest(
        @Schema(example = "Canino") @NotBlank String especie,
        @Schema(example = "Rabia") @NotBlank String vacuna,
        @Schema(example = "2") @NotNull @Min(1) Integer dosis,
        @Schema(example = "365") @NotNull @Min(1) Integer intervaloDias) {
}
