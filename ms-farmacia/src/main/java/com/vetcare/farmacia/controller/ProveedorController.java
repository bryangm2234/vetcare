package com.vetcare.farmacia.controller;

import com.vetcare.farmacia.dto.OrdenCompraRequest;
import com.vetcare.farmacia.dto.ProveedorRequest;
import com.vetcare.farmacia.dto.RecepcionRequest;
import com.vetcare.farmacia.model.OrdenCompra;
import com.vetcare.farmacia.model.Proveedor;
import com.vetcare.farmacia.security.Permisos;
import com.vetcare.farmacia.service.ProveedorService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Proveedores y compras", description = "HU-057 a HU-060")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @PostMapping("/api/proveedores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Registrar un proveedor")
    @ApiResponse(responseCode = "201", description = "Proveedor creado")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @ApiResponse(responseCode = "409", description = "El NIT ya existe")
    public Proveedor registrar(@Valid @RequestBody ProveedorRequest solicitud) {
        return proveedorService.registrar(solicitud);
    }

    @GetMapping("/api/proveedores")
    @PreAuthorize(Permisos.CONSULTA_PROVEEDORES)
    @Operation(summary = "Consultar los proveedores")
    @ApiResponse(responseCode = "200", description = "Proveedores")
    public List<Proveedor> listar() {
        return proveedorService.listar();
    }

    @PostMapping("/api/ordenes-compra")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.FARMACIA)
    @Operation(summary = "Generar una orden de compra; no modifica el stock")
    @ApiResponse(responseCode = "201", description = "Orden ENVIADA")
    @ApiResponse(responseCode = "400", description = "Cantidad no positiva")
    @ApiResponse(responseCode = "404", description = "El proveedor o el medicamento no existen")
    public OrdenCompra crearOrden(@Valid @RequestBody OrdenCompraRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return proveedorService.crearOrden(solicitud, jwt.getClaimAsString("correo"));
    }

    @GetMapping("/api/ordenes-compra")
    @PreAuthorize(Permisos.CONSULTA_PROVEEDORES)
    @Operation(summary = "Consultar las órdenes de compra")
    @ApiResponse(responseCode = "200", description = "Órdenes")
    public List<OrdenCompra> listarOrdenes() {
        return proveedorService.listarOrdenes();
    }

    @PostMapping("/api/ordenes-compra/{id}/recepciones")
    @PreAuthorize(Permisos.FARMACIA)
    @Operation(summary = "Registrar la mercancía recibida; crea lotes y aumenta el stock")
    @ApiResponse(responseCode = "200", description = "Orden RECIBIDA o RECIBIDA_PARCIAL")
    @ApiResponse(responseCode = "409", description = "El lote ya existe")
    @ApiResponse(responseCode = "422", description = "Orden sin confirmar, cantidad excedida o lote vencido")
    public OrdenCompra recibir(@PathVariable String id, @Valid @RequestBody RecepcionRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return proveedorService.recibir(id, solicitud, jwt.getClaimAsString("correo"));
    }

    @GetMapping("/api/proveedor/ordenes-compra")
    @PreAuthorize(Permisos.PROVEEDOR)
    @Operation(summary = "Consultar las órdenes asignadas al proveedor autenticado")
    @ApiResponse(responseCode = "200", description = "Órdenes del proveedor")
    public List<OrdenCompra> misOrdenes(@AuthenticationPrincipal Jwt jwt) {
        return proveedorService.ordenesDe(jwt.getClaimAsString("proveedorId"));
    }

    @PatchMapping("/api/proveedor/ordenes-compra/{id}/confirmacion")
    @PreAuthorize(Permisos.PROVEEDOR)
    @Operation(summary = "Confirmar una orden asignada")
    @ApiResponse(responseCode = "200", description = "Orden CONFIRMADA")
    @ApiResponse(responseCode = "403", description = "La orden es de otro proveedor")
    @ApiResponse(responseCode = "422", description = "La orden ya fue confirmada")
    public OrdenCompra confirmar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return proveedorService.confirmar(id, jwt.getClaimAsString("proveedorId"));
    }
}
