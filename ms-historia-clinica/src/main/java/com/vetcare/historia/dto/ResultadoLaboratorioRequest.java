package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ResultadoLaboratorioRequest(@Schema(example = "Leucocitos elevados") @NotBlank String resultado) {
}
