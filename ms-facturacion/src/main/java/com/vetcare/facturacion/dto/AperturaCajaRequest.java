package com.vetcare.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AperturaCajaRequest(@Schema(example = "100000") @NotNull @PositiveOrZero Long base) {
}
