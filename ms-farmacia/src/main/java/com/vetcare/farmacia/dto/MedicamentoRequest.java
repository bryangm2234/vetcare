package com.vetcare.farmacia.dto;

import com.vetcare.farmacia.model.TipoProducto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MedicamentoRequest(
        @Schema(example = "MED-001") @NotBlank String codigo,
        @Schema(example = "Metronidazol 250 mg") @NotBlank String nombre,
        @Schema(example = "MEDICAMENTO") @NotNull TipoProducto tipo,
        @Schema(example = "Caja x 20 tabletas") @NotBlank String presentacion,
        @Schema(example = "1500", description = "Precio unitario en pesos") @NotNull @PositiveOrZero Long precio,
        @Schema(example = "20") @NotNull @PositiveOrZero Integer stockMinimo) {
}
