package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record EvolucionRequest(
        @Schema(example = "Paciente estable") @NotBlank String observaciones,
        @Schema(example = "38.2") @Positive Double temperatura,
        @Schema(description = "Ids de medicamentos administrados") List<String> medicamentosAdministrados) {
}
