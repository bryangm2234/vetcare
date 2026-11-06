package com.vetcare.farmacia.controller;

import com.vetcare.farmacia.dto.PaginaResponse;
import com.vetcare.farmacia.model.Auditoria;
import com.vetcare.farmacia.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
@Tag(name = "Auditoría", description = "HU-009")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Consultar la auditoría de este microservicio")
    @ApiResponse(responseCode = "200", description = "Registros, del más reciente al más antiguo")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    public PaginaResponse<Auditoria> consultar(@RequestParam(required = false) String usuario,
                                               @RequestParam(required = false) String accion,
                                               @RequestParam(required = false) Instant desde,
                                               @RequestParam(required = false) Instant hasta,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return auditoriaService.consultar(usuario, accion, desde, hasta, page, size);
    }
}
