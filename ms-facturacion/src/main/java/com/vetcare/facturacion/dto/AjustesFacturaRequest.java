package com.vetcare.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;

public record AjustesFacturaRequest(
        @Schema(example = "10", description = "Porcentaje de descuento. Se usa si no se indica un valor") @PositiveOrZero @Max(100) Integer descuentoPorcentaje,
        @Schema(example = "10000", description = "Descuento en pesos") @PositiveOrZero Long descuentoValor) {
}
