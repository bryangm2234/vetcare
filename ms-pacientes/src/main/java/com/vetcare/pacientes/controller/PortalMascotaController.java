package com.vetcare.pacientes.controller;

import com.vetcare.pacientes.dto.MascotaResponse;
import com.vetcare.pacientes.repository.MascotaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Portal del propietario", description = "HU-071")
public class PortalMascotaController {

    private final MascotaRepository mascotaRepository;

    public PortalMascotaController(MascotaRepository mascotaRepository) {
        this.mascotaRepository = mascotaRepository;
    }

    @GetMapping("/api/portal/mascotas")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @Operation(summary = "Consultar las mascotas propias")
    @ApiResponse(responseCode = "200", description = "Mascotas del propietario autenticado")
    public List<MascotaResponse> misMascotas(@AuthenticationPrincipal Jwt jwt) {
        return mascotaRepository.findByPropietarioId(jwt.getClaimAsString("propietarioId")).stream().map(MascotaResponse::de).toList();
    }
}
