package com.vetcare.pacientes.controller;

import com.vetcare.pacientes.dto.CambioContrasenaRequest;
import com.vetcare.pacientes.dto.PaginaResponse;
import com.vetcare.pacientes.dto.UsuarioRequest;
import com.vetcare.pacientes.dto.UsuarioResponse;
import com.vetcare.pacientes.model.EstadoUsuario;
import com.vetcare.pacientes.security.Permisos;
import com.vetcare.pacientes.security.Rol;
import com.vetcare.pacientes.service.UsuarioService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "HU-001, HU-004, HU-005 y HU-006")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Crear un usuario de cualquiera de los 10 roles")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o rol inexistente")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @ApiResponse(responseCode = "404", description = "El propietario vinculado no existe")
    @ApiResponse(responseCode = "409", description = "El correo ya está registrado")
    @ApiResponse(responseCode = "422", description = "Falta el vínculo con el propietario o el proveedor")
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest solicitud) {
        return usuarioService.crear(solicitud);
    }

    @GetMapping
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Consultar usuarios filtrando por rol y estado")
    @ApiResponse(responseCode = "200", description = "Usuarios paginados, sin contraseña")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    public PaginaResponse<UsuarioResponse> consultar(@RequestParam(required = false) Rol rol,
                                                     @RequestParam(required = false) EstadoUsuario estado,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return usuarioService.consultar(rol, estado, page, size);
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize(Permisos.ADMINISTRADOR)
    @Operation(summary = "Desactivar un usuario", description = "Un token ya emitido sigue vigente hasta que expire, en máximo 30 minutos.")
    @ApiResponse(responseCode = "200", description = "Usuario desactivado")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    @ApiResponse(responseCode = "422", description = "Intento de desactivar el propio usuario")
    public UsuarioResponse desactivar(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.desactivar(id, jwt.getSubject());
    }

    @PutMapping("/me/contrasena")
    @Operation(summary = "Cambiar la contraseña propia")
    @ApiResponse(responseCode = "200", description = "Contraseña actualizada")
    @ApiResponse(responseCode = "400", description = "La contraseña nueva tiene menos de 8 caracteres")
    @ApiResponse(responseCode = "422", description = "La contraseña actual no es correcta")
    public void cambiarContrasena(@Valid @RequestBody CambioContrasenaRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.cambiarContrasena(jwt.getSubject(), solicitud);
    }
}
