package com.vetcare.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record HorarioRequest(
        @Schema(example = "Dra. Laura Pérez") @NotBlank String nombre,
        @Schema(example = "[\"MONDAY\",\"TUESDAY\"]") @NotEmpty List<DayOfWeek> dias,
        @Schema(type = "string", example = "08:00") @NotNull LocalTime horaInicio,
        @Schema(type = "string", example = "12:00") @NotNull LocalTime horaFin,
        @Schema(example = "30", description = "30 minutos por defecto") @Min(5) @Max(240) Integer duracionMinutos,
        @Schema(example = "8") @NotNull @Min(1) Integer cuposDiarios) {

    public int duracionOPorDefecto() {
        return duracionMinutos != null ? duracionMinutos : 30;
    }
}
