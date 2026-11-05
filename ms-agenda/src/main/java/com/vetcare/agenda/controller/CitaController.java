package com.vetcare.agenda.controller;

import com.vetcare.agenda.dto.CancelacionRequest;
import com.vetcare.agenda.dto.CitaRequest;
import com.vetcare.agenda.dto.CitaResponse;
import com.vetcare.agenda.dto.ReprogramacionRequest;
import com.vetcare.agenda.security.Permisos;
import com.vetcare.agenda.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/citas")
@Tag(name = "Citas", description = "HU-022, HU-025, HU-026, HU-027 y HU-035")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.RECEPCIONISTA)
    @Operation(summary = "Agendar una cita respetando los cupos del veterinario (RN-03)")
    @ApiResponse(responseCode = "201", description = "Cita programada")
    @ApiResponse(responseCode = "404", description = "La mascota no existe o el veterinario no tiene horario")
    @ApiResponse(responseCode = "409", description = "No hay cupos disponibles o la franja está ocupada")
    @ApiResponse(responseCode = "422", description = "Fuera de horario, fecha pasada o mascota no activa")
    @ApiResponse(responseCode = "503", description = "ms-pacientes no está disponible")
    public CitaResponse agendar(@Valid @RequestBody CitaRequest solicitud) {
        return CitaResponse.de(citaService.agendar(solicitud));
    }

    @GetMapping("/mi-agenda")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Ver la agenda propia de una fecha, ordenada por hora")
    @ApiResponse(responseCode = "200", description = "Citas del veterinario autenticado")
    public List<CitaResponse> miAgenda(@RequestParam(required = false) LocalDate fecha, @AuthenticationPrincipal Jwt jwt) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        return citaService.agendaDe(jwt.getSubject(), dia).stream().map(CitaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.CONSULTA_CITA)
    @Operation(summary = "Consultar una cita")
    @ApiResponse(responseCode = "200", description = "Cita encontrada")
    @ApiResponse(responseCode = "404", description = "La cita no existe")
    public CitaResponse obtener(@PathVariable String id) {
        return CitaResponse.de(citaService.buscar(id));
    }

    @PutMapping("/{id}/reprogramacion")
    @PreAuthorize(Permisos.RECEPCIONISTA)
    @Operation(summary = "Reprogramar una cita y liberar el cupo de origen")
    @ApiResponse(responseCode = "200", description = "Cita reprogramada")
    @ApiResponse(responseCode = "404", description = "La cita no existe")
    @ApiResponse(responseCode = "409", description = "El destino no tiene cupo o la franja está ocupada")
    @ApiResponse(responseCode = "422", description = "La cita ya no está programada o el destino está fuera de horario")
    public CitaResponse reprogramar(@PathVariable String id, @Valid @RequestBody ReprogramacionRequest solicitud) {
        return CitaResponse.de(citaService.reprogramar(id, solicitud));
    }

    @PatchMapping("/{id}/cancelacion")
    @PreAuthorize(Permisos.RECEPCIONISTA)
    @Operation(summary = "Cancelar una cita y liberar el cupo y la franja")
    @ApiResponse(responseCode = "200", description = "Cita cancelada")
    @ApiResponse(responseCode = "404", description = "La cita no existe")
    @ApiResponse(responseCode = "422", description = "La cita ya no se puede cancelar")
    public CitaResponse cancelar(@PathVariable String id, @Valid @RequestBody CancelacionRequest solicitud) {
        return CitaResponse.de(citaService.cancelar(id, solicitud.motivo()));
    }

    @PatchMapping("/{id}/llegada")
    @PreAuthorize(Permisos.RECEPCIONISTA)
    @Operation(summary = "Registrar la llegada del paciente")
    @ApiResponse(responseCode = "200", description = "Cita en espera")
    @ApiResponse(responseCode = "422", description = "La cita no es de hoy o no está programada")
    public CitaResponse registrarLlegada(@PathVariable String id) {
        return CitaResponse.de(citaService.registrarLlegada(id));
    }

    @PatchMapping("/{id}/atencion")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Marcar la cita como atendida al cerrar la consulta")
    @ApiResponse(responseCode = "200", description = "Cita atendida")
    @ApiResponse(responseCode = "422", description = "La cita no está en espera")
    public CitaResponse marcarAtendida(@PathVariable String id) {
        return CitaResponse.de(citaService.marcarAtendida(id));
    }
}
