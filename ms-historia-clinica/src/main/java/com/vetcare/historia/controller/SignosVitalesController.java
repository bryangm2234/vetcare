package com.vetcare.historia.controller;

import com.vetcare.historia.dto.SignosVitalesRequest;
import com.vetcare.historia.model.SignosVitales;
import com.vetcare.historia.security.Permisos;
import com.vetcare.historia.service.ConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Signos vitales", description = "HU-032")
public class SignosVitalesController {

    private final ConsultaService consultaService;

    public SignosVitalesController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @PutMapping("/api/citas/{citaId}/signos-vitales")
    @PreAuthorize(Permisos.AUXILIAR)
    @Operation(summary = "Registrar los signos vitales antes de la consulta")
    @ApiResponse(responseCode = "200", description = "Signos registrados")
    @ApiResponse(responseCode = "400", description = "Valores no positivos")
    @ApiResponse(responseCode = "422", description = "La cita no está en espera o la consulta está cerrada")
    public SignosVitales registrar(@PathVariable String citaId, @Valid @RequestBody SignosVitalesRequest solicitud,
                                   @AuthenticationPrincipal Jwt jwt) {
        return consultaService.registrarSignos(citaId, solicitud, jwt.getClaimAsString("correo"));
    }
}
