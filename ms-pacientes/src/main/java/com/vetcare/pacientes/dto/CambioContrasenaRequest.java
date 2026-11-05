package com.vetcare.pacientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioContrasenaRequest(
        @Schema(example = "Cambiar.1234") @NotBlank String contrasenaActual,
        @Schema(example = "Nueva.5678") @NotBlank @Size(min = 8, max = 64) String contrasenaNueva) {
}
