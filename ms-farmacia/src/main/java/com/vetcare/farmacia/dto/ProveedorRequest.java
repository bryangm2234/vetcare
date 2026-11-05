package com.vetcare.farmacia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProveedorRequest(
        @Schema(example = "900123456-7") @NotBlank String nit,
        @Schema(example = "Distribuciones Vet SAS") @NotBlank String nombre,
        @Schema(example = "6071234567") @NotBlank String telefono,
        @Schema(example = "ventas@proveedor.test") @NotBlank @Email String correo) {
}
