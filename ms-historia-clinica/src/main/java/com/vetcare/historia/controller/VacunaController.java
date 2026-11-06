package com.vetcare.historia.controller;

import com.vetcare.historia.dto.AlertaVacunaResponse;
import com.vetcare.historia.dto.EsquemaVacunacionRequest;
import com.vetcare.historia.dto.VacunaRequest;
import com.vetcare.historia.dto.VacunaResponse;
import com.vetcare.historia.model.EsquemaVacunacion;
import com.vetcare.historia.security.Permisos;
import com.vetcare.historia.service.VacunaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Vacunación", description = "HU-036 a HU-039 (RN-01)")
public class VacunaController {

    private final VacunaService vacunaService;

    public VacunaController(VacunaService vacunaService) {
        this.vacunaService = vacunaService;
    }

    @PostMapping("/api/esquemas-vacunacion")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Definir el esquema de una vacuna por especie")
    @ApiResponse(responseCode = "201", description = "Esquema creado")
    @ApiResponse(responseCode = "400", description = "Dosis o intervalo inválidos")
    @ApiResponse(responseCode = "409", description = "Ya existe esa vacuna para la especie")
    public EsquemaVacunacion crearEsquema(@Valid @RequestBody EsquemaVacunacionRequest solicitud) {
        return vacunaService.crearEsquema(solicitud);
    }

    @GetMapping("/api/esquemas-vacunacion")
    @PreAuthorize(Permisos.CONSULTA_ESQUEMAS)
    @Operation(summary = "Consultar los esquemas de vacunación")
    @ApiResponse(responseCode = "200", description = "Esquemas")
    public List<EsquemaVacunacion> esquemas(@RequestParam(required = false) String especie) {
        return vacunaService.esquemasDe(especie);
    }

    @PostMapping("/api/vacunas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Registrar una dosis y calcular la próxima según el esquema")
    @ApiResponse(responseCode = "201", description = "Dosis registrada")
    @ApiResponse(responseCode = "400", description = "Fecha futura o datos inválidos")
    @ApiResponse(responseCode = "409", description = "El lote no tiene stock")
    @ApiResponse(responseCode = "422", description = "Sin esquema o esquema ya completo")
    public VacunaResponse registrar(@Valid @RequestBody VacunaRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return vacunaService.registrar(solicitud, jwt.getSubject());
    }

    @GetMapping("/api/vacunas/alertas")
    @PreAuthorize(Permisos.RECEPCIONISTA)
    @Operation(summary = "Vacunas próximas a vencer o vencidas")
    @ApiResponse(responseCode = "200", description = "Alertas con estado PROXIMA o VENCIDA")
    public List<AlertaVacunaResponse> alertas(@RequestParam(defaultValue = "30") int dias) {
        return vacunaService.alertas(dias);
    }

    @GetMapping("/api/portal/vacunas/pendientes")
    @PreAuthorize(Permisos.PROPIETARIO)
    @Operation(summary = "Consultar las dosis pendientes de las mascotas propias")
    @ApiResponse(responseCode = "200", description = "Dosis con estado PENDIENTE o VENCIDA")
    public List<AlertaVacunaResponse> pendientes() {
        return vacunaService.pendientesDelPropietario();
    }

    @GetMapping("/api/portal/mascotas/{mascotaId}/carne")
    @PreAuthorize(Permisos.PROPIETARIO)
    @Operation(summary = "Consultar el carné de vacunación de una mascota propia")
    @ApiResponse(responseCode = "200", description = "Vacunas con dosis, fecha y próxima dosis")
    @ApiResponse(responseCode = "403", description = "La mascota es de otro propietario")
    public List<VacunaResponse> carne(@PathVariable String mascotaId) {
        return vacunaService.carne(mascotaId);
    }
}
