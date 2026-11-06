package com.vetcare.facturacion.controller;

import com.vetcare.facturacion.dto.AperturaCajaRequest;
import com.vetcare.facturacion.dto.CierreCajaRequest;
import com.vetcare.facturacion.model.Caja;
import com.vetcare.facturacion.security.Permisos;
import com.vetcare.facturacion.service.CajaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cajas")
@Tag(name = "Caja", description = "HU-066")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @PostMapping("/apertura")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Abrir la caja del turno")
    @ApiResponse(responseCode = "201", description = "Caja ABIERTA")
    @ApiResponse(responseCode = "409", description = "Ya tiene una caja abierta")
    public Caja abrir(@Valid @RequestBody AperturaCajaRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return cajaService.abrir(solicitud.base(), jwt.getSubject(), jwt.getClaimAsString("correo"));
    }

    @PatchMapping("/{id}/cierre")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Cerrar la caja y comparar el efectivo esperado con el contado")
    @ApiResponse(responseCode = "200", description = "Caja CERRADA con su diferencia")
    @ApiResponse(responseCode = "403", description = "La caja es de otro cajero")
    @ApiResponse(responseCode = "422", description = "La caja ya está cerrada")
    public Caja cerrar(@PathVariable String id, @Valid @RequestBody CierreCajaRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return cajaService.cerrar(id, solicitud.efectivoContado(), jwt.getSubject());
    }
}
