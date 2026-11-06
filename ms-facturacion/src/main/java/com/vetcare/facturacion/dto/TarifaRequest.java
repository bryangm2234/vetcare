package com.vetcare.facturacion.dto;

import com.vetcare.facturacion.model.TipoServicio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record TarifaRequest(
        @Schema(example = "CONSULTA") @NotNull TipoServicio servicio,
        @Schema(example = "Consulta general") @NotBlank String nombre,
        @Schema(example = "60000") @NotNull @PositiveOrZero Long precio) {
}
