package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RegistroClinicoRequest(
        @Schema(example = "Vómito desde hace dos días") @NotBlank String anamnesis,
        @Schema(example = "Abdomen sensible a la palpación") @NotBlank String examenFisico,
        @Schema(example = "Gastroenteritis") @NotBlank String diagnostico,
        @Schema(example = "Dieta blanda e hidratación") @NotBlank String planTratamiento) {
}
