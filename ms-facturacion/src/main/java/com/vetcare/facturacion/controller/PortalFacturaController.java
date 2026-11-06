package com.vetcare.facturacion.controller;

import com.vetcare.facturacion.model.Factura;
import com.vetcare.facturacion.security.Permisos;
import com.vetcare.facturacion.service.DocumentoFacturaService;
import com.vetcare.facturacion.service.FacturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/facturas")
@Tag(name = "Portal del propietario", description = "HU-067")
public class PortalFacturaController {

    private final FacturaService facturaService;
    private final DocumentoFacturaService documentoService;

    public PortalFacturaController(FacturaService facturaService, DocumentoFacturaService documentoService) {
        this.facturaService = facturaService;
        this.documentoService = documentoService;
    }

    @GetMapping
    @PreAuthorize(Permisos.PROPIETARIO)
    @Operation(summary = "Consultar las facturas propias")
    @ApiResponse(responseCode = "200", description = "Facturas con fecha, total, saldo y estado")
    public List<Factura> misFacturas(@AuthenticationPrincipal Jwt jwt) {
        return facturaService.dePropietario(jwt.getClaimAsString("propietarioId"));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize(Permisos.PROPIETARIO)
    @Operation(summary = "Descargar una factura propia en PDF")
    @ApiResponse(responseCode = "200", description = "PDF con el detalle")
    @ApiResponse(responseCode = "403", description = "La factura es de otro propietario")
    public ResponseEntity<byte[]> descargar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        Factura factura = facturaService.propiaDe(id, jwt.getClaimAsString("propietarioId"));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(factura.getNumero() + ".pdf").build().toString())
                .body(documentoService.pdf(factura));
    }
}
