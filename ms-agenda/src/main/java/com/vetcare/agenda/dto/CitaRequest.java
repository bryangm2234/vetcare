package com.vetcare.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CitaRequest(
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d0") @NotBlank String mascotaId,
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d1") @NotBlank String veterinarioId,
        @Schema(example = "2026-11-20") @NotNull LocalDate fecha,
        @Schema(type = "string", example = "09:00") @NotNull LocalTime hora,
        @Schema(example = "Control de vacunas") @NotBlank String motivo) {
}
