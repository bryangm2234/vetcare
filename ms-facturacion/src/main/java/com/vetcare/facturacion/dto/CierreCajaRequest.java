package com.vetcare.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CierreCajaRequest(@Schema(example = "480000") @NotNull @PositiveOrZero Long efectivoContado) {
}
