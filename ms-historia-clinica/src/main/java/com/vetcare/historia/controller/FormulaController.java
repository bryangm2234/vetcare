package com.vetcare.historia.controller;

import com.vetcare.historia.dto.AnulacionRequest;
import com.vetcare.historia.dto.FormulaRequest;
import com.vetcare.historia.dto.FormulaResponse;
import com.vetcare.historia.dto.RegistroDespachoRequest;
import com.vetcare.historia.dto.VigenciaResponse;
import com.vetcare.historia.security.Permisos;
import com.vetcare.historia.service.FormulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/formulas")
@Tag(name = "Fórmulas", description = "HU-045, HU-046 y HU-053 (RN-02)")
public class FormulaController {

    private final FormulaService formulaService;

    public FormulaController(FormulaService formulaService) {
        this.formulaService = formulaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Emitir una fórmula con uno o más medicamentos y vigencia")
    @ApiResponse(responseCode = "201", description = "Fórmula VIGENTE")
    @ApiResponse(responseCode = "400", description = "Cantidad no positiva, vigencia pasada o medicamento repetido")
    @ApiResponse(responseCode = "422", description = "Consulta cerrada o medicamento inexistente")
    @ApiResponse(responseCode = "503", description = "ms-farmacia no está disponible")
    public FormulaResponse emitir(@Valid @RequestBody FormulaRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return formulaService.emitir(solicitud, jwt.getSubject());
    }

    @PatchMapping("/{id}/anulacion")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Anular una fórmula indicando el motivo")
    @ApiResponse(responseCode = "200", description = "Fórmula ANULADA")
    @ApiResponse(responseCode = "400", description = "Falta el motivo")
    @ApiResponse(responseCode = "422", description = "La fórmula ya no se puede anular")
    public FormulaResponse anular(@PathVariable String id, @Valid @RequestBody AnulacionRequest solicitud) {
        return formulaService.anular(id, solicitud.motivo());
    }

    @GetMapping("/{id}/vigencia")
    @PreAuthorize(Permisos.CONSULTA_VIGENCIA)
    @Operation(summary = "Consultar si una fórmula se puede despachar")
    @ApiResponse(responseCode = "200", description = "Estado, si es despachable y cantidades pendientes")
    @ApiResponse(responseCode = "404", description = "La fórmula no existe")
    public VigenciaResponse vigencia(@PathVariable String id) {
        return formulaService.vigencia(id);
    }

    @PatchMapping("/{id}/despachos")
    @PreAuthorize(Permisos.FARMACIA)
    @Operation(summary = "Registrar las cantidades despachadas; lo invoca ms-farmacia")
    @ApiResponse(responseCode = "200", description = "Fórmula PARCIAL o DESPACHADA")
    @ApiResponse(responseCode = "409", description = "La fórmula cambió mientras se procesaba")
    @ApiResponse(responseCode = "422", description = "Fórmula no vigente o cantidad mayor a la pendiente")
    public FormulaResponse registrarDespacho(@PathVariable String id, @Valid @RequestBody RegistroDespachoRequest solicitud) {
        return formulaService.registrarDespacho(id, solicitud.items());
    }
}
