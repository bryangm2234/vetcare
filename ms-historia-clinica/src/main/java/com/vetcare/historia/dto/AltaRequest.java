package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AltaRequest(@Schema(example = "Control en 8 días") @NotBlank String indicaciones) {
}
