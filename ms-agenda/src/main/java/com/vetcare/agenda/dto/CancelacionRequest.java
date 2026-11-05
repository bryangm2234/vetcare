package com.vetcare.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CancelacionRequest(@Schema(example = "El propietario no puede asistir") @NotBlank String motivo) {
}
