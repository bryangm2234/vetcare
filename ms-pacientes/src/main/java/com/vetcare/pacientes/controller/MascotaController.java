package com.vetcare.pacientes.controller;

import com.vetcare.pacientes.dto.AntecedentesRequest;
import com.vetcare.pacientes.dto.EstadoMascotaRequest;
import com.vetcare.pacientes.dto.MascotaRequest;
import com.vetcare.pacientes.dto.MascotaResponse;
import com.vetcare.pacientes.dto.TransferenciaRequest;
import com.vetcare.pacientes.security.Permisos;
import com.vetcare.pacientes.security.Rol;
import com.vetcare.pacientes.service.MascotaService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mascotas")
@Tag(name = "Mascotas", description = "HU-012, HU-016, HU-017 y HU-018")
public class MascotaController {

    private final MascotaService mascotaService;

    public MascotaController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.RECEPCIONISTA)
    @Operation(summary = "Registrar una mascota asociada a su propietario")
    @ApiResponse(responseCode = "201", description = "Mascota creada en estado ACTIVA")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, fecha futura o peso no positivo")
    @ApiResponse(responseCode = "404", description = "El propietario no existe")
    public MascotaResponse registrar(@Valid @RequestBody MascotaRequest solicitud) {
        return mascotaService.registrar(solicitud);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.CONSULTA_MASCOTA)
    @Operation(summary = "Consultar una mascota", description = "Un propietario solo puede consultar sus propias mascotas.")
    @ApiResponse(responseCode = "200", description = "Mascota encontrada")
    @ApiResponse(responseCode = "403", description = "Rol no autorizado o mascota de otro propietario")
    @ApiResponse(responseCode = "404", description = "La mascota no existe")
    public MascotaResponse obtener(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return mascotaService.obtener(id, Rol.valueOf(jwt.getClaimAsString("rol")), jwt.getClaimAsString("propietarioId"));
    }

    @PutMapping("/{id}/antecedentes")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Registrar alergias y condiciones crónicas")
    @ApiResponse(responseCode = "200", description = "Antecedentes registrados")
    @ApiResponse(responseCode = "403", description = "El usuario no es veterinario")
    @ApiResponse(responseCode = "404", description = "La mascota no existe")
    public MascotaResponse registrarAntecedentes(@PathVariable String id, @Valid @RequestBody AntecedentesRequest solicitud) {
        return mascotaService.registrarAntecedentes(id, solicitud);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize(Permisos.RECEPCIONISTA)
    @Operation(summary = "Inactivar una mascota o registrar su fallecimiento")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "404", description = "La mascota no existe")
    @ApiResponse(responseCode = "422", description = "Mascota fallecida o falta la fecha de fallecimiento")
    public MascotaResponse cambiarEstado(@PathVariable String id, @Valid @RequestBody EstadoMascotaRequest solicitud) {
        return mascotaService.cambiarEstado(id, solicitud);
    }

    @PostMapping("/{id}/transferencia")
    @PreAuthorize(Permisos.RECEPCIONISTA)
    @Operation(summary = "Transferir una mascota a otro propietario")
    @ApiResponse(responseCode = "200", description = "Mascota transferida; queda registrado el propietario anterior")
    @ApiResponse(responseCode = "404", description = "La mascota o el propietario destino no existen")
    @ApiResponse(responseCode = "422", description = "La mascota está fallecida")
    public MascotaResponse transferir(@PathVariable String id, @Valid @RequestBody TransferenciaRequest solicitud) {
        return mascotaService.transferir(id, solicitud.nuevoPropietarioId());
    }
}
