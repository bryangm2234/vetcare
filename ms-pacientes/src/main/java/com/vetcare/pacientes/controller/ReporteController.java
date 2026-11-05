package com.vetcare.pacientes.controller;

import com.vetcare.pacientes.dto.ReporteMascotaItem;
import com.vetcare.pacientes.security.Permisos;
import com.vetcare.pacientes.service.MascotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "HU-019")
public class ReporteController {

    private final MascotaService mascotaService;

    public ReporteController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    @GetMapping("/mascotas")
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Total de mascotas por especie y raza")
    @ApiResponse(responseCode = "200", description = "Totales agrupados")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    public List<ReporteMascotaItem> mascotas() {
        return mascotaService.reportePorEspecieYRaza();
    }
}
