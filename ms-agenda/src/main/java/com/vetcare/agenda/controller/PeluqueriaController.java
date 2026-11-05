package com.vetcare.agenda.controller;

import com.vetcare.agenda.dto.CitaPeluqueriaRequest;
import com.vetcare.agenda.dto.CitaResponse;
import com.vetcare.agenda.dto.HorarioRequest;
import com.vetcare.agenda.dto.ItemPendienteResponse;
import com.vetcare.agenda.dto.MarcaFacturadoRequest;
import com.vetcare.agenda.model.Cita;
import com.vetcare.agenda.dto.ServicioRealizadoRequest;
import com.vetcare.agenda.model.Horario;
import com.vetcare.agenda.model.TipoCita;
import com.vetcare.agenda.security.Permisos;
import com.vetcare.agenda.service.CitaService;
import com.vetcare.agenda.service.HorarioService;
import com.vetcare.agenda.service.MarcadorFacturacion;
import com.vetcare.agenda.service.PeluqueriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/peluqueria")
@Tag(name = "Peluquería", description = "HU-028 y HU-029")
public class PeluqueriaController {

    private final HorarioService horarioService;
    private final CitaService citaService;
    private final PeluqueriaService peluqueriaService;
    private final MarcadorFacturacion marcador;

    public PeluqueriaController(HorarioService horarioService, CitaService citaService, PeluqueriaService peluqueriaService,
                                MarcadorFacturacion marcador) {
        this.marcador = marcador;
        this.horarioService = horarioService;
        this.citaService = citaService;
        this.peluqueriaService = peluqueriaService;
    }

    @PutMapping("/horario")
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Configurar el horario y los cupos propios de peluquería")
    @ApiResponse(responseCode = "200", description = "Horario guardado")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    public Horario configurar(@Valid @RequestBody HorarioRequest solicitud) {
        return horarioService.configurar(HorarioService.RECURSO_PELUQUERIA, TipoCita.PELUQUERIA, solicitud);
    }

    @PostMapping("/citas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.PELUQUERIA)
    @Operation(summary = "Agendar baño o estética con cupos de peluquería")
    @ApiResponse(responseCode = "201", description = "Servicio agendado")
    @ApiResponse(responseCode = "409", description = "No hay cupos disponibles o la franja está ocupada")
    @ApiResponse(responseCode = "422", description = "Fuera de horario, fecha pasada o mascota no activa")
    public CitaResponse agendar(@Valid @RequestBody CitaPeluqueriaRequest solicitud) {
        return CitaResponse.de(citaService.agendarPeluqueria(solicitud.mascotaId(), solicitud.fecha(), solicitud.hora(), solicitud.servicio()));
    }

    @PatchMapping("/citas/{id}/servicio")
    @PreAuthorize(Permisos.PELUQUERIA)
    @Operation(summary = "Registrar el servicio realizado con observaciones")
    @ApiResponse(responseCode = "200", description = "Servicio realizado y pendiente de facturar")
    @ApiResponse(responseCode = "409", description = "El servicio ya está registrado")
    @ApiResponse(responseCode = "422", description = "La cita está cancelada o no es de peluquería")
    public CitaResponse registrarServicio(@PathVariable String id, @Valid @RequestBody ServicioRealizadoRequest solicitud) {
        return CitaResponse.de(peluqueriaService.registrarServicio(id, solicitud.observaciones()));
    }

    @GetMapping("/items-pendientes")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Servicios realizados pendientes de facturar")
    @ApiResponse(responseCode = "200", description = "Ítems pendientes")
    public List<ItemPendienteResponse> itemsPendientes() {
        return peluqueriaService.itemsPendientes();
    }

    @PatchMapping("/items-facturados")
    @PreAuthorize(Permisos.CAJERO)
    @Operation(summary = "Marcar o liberar ítems facturados; lo invoca ms-facturacion")
    @ApiResponse(responseCode = "200", description = "Ítems actualizados")
    @ApiResponse(responseCode = "409", description = "Un ítem no existe o ya cambió de estado; no se aplica ningún cambio")
    public void marcarFacturados(@Valid @RequestBody MarcaFacturadoRequest solicitud) {
        marcador.marcar(Cita.class, solicitud.ids(), solicitud.facturado());
    }
}
