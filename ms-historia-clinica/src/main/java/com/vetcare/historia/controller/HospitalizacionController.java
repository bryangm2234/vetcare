package com.vetcare.historia.controller;

import com.vetcare.historia.dto.AltaRequest;
import com.vetcare.historia.dto.EvolucionRequest;
import com.vetcare.historia.dto.HospitalizacionRequest;
import com.vetcare.historia.dto.ItemPendienteResponse;
import com.vetcare.historia.dto.MarcaFacturadoRequest;
import com.vetcare.historia.service.MarcadorFacturacion;
import com.vetcare.historia.model.Hospitalizacion;
import com.vetcare.historia.security.Permisos;
import com.vetcare.historia.service.HospitalizacionService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hospitalizaciones")
@Tag(name = "Hospitalización", description = "HU-047 a HU-049")
public class HospitalizacionController {

    private final HospitalizacionService hospitalizacionService;
    private final MarcadorFacturacion marcador;

    public HospitalizacionController(HospitalizacionService hospitalizacionService, MarcadorFacturacion marcador) {
        this.marcador = marcador;
        this.hospitalizacionService = hospitalizacionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Ordenar la hospitalización con motivo y cubículo libre")
    @ApiResponse(responseCode = "201", description = "Hospitalización ACTIVA")
    @ApiResponse(responseCode = "404", description = "La mascota no existe")
    @ApiResponse(responseCode = "409", description = "Cubículo ocupado o mascota ya hospitalizada")
    public Hospitalizacion ordenar(@Valid @RequestBody HospitalizacionRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return hospitalizacionService.ordenar(solicitud, jwt.getSubject());
    }

    @PostMapping("/{id}/evoluciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.AUXILIAR)
    @Operation(summary = "Registrar una evolución y los medicamentos administrados")
    @ApiResponse(responseCode = "201", description = "Evolución registrada")
    @ApiResponse(responseCode = "422", description = "Medicamento sin despacho o mascota dada de alta")
    public Hospitalizacion registrarEvolucion(@PathVariable String id, @Valid @RequestBody EvolucionRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return hospitalizacionService.registrarEvolucion(id, solicitud, jwt.getClaimAsString("correo"));
    }

    @PatchMapping("/{id}/alta")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Dar de alta; libera el cubículo y genera el ítem a facturar")
    @ApiResponse(responseCode = "200", description = "Hospitalización FINALIZADA")
    @ApiResponse(responseCode = "403", description = "El usuario no es veterinario")
    @ApiResponse(responseCode = "422", description = "La mascota ya fue dada de alta")
    public Hospitalizacion darDeAlta(@PathVariable String id, @Valid @RequestBody AltaRequest solicitud) {
        return hospitalizacionService.darDeAlta(id, solicitud.indicaciones());
    }

    @GetMapping("/items-pendientes")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Estancias pendientes de facturar")
    @ApiResponse(responseCode = "200", description = "Ítems pendientes")
    public List<ItemPendienteResponse> itemsPendientes() {
        return hospitalizacionService.itemsPendientes();
    }

    @PatchMapping("/items-facturados")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Marcar o liberar ítems facturados; lo invoca ms-facturacion")
    @ApiResponse(responseCode = "200", description = "Ítems actualizados")
    @ApiResponse(responseCode = "409", description = "Un ítem no existe o ya cambió de estado; no se aplica ningún cambio")
    public void marcarFacturados(@Valid @RequestBody MarcaFacturadoRequest solicitud) {
        marcador.marcar(Hospitalizacion.class, solicitud.ids(), solicitud.facturado());
    }
}
