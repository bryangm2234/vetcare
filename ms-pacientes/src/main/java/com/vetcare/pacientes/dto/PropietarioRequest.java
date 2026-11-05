package com.vetcare.pacientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PropietarioRequest(
        @Schema(example = "1098765432") @NotBlank String documento,
        @Schema(example = "María Gómez") @NotBlank String nombre,
        @Schema(example = "3001234567") @NotBlank String telefono,
        @Schema(example = "maria@correo.test") @NotBlank @Email String correo,
        @Schema(example = "Calle 10 # 20-30, Bucaramanga") @NotBlank String direccion) {
}
