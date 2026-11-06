package com.vetcare.historia.controller;

import com.vetcare.historia.dto.ResultadosFormulasResponse;
import com.vetcare.historia.security.Permisos;
import com.vetcare.historia.service.HistoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Portal del propietario", description = "HU-050")
public class PortalClinicoController {

    private final HistoriaService historiaService;

    public PortalClinicoController(HistoriaService historiaService) {
        this.historiaService = historiaService;
    }

    @GetMapping("/api/portal/mascotas/{mascotaId}/resultados-formulas")
    @PreAuthorize(Permisos.PROPIETARIO)
    @Operation(summary = "Consultar resultados completados y fórmulas de una mascota propia")
    @ApiResponse(responseCode = "200", description = "Resultados y fórmulas")
    @ApiResponse(responseCode = "403", description = "La mascota es de otro propietario")
    public ResultadosFormulasResponse consultar(@PathVariable String mascotaId) {
        return historiaService.paraPropietario(mascotaId);
    }
}
