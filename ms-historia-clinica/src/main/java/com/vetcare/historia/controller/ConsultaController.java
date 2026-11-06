package com.vetcare.historia.controller;

import com.vetcare.historia.dto.AperturaConsultaRequest;
import com.vetcare.historia.dto.ConsultaResponse;
import com.vetcare.historia.dto.ItemPendienteResponse;
import com.vetcare.historia.dto.RegistroClinicoRequest;
import com.vetcare.historia.security.Permisos;
import com.vetcare.historia.service.ConsultaService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultas")
@Tag(name = "Consultas", description = "HU-031, HU-033 y HU-035")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Abrir una consulta desde una cita en espera")
    @ApiResponse(responseCode = "201", description = "Consulta abierta")
    @ApiResponse(responseCode = "404", description = "La cita no existe")
    @ApiResponse(responseCode = "409", description = "La cita ya tiene consulta")
    @ApiResponse(responseCode = "422", description = "La cita no está en espera")
    @ApiResponse(responseCode = "503", description = "ms-agenda no está disponible")
    public ConsultaResponse abrir(@Valid @RequestBody AperturaConsultaRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return consultaService.abrir(solicitud.citaId(), jwt.getSubject());
    }

    @GetMapping("/items-pendientes")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Consultas cerradas pendientes de facturar")
    @ApiResponse(responseCode = "200", description = "Ítems pendientes")
    public List<ItemPendienteResponse> itemsPendientes() {
        return consultaService.itemsPendientes();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Consultar una consulta con sus signos vitales")
    @ApiResponse(responseCode = "200", description = "Consulta encontrada")
    @ApiResponse(responseCode = "404", description = "La consulta no existe")
    public ConsultaResponse obtener(@PathVariable String id) {
        return consultaService.obtener(id);
    }

    @PutMapping("/{id}/registro-clinico")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Registrar anamnesis, examen físico, diagnóstico y plan")
    @ApiResponse(responseCode = "200", description = "Registro guardado")
    @ApiResponse(responseCode = "403", description = "El usuario no es veterinario")
    @ApiResponse(responseCode = "422", description = "La consulta está cerrada")
    public ConsultaResponse registrarClinico(@PathVariable String id, @Valid @RequestBody RegistroClinicoRequest solicitud) {
        return consultaService.registrarClinico(id, solicitud);
    }

    @PatchMapping("/{id}/cierre")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Cerrar la consulta; queda inmodificable y pendiente de facturar")
    @ApiResponse(responseCode = "200", description = "Consulta cerrada y cita atendida")
    @ApiResponse(responseCode = "422", description = "Sin diagnóstico o ya cerrada")
    @ApiResponse(responseCode = "503", description = "ms-agenda no está disponible; la consulta sigue abierta")
    public ConsultaResponse cerrar(@PathVariable String id) {
        return consultaService.cerrar(id);
    }
}
