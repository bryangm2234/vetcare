package com.vetcare.pacientes.dto;

import com.vetcare.pacientes.model.EstadoMascota;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record EstadoMascotaRequest(
        @Schema(example = "FALLECIDA") @NotNull EstadoMascota estado,
        @Schema(example = "2026-11-01", description = "Obligatoria para el estado FALLECIDA") @PastOrPresent LocalDate fechaFallecimiento) {
}
