package com.vetcare.facturacion.controller;

import com.vetcare.facturacion.dto.IngresosResponse;
import com.vetcare.facturacion.model.MedioPago;
import com.vetcare.facturacion.model.TipoServicio;
import com.vetcare.facturacion.security.Permisos;
import com.vetcare.facturacion.service.ReporteIngresosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "HU-069 y HU-072")
public class ReporteController {

    private final ReporteIngresosService reporteService;

    public ReporteController(ReporteIngresosService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/ingresos")
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Ingresos por período, servicio y medio de pago")
    @ApiResponse(responseCode = "200", description = "Totales; no cuentan los pagos revertidos")
    @ApiResponse(responseCode = "400", description = "La fecha inicial es posterior a la final")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    public IngresosResponse ingresos(@RequestParam LocalDate desde, @RequestParam LocalDate hasta,
                                     @RequestParam(required = false) TipoServicio servicio,
                                     @RequestParam(required = false) MedioPago medioPago) {
        return reporteService.ingresos(desde, hasta, servicio, medioPago);
    }
}
