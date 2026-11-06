package com.vetcare.facturacion.controller;

import com.vetcare.facturacion.dto.TarifaRequest;
import com.vetcare.facturacion.model.Tarifa;
import com.vetcare.facturacion.security.Permisos;
import com.vetcare.facturacion.service.TarifaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tarifas")
@Tag(name = "Tarifas", description = "HU-062")
public class TarifaController {

    private final TarifaService tarifaService;

    public TarifaController(TarifaService tarifaService) {
        this.tarifaService = tarifaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Crear la tarifa de un servicio")
    @ApiResponse(responseCode = "201", description = "Tarifa creada")
    @ApiResponse(responseCode = "400", description = "Precio negativo")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @ApiResponse(responseCode = "409", description = "Ya existe una tarifa para ese servicio")
    public Tarifa crear(@Valid @RequestBody TarifaRequest solicitud) {
        return tarifaService.crear(solicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Actualizar una tarifa; solo afecta las facturas nuevas")
    @ApiResponse(responseCode = "200", description = "Tarifa actualizada")
    @ApiResponse(responseCode = "404", description = "La tarifa no existe")
    public Tarifa actualizar(@PathVariable String id, @Valid @RequestBody TarifaRequest solicitud) {
        return tarifaService.actualizar(id, solicitud);
    }

    @GetMapping
    @PreAuthorize(Permisos.CONSULTA_TARIFAS)
    @Operation(summary = "Consultar las tarifas")
    @ApiResponse(responseCode = "200", description = "Tarifas")
    public List<Tarifa> listar() {
        return tarifaService.listar();
    }
}
