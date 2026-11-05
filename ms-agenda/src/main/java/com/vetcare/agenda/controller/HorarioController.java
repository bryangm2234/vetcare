package com.vetcare.agenda.controller;

import com.vetcare.agenda.dto.DisponibilidadResponse;
import com.vetcare.agenda.dto.HorarioRequest;
import com.vetcare.agenda.model.Horario;
import com.vetcare.agenda.model.TipoCita;
import com.vetcare.agenda.security.Permisos;
import com.vetcare.agenda.service.HorarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Horarios y disponibilidad", description = "HU-021 y HU-023")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @PutMapping("/api/horarios/{veterinarioId}")
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Configurar el horario y los cupos diarios de un veterinario", description = "veterinarioId es el id del usuario en ms-pacientes.")
    @ApiResponse(responseCode = "200", description = "Horario guardado")
    @ApiResponse(responseCode = "400", description = "Hora de fin anterior a la de inicio o cupos menores que 1")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @ApiResponse(responseCode = "422", description = "Los cupos son menores a las citas ya agendadas")
    public Horario configurar(@PathVariable String veterinarioId, @Valid @RequestBody HorarioRequest solicitud) {
        return horarioService.configurar(veterinarioId, TipoCita.VETERINARIA, solicitud);
    }

    @GetMapping("/api/disponibilidad")
    @PreAuthorize(Permisos.CONSULTA_DISPONIBILIDAD)
    @Operation(summary = "Consultar franjas libres y cupos restantes por fecha")
    @ApiResponse(responseCode = "200", description = "Disponibilidad por veterinario")
    @ApiResponse(responseCode = "400", description = "La fecha ya pasó")
    @ApiResponse(responseCode = "404", description = "El veterinario no tiene horario")
    public List<DisponibilidadResponse> disponibilidad(@RequestParam LocalDate fecha,
                                                       @RequestParam(required = false) String veterinarioId) {
        return horarioService.disponibilidad(fecha, veterinarioId);
    }
}
