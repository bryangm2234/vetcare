package com.vetcare.pacientes.dto;

import com.vetcare.pacientes.model.SexoMascota;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record MascotaRequest(
        @Schema(example = "Max") @NotBlank String nombre,
        @Schema(example = "Canino") @NotBlank String especie,
        @Schema(example = "Criollo") @NotBlank String raza,
        @Schema(example = "MACHO") @NotNull SexoMascota sexo,
        @Schema(example = "2022-03-15") @NotNull @PastOrPresent LocalDate fechaNacimiento,
        @Schema(example = "12.5") @NotNull @Positive Double pesoKg,
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d0") @NotBlank String propietarioId) {
}
