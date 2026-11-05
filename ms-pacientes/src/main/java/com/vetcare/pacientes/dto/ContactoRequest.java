package com.vetcare.pacientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactoRequest(
        @Schema(example = "3009876543") @NotBlank String telefono,
        @Schema(example = "maria.nuevo@correo.test") @NotBlank @Email String correo,
        @Schema(example = "Carrera 5 # 12-40, Bucaramanga") @NotBlank String direccion) {
}
