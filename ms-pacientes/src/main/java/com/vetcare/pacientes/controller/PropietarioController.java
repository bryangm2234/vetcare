package com.vetcare.pacientes.controller;

import com.vetcare.pacientes.dto.ContactoRequest;
import com.vetcare.pacientes.dto.MascotaResponse;
import com.vetcare.pacientes.dto.PaginaResponse;
import com.vetcare.pacientes.dto.PropietarioRequest;
import com.vetcare.pacientes.dto.PropietarioResponse;
import com.vetcare.pacientes.security.Permisos;
import com.vetcare.pacientes.service.PropietarioService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/propietarios")
@Tag(name = "Propietarios", description = "HU-011, HU-013, HU-014 y HU-015")
public class PropietarioController {

    private final PropietarioService propietarioService;

    public PropietarioController(PropietarioService propietarioService) {
        this.propietarioService = propietarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.RECEPCIONISTA)
    @Operation(summary = "Registrar un propietario")
    @ApiResponse(responseCode = "201", description = "Propietario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "El documento ya está registrado")
    public PropietarioResponse registrar(@Valid @RequestBody PropietarioRequest solicitud) {
        return propietarioService.registrar(solicitud);
    }

    @GetMapping
    @PreAuthorize(Permisos.CONSULTA_PROPIETARIOS)
    @Operation(summary = "Buscar propietarios por documento exacto o parte del nombre")
    @ApiResponse(responseCode = "200", description = "Propietarios paginados; la lista puede estar vacía")
    public PaginaResponse<PropietarioResponse> buscar(@RequestParam(required = false) String documento,
                                                      @RequestParam(required = false) String nombre,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return propietarioService.buscar(documento, nombre, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.CONSULTA_PROPIETARIOS)
    @Operation(summary = "Consultar un propietario")
    @ApiResponse(responseCode = "200", description = "Propietario encontrado")
    @ApiResponse(responseCode = "404", description = "El propietario no existe")
    public PropietarioResponse obtener(@PathVariable String id) {
        return propietarioService.obtener(id);
    }

    @PatchMapping("/{id}/contacto")
    @PreAuthorize(Permisos.RECEPCIONISTA)
    @Operation(summary = "Actualizar teléfono, correo y dirección")
    @ApiResponse(responseCode = "200", description = "Datos actualizados")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "El propietario no existe")
    public PropietarioResponse actualizarContacto(@PathVariable String id, @Valid @RequestBody ContactoRequest solicitud) {
        return propietarioService.actualizarContacto(id, solicitud);
    }

    @GetMapping("/{id}/mascotas")
    @PreAuthorize(Permisos.CONSULTA_PROPIETARIOS)
    @Operation(summary = "Consultar las mascotas de un propietario")
    @ApiResponse(responseCode = "200", description = "Mascotas con su estado; la lista puede estar vacía")
    @ApiResponse(responseCode = "404", description = "El propietario no existe")
    public List<MascotaResponse> mascotas(@PathVariable String id) {
        return propietarioService.mascotasDe(id);
    }
}
