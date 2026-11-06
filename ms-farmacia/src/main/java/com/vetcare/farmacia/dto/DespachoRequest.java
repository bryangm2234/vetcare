package com.vetcare.farmacia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record DespachoRequest(
        @Schema(example = "6728f1a2c3d4e5f6a7b8c9d5") @NotBlank String formulaId,
        @NotEmpty List<@Valid ItemDespachoRequest> items) {
}
