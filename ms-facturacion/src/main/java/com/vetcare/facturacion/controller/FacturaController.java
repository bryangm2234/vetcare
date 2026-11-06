package com.vetcare.facturacion.controller;

import com.vetcare.facturacion.dto.AjustesFacturaRequest;
import com.vetcare.facturacion.dto.AnulacionRequest;
import com.vetcare.facturacion.dto.FacturaRequest;
import com.vetcare.facturacion.dto.ItemPagoRequest;
import com.vetcare.facturacion.dto.PaginaResponse;
import com.vetcare.facturacion.dto.PagoRequest;
import com.vetcare.facturacion.model.EstadoFactura;
import com.vetcare.facturacion.model.Factura;
import com.vetcare.facturacion.security.Permisos;
import com.vetcare.facturacion.service.EnvioFacturaService;
import com.vetcare.facturacion.service.FacturaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facturas")
@Tag(name = "Facturas", description = "HU-061, HU-063, HU-064, HU-065, HU-068 y HU-070")
public class FacturaController {

    private final FacturaService facturaService;
    private final EnvioFacturaService envioService;

    public FacturaController(FacturaService facturaService, EnvioFacturaService envioService) {
        this.facturaService = facturaService;
        this.envioService = envioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Generar la factura con todos los ítems pendientes de una mascota")
    @ApiResponse(responseCode = "201", description = "Factura generada y sus ítems marcados como facturados")
    @ApiResponse(responseCode = "409", description = "Un ítem fue facturado por otro cajero")
    @ApiResponse(responseCode = "422", description = "Sin ítems pendientes o servicio sin tarifa")
    @ApiResponse(responseCode = "503", description = "Un microservicio de origen no responde; no se crea la factura")
    public Factura generar(@Valid @RequestBody FacturaRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return facturaService.generar(solicitud, jwt.getClaimAsString("correo"));
    }

    @GetMapping
    @PreAuthorize(Permisos.CONSULTA_TARIFAS)
    @Operation(summary = "Consultar facturas, de la más reciente a la más antigua")
    @ApiResponse(responseCode = "200", description = "Facturas paginadas")
    public PaginaResponse<Factura> consultar(@RequestParam(required = false) EstadoFactura estado,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return facturaService.consultar(estado, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.CONSULTA_TARIFAS)
    @Operation(summary = "Consultar una factura")
    @ApiResponse(responseCode = "200", description = "Factura")
    @ApiResponse(responseCode = "404", description = "La factura no existe")
    public Factura obtener(@PathVariable String id) {
        return facturaService.buscar(id);
    }

    @PatchMapping("/{id}/ajustes")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Aplicar un descuento y recalcular el impuesto")
    @ApiResponse(responseCode = "200", description = "Factura recalculada")
    @ApiResponse(responseCode = "422", description = "Descuento mayor que el subtotal o factura con pagos")
    public Factura ajustes(@PathVariable String id, @Valid @RequestBody AjustesFacturaRequest solicitud) {
        return facturaService.aplicarAjustes(id, solicitud);
    }

    @PostMapping("/{id}/pagos")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Registrar pagos en efectivo, tarjeta o transferencia, incluso combinados")
    @ApiResponse(responseCode = "200", description = "Pago registrado; con saldo cero queda PAGADA")
    @ApiResponse(responseCode = "400", description = "Medio de pago inválido")
    @ApiResponse(responseCode = "422", description = "Sin caja abierta o pago mayor al saldo")
    public Factura pagar(@PathVariable String id, @Valid @RequestBody PagoRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return facturaService.pagar(id, solicitud.pagos(), jwt.getSubject(), jwt.getClaimAsString("correo"));
    }

    @PostMapping("/{id}/abonos")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Registrar un abono a una factura a crédito")
    @ApiResponse(responseCode = "200", description = "Abono registrado")
    @ApiResponse(responseCode = "422", description = "Sin caja abierta, abono mayor al saldo o factura de contado")
    public Factura abonar(@PathVariable String id, @Valid @RequestBody ItemPagoRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return facturaService.abonar(id, solicitud, jwt.getSubject(), jwt.getClaimAsString("correo"));
    }

    @PatchMapping("/{id}/anulacion")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Anular una factura; sus ítems vuelven a pendientes")
    @ApiResponse(responseCode = "200", description = "Factura ANULADA")
    @ApiResponse(responseCode = "400", description = "Falta el motivo")
    @ApiResponse(responseCode = "422", description = "Ya anulada o con pagos en una caja cerrada")
    public Factura anular(@PathVariable String id, @Valid @RequestBody AnulacionRequest solicitud) {
        return facturaService.anular(id, solicitud.motivo());
    }

    @PostMapping("/{id}/envio-correo")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Enviar el PDF de la factura al correo del propietario")
    @ApiResponse(responseCode = "200", description = "Factura enviada")
    @ApiResponse(responseCode = "422", description = "El propietario no tiene correo")
    @ApiResponse(responseCode = "502", description = "El servicio de correo no responde; la factura no cambia")
    public Factura enviar(@PathVariable String id) {
        return envioService.enviar(id);
    }
}
