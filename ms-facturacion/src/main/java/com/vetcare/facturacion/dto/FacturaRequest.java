package com.vetcare.facturacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record FacturaRequest(
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d0") @NotBlank String mascotaId,
        @Schema(example = "false", description = "true para generar una factura a crédito") Boolean credito) {
}
