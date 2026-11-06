package com.vetcare.farmacia.controller;

import com.vetcare.farmacia.dto.AjusteRequest;
import com.vetcare.farmacia.dto.AlertaStockResponse;
import com.vetcare.farmacia.dto.ConsumoRequest;
import com.vetcare.farmacia.dto.LoteProximoResponse;
import com.vetcare.farmacia.dto.LoteRequest;
import com.vetcare.farmacia.dto.LoteResponse;
import com.vetcare.farmacia.dto.MedicamentoRequest;
import com.vetcare.farmacia.dto.MedicamentoResponse;
import com.vetcare.farmacia.dto.PaginaResponse;
import com.vetcare.farmacia.model.MovimientoKardex;
import com.vetcare.farmacia.security.Permisos;
import com.vetcare.farmacia.service.InventarioService;
import com.vetcare.farmacia.service.MedicamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Medicamentos e inventario", description = "HU-051, HU-052, HU-054, HU-055 y HU-056 (RN-04)")
public class MedicamentoController {

    private final MedicamentoService medicamentoService;
    private final InventarioService inventarioService;

    public MedicamentoController(MedicamentoService medicamentoService, InventarioService inventarioService) {
        this.medicamentoService = medicamentoService;
        this.inventarioService = inventarioService;
    }

    @PostMapping("/api/medicamentos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.FARMACIA)
    @Operation(summary = "Registrar un medicamento o insumo")
    @ApiResponse(responseCode = "201", description = "Registrado con stock 0")
    @ApiResponse(responseCode = "400", description = "Precio o stock mínimo negativos")
    @ApiResponse(responseCode = "409", description = "El código ya existe")
    public MedicamentoResponse registrar(@Valid @RequestBody MedicamentoRequest solicitud) {
        return medicamentoService.registrar(solicitud);
    }

    @GetMapping("/api/medicamentos")
    @PreAuthorize(Permisos.CONSULTA_CATALOGO)
    @Operation(summary = "Buscar en el catálogo por nombre o código")
    @ApiResponse(responseCode = "200", description = "Productos paginados con su stock disponible")
    public PaginaResponse<MedicamentoResponse> buscar(@RequestParam(required = false) String texto,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return medicamentoService.buscarPorTexto(texto, page, size);
    }

    @GetMapping("/api/medicamentos/{id}")
    @PreAuthorize(Permisos.CONSULTA_CATALOGO)
    @Operation(summary = "Consultar un medicamento")
    @ApiResponse(responseCode = "200", description = "Producto con su stock disponible")
    @ApiResponse(responseCode = "404", description = "El medicamento no existe")
    public MedicamentoResponse obtener(@PathVariable String id) {
        return medicamentoService.obtener(id);
    }

    @PostMapping("/api/medicamentos/{id}/lotes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.FARMACIA)
    @Operation(summary = "Registrar un lote con vencimiento; aumenta el stock y genera una entrada")
    @ApiResponse(responseCode = "201", description = "Lote registrado")
    @ApiResponse(responseCode = "400", description = "Cantidad no positiva")
    @ApiResponse(responseCode = "404", description = "El medicamento no existe")
    @ApiResponse(responseCode = "409", description = "El lote ya existe")
    @ApiResponse(responseCode = "422", description = "La fecha de vencimiento ya pasó")
    public LoteResponse registrarLote(@PathVariable String id, @Valid @RequestBody LoteRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return medicamentoService.registrarLote(id, solicitud, jwt.getClaimAsString("correo"));
    }

    @GetMapping("/api/medicamentos/{id}/lotes")
    @PreAuthorize(Permisos.CONSULTA_PROVEEDORES)
    @Operation(summary = "Consultar los lotes de un medicamento")
    @ApiResponse(responseCode = "200", description = "Lotes, primero el que vence antes")
    @ApiResponse(responseCode = "404", description = "El medicamento no existe")
    public List<LoteResponse> lotes(@PathVariable String id) {
        return medicamentoService.lotesDe(id);
    }

    @GetMapping("/api/medicamentos/{id}/kardex")
    @PreAuthorize(Permisos.CONSULTA_PROVEEDORES)
    @Operation(summary = "Consultar el kardex de un medicamento")
    @ApiResponse(responseCode = "200", description = "Movimientos con tipo, lote, cantidad, saldo y responsable")
    @ApiResponse(responseCode = "404", description = "El medicamento no existe")
    public PaginaResponse<MovimientoKardex> kardex(@PathVariable String id,
                                                   @RequestParam(required = false) Instant desde,
                                                   @RequestParam(required = false) Instant hasta,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        return medicamentoService.kardex(id, desde, hasta, page, size);
    }

    @PostMapping("/api/medicamentos/{id}/ajustes")
    @PreAuthorize(Permisos.FARMACIA)
    @Operation(summary = "Registrar un ajuste de inventario con motivo")
    @ApiResponse(responseCode = "200", description = "Lote actualizado")
    @ApiResponse(responseCode = "400", description = "Sin motivo o cantidad cero")
    @ApiResponse(responseCode = "404", description = "El lote no existe para ese medicamento")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente: dejaría el lote en negativo")
    public LoteResponse ajustar(@PathVariable String id, @Valid @RequestBody AjusteRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return LoteResponse.de(inventarioService.ajustar(id, solicitud, jwt.getClaimAsString("correo")));
    }

    @GetMapping("/api/alertas/stock-minimo")
    @PreAuthorize(Permisos.CONSULTA_PROVEEDORES)
    @Operation(summary = "Productos con stock disponible igual o menor que su mínimo")
    @ApiResponse(responseCode = "200", description = "Alertas; no cuentan los lotes vencidos")
    public List<AlertaStockResponse> alertasStock() {
        return medicamentoService.alertasStockMinimo();
    }

    @GetMapping("/api/lotes/proximos-vencer")
    @PreAuthorize(Permisos.CONSULTA_PROVEEDORES)
    @Operation(summary = "Lotes con stock próximos a vencer")
    @ApiResponse(responseCode = "200", description = "Lotes, primero los más cercanos a vencer")
    public List<LoteProximoResponse> proximosVencer(@RequestParam(defaultValue = "60") int dias) {
        return medicamentoService.lotesProximosAVencer(dias);
    }

    @PostMapping("/api/lotes/{numero}/consumos")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Descontar del lote una vacuna aplicada (D11)")
    @ApiResponse(responseCode = "200", description = "Lote actualizado")
    @ApiResponse(responseCode = "404", description = "El lote no existe")
    @ApiResponse(responseCode = "409", description = "Sin stock o número de lote ambiguo")
    @ApiResponse(responseCode = "422", description = "El lote está vencido")
    public LoteResponse consumir(@PathVariable String numero, @Valid @RequestBody ConsumoRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return LoteResponse.de(inventarioService.consumirPorNumero(numero, solicitud.cantidad(), jwt.getClaimAsString("correo"), "Vacuna aplicada"));
    }
}
