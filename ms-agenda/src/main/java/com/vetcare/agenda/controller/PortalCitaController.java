package com.vetcare.agenda.controller;

import com.vetcare.agenda.dto.CitaRequest;
import com.vetcare.agenda.dto.CitaResponse;
import com.vetcare.agenda.security.Permisos;
import com.vetcare.agenda.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/citas")
@Tag(name = "Portal del propietario", description = "HU-024 y HU-071")
public class PortalCitaController {

    private final CitaService citaService;

    public PortalCitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @GetMapping
    @PreAuthorize(Permisos.PROPIETARIO)
    @Operation(summary = "Consultar las próximas citas de las mascotas propias")
    @ApiResponse(responseCode = "200", description = "Citas programadas o en espera")
    public List<CitaResponse> proximas() {
        return citaService.proximasDelPropietario().stream().map(CitaResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.PROPIETARIO)
    @Operation(summary = "Solicitar una cita para una mascota propia")
    @ApiResponse(responseCode = "201", description = "Cita programada")
    @ApiResponse(responseCode = "403", description = "La mascota es de otro propietario")
    @ApiResponse(responseCode = "409", description = "No hay cupos disponibles o la franja está ocupada")
    public CitaResponse solicitar(@Valid @RequestBody CitaRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return CitaResponse.de(citaService.agendarDesdePortal(solicitud, jwt.getClaimAsString("correo")));
    }
}
