package com.vetcare.farmacia.controller;

import com.vetcare.farmacia.dto.VentasFarmaciaResponse;
import com.vetcare.farmacia.security.Permisos;
import com.vetcare.farmacia.service.DespachoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Reportes", description = "HU-072")
public class ReporteFarmaciaController {

    private final DespachoService despachoService;

    public ReporteFarmaciaController(DespachoService despachoService) {
        this.despachoService = despachoService;
    }

    @GetMapping("/api/reportes/ventas-farmacia")
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Ventas de farmacia del período")
    @ApiResponse(responseCode = "200", description = "Despachos, unidades y valor")
    @ApiResponse(responseCode = "400", description = "La fecha inicial es posterior a la final")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    public VentasFarmaciaResponse ventas(@RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return despachoService.ventas(desde, hasta);
    }
}
