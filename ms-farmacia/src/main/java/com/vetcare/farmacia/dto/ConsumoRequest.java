package com.vetcare.farmacia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConsumoRequest(@Schema(example = "1") @NotNull @Positive Integer cantidad) {
}
