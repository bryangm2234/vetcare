package com.vetcare.pacientes.service;

import com.vetcare.pacientes.dto.LoginRequest;
import com.vetcare.pacientes.dto.LoginResponse;
import com.vetcare.pacientes.exception.AutenticacionFallidaException;
import com.vetcare.pacientes.model.EstadoUsuario;
import com.vetcare.pacientes.model.Usuario;
import com.vetcare.pacientes.repository.UsuarioRepository;
import com.vetcare.pacientes.security.JwtService;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoriaService = auditoriaService;
    }

    public LoginResponse iniciarSesion(LoginRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                .filter(u -> passwordEncoder.matches(solicitud.contrasena(), u.getContrasenaHash()))
                .orElse(null);
        if (usuario == null) {
            auditoriaService.registrar(correo, "INICIO_SESION", "auth", "FALLIDO");
            throw new AutenticacionFallidaException();
        }
        auditoriaService.registrar(correo, "INICIO_SESION", "auth", "EXITOSO");
        Jwt token = jwtService.emitir(usuario);
        return new LoginResponse(token.getTokenValue(), "Bearer", token.getExpiresAt(), usuario.getRol());
    }
}
