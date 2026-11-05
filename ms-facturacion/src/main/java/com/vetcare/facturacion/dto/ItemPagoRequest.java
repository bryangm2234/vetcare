package com.vetcare.facturacion.dto;

import com.vetcare.facturacion.model.MedioPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemPagoRequest(
        @Schema(example = "EFECTIVO") @NotNull MedioPago medio,
        @Schema(example = "60000") @NotNull @Positive Long valor) {
}
