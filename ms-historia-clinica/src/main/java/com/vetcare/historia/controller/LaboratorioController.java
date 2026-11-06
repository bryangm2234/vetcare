package com.vetcare.historia.controller;

import com.vetcare.historia.dto.ItemPendienteResponse;
import com.vetcare.historia.dto.OrdenLaboratorioRequest;
import com.vetcare.historia.dto.OrdenLaboratorioResponse;
import com.vetcare.historia.dto.PaginaResponse;
import com.vetcare.historia.dto.ResultadoLaboratorioRequest;
import com.vetcare.historia.model.EstadoOrdenLaboratorio;
import com.vetcare.historia.security.Permisos;
import com.vetcare.historia.service.LaboratorioService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Laboratorio", description = "HU-041 a HU-044")
public class LaboratorioController {

    private final LaboratorioService laboratorioService;

    public LaboratorioController(LaboratorioService laboratorioService) {
        this.laboratorioService = laboratorioService;
    }

    @PostMapping("/api/consultas/{id}/ordenes-laboratorio")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Crear una orden de laboratorio desde una consulta abierta")
    @ApiResponse(responseCode = "201", description = "Orden PENDIENTE")
    @ApiResponse(responseCode = "400", description = "Falta el examen")
    @ApiResponse(responseCode = "404", description = "La consulta no existe")
    @ApiResponse(responseCode = "422", description = "La consulta está cerrada")
    public OrdenLaboratorioResponse crear(@PathVariable String id, @Valid @RequestBody OrdenLaboratorioRequest solicitud) {
        return laboratorioService.crear(id, solicitud);
    }

    @GetMapping("/api/ordenes-laboratorio")
    @PreAuthorize(Permisos.LABORATORIO)
    @Operation(summary = "Consultar órdenes por estado, primero la más antigua")
    @ApiResponse(responseCode = "200", description = "Órdenes paginadas")
    public PaginaResponse<OrdenLaboratorioResponse> consultar(@RequestParam(defaultValue = "PENDIENTE") EstadoOrdenLaboratorio estado,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return laboratorioService.pendientes(estado, page, size);
    }

    @PutMapping("/api/ordenes-laboratorio/{id}/resultado")
    @PreAuthorize(Permisos.LABORATORIO)
    @Operation(summary = "Registrar el resultado de un examen")
    @ApiResponse(responseCode = "200", description = "Orden COMPLETADA y pendiente de facturar")
    @ApiResponse(responseCode = "400", description = "El resultado está vacío")
    @ApiResponse(responseCode = "404", description = "La orden no existe")
    @ApiResponse(responseCode = "409", description = "La orden ya tiene resultado")
    public OrdenLaboratorioResponse registrarResultado(@PathVariable String id, @Valid @RequestBody ResultadoLaboratorioRequest solicitud,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return laboratorioService.registrarResultado(id, solicitud.resultado(), jwt.getClaimAsString("correo"));
    }

    @GetMapping("/api/ordenes-laboratorio/items-pendientes")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Exámenes completados pendientes de facturar")
    @ApiResponse(responseCode = "200", description = "Ítems pendientes")
    public List<ItemPendienteResponse> itemsPendientes() {
        return laboratorioService.itemsPendientes();
    }

    @GetMapping("/api/historias/{mascotaId}/resultados-laboratorio")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Ver los exámenes de una mascota")
    @ApiResponse(responseCode = "200", description = "Exámenes con su estado y resultado")
    @ApiResponse(responseCode = "403", description = "El usuario no es veterinario")
    public List<OrdenLaboratorioResponse> resultados(@PathVariable String mascotaId) {
        return laboratorioService.deMascota(mascotaId);
    }
}
