package com.vetcare.pacientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AntecedentesRequest(
        @Schema(example = "[\"Penicilina\"]") @NotNull List<String> alergias,
        @Schema(example = "[\"Diabetes\"]") @NotNull List<String> condicionesCronicas) {
}
