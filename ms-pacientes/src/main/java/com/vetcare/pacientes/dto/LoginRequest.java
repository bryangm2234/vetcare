package com.vetcare.pacientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "cajero@vetcare.local") @NotBlank @Email String correo,
        @Schema(example = "Cambiar.1234") @NotBlank String contrasena) {
}
