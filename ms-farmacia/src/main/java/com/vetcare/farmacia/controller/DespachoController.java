package com.vetcare.farmacia.controller;

import com.vetcare.farmacia.dto.DespachoRequest;
import com.vetcare.farmacia.dto.ItemPendienteResponse;
import com.vetcare.farmacia.model.Despacho;
import com.vetcare.farmacia.security.Permisos;
import com.vetcare.farmacia.service.DespachoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/despachos")
@Tag(name = "Despachos", description = "HU-053 (RN-02 y RN-04)")
public class DespachoController {

    private final DespachoService despachoService;

    public DespachoController(DespachoService despachoService) {
        this.despachoService = despachoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.FARMACIA)
    @Operation(summary = "Despachar medicamentos de una fórmula vigente, descontando primero el lote que vence antes")
    @ApiResponse(responseCode = "201", description = "Despacho registrado")
    @ApiResponse(responseCode = "404", description = "La fórmula no existe")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente; no se modifica el inventario")
    @ApiResponse(responseCode = "422", description = "Fórmula no vigente o cantidad mayor a la formulada")
    @ApiResponse(responseCode = "503", description = "ms-historia-clinica no está disponible; no se modifica el inventario")
    public Despacho despachar(@Valid @RequestBody DespachoRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return despachoService.despachar(solicitud, jwt.getClaimAsString("correo"));
    }

    @GetMapping("/items-pendientes")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Despachos pendientes de facturar")
    @ApiResponse(responseCode = "200", description = "Ítems pendientes con su valor")
    public List<ItemPendienteResponse> itemsPendientes() {
        return despachoService.itemsPendientes();
    }
}
