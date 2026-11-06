package com.vetcare.pacientes.controller;

import com.vetcare.pacientes.dto.LoginRequest;
import com.vetcare.pacientes.dto.LoginResponse;
import com.vetcare.pacientes.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "HU-002")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión y obtener un token de 30 minutos")
    @ApiResponse(responseCode = "200", description = "Inicio de sesión correcto")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos, o usuario desactivado")
    public LoginResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
        return authService.iniciarSesion(solicitud);
    }
}
