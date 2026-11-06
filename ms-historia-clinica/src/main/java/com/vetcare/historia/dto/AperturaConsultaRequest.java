package com.vetcare.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AperturaConsultaRequest(@Schema(example = "6728f1a2c3d4e5f6a7b8c9d2") @NotBlank String citaId) {
}
