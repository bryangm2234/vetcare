package com.vetcare.farmacia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ItemRecepcionRequest(
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d4") @NotBlank String medicamentoId,
        @Schema(example = "L-200") @NotBlank String lote,
        @Schema(example = "2027-06-30") @NotNull LocalDate vencimiento,
        @Schema(example = "60") @NotNull @Positive Integer cantidad) {
}
