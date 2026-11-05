package com.vetcare.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AnulacionRequest(@Schema(example = "Error en los conceptos") @NotBlank String motivo) {
}
