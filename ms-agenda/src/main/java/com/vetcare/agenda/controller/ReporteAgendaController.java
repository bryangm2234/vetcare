package com.vetcare.agenda.controller;

import com.vetcare.agenda.dto.CitasAtendidasResponse;
import com.vetcare.agenda.dto.OcupacionResponse;
import com.vetcare.agenda.security.Permisos;
import com.vetcare.agenda.service.ReporteAgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "HU-072 y HU-073")
public class ReporteAgendaController {

    private final ReporteAgendaService reporteService;

    public ReporteAgendaController(ReporteAgendaService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/ocupacion")
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Porcentaje de cupos usados por veterinario; no cuenta citas canceladas")
    @ApiResponse(responseCode = "200", description = "Ocupación por veterinario")
    @ApiResponse(responseCode = "400", description = "La fecha inicial es posterior a la final")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    public List<OcupacionResponse> ocupacion(@RequestParam(required = false) String veterinarioId,
                                             @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return reporteService.ocupacion(veterinarioId, desde, hasta);
    }

    @GetMapping("/citas")
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Citas atendidas y canceladas del período")
    @ApiResponse(responseCode = "200", description = "Totales")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    public CitasAtendidasResponse citas(@RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return reporteService.citas(desde, hasta);
    }
}
