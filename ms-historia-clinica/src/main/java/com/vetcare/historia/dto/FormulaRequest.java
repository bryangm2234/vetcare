package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record FormulaRequest(
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d7") @NotBlank String consultaId,
        @Schema(example = "2026-12-12") @NotNull @FutureOrPresent LocalDate vigenteHasta,
        @NotEmpty List<@Valid ItemFormulaRequest> items) {
}
