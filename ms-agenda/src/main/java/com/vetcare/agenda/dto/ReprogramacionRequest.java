package com.vetcare.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReprogramacionRequest(
        @Schema(example = "2026-11-21") @NotNull LocalDate fecha,
        @Schema(type = "string", example = "10:00") @NotNull LocalTime hora) {
}
