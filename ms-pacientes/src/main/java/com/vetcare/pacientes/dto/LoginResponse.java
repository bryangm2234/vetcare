package com.vetcare.pacientes.dto;

import com.vetcare.pacientes.security.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record LoginResponse(
        @Schema(example = "eyJhbGciOiJSUzI1NiJ9...") String token,
        @Schema(example = "Bearer") String tipo,
        @Schema(example = "2026-11-05T15:30:00Z") Instant expiraEn,
        @Schema(example = "CAJERO") Rol rol) {
}
