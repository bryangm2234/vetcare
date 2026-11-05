package com.vetcare.farmacia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record LoteRequest(
        @Schema(example = "L-100") @NotBlank String numero,
        @Schema(example = "2027-06-30") @NotNull LocalDate vencimiento,
        @Schema(example = "50") @NotNull @Positive Integer cantidad) {
}
