package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record VacunaRequest(
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d0") @NotBlank String mascotaId,
        @Schema(example = "Rabia") @NotBlank String vacuna,
        @Schema(example = "L-100") @NotBlank String lote,
        @Schema(example = "2026-11-12") @NotNull @PastOrPresent LocalDate fecha) {
}
