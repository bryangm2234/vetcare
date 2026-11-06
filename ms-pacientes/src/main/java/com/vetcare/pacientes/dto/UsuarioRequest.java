package com.vetcare.pacientes.dto;

import com.vetcare.pacientes.security.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @Schema(example = "Ana Torres") @NotBlank String nombre,
        @Schema(example = "cajero@vetcare.local") @NotBlank @Email String correo,
        @Schema(example = "Cambiar.1234") @NotBlank @Size(min = 8, max = 64) String contrasena,
        @Schema(example = "CAJERO") @NotNull Rol rol,
        @Schema(description = "Obligatorio para el rol PROPIETARIO") String propietarioId,
        @Schema(description = "Obligatorio para el rol PROVEEDOR") String proveedorId) {
}
