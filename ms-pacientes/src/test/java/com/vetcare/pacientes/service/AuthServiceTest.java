package com.vetcare.pacientes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetcare.pacientes.dto.LoginRequest;
import com.vetcare.pacientes.dto.LoginResponse;
import com.vetcare.pacientes.exception.AutenticacionFallidaException;
import com.vetcare.pacientes.model.EstadoUsuario;
import com.vetcare.pacientes.model.Usuario;
import com.vetcare.pacientes.repository.UsuarioRepository;
import com.vetcare.pacientes.security.JwtService;
import com.vetcare.pacientes.security.Rol;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditoriaService auditoriaService;
    @InjectMocks
    private AuthService authService;

    private Usuario usuario(EstadoUsuario estado) {
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        usuario.setCorreo("cajero@vetcare.test");
        usuario.setContrasenaHash("hash");
        usuario.setRol(Rol.CAJERO);
        usuario.setEstado(estado);
        return usuario;
    }

    @Test
    void hu002_inicioDeSesionValido() {
        Usuario usuario = usuario(EstadoUsuario.ACTIVO);
        Instant expira = Instant.now().plusSeconds(1800);
        when(usuarioRepository.findByCorreo("cajero@vetcare.test")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-valida", "hash")).thenReturn(true);
        when(jwtService.emitir(usuario)).thenReturn(new Jwt("token", Instant.now(), expira, Map.of("alg", "RS256"), Map.of("sub", "u1")));

        LoginResponse respuesta = authService.iniciarSesion(new LoginRequest("Cajero@VetCare.test", "clave-valida"));

        assertThat(respuesta.token()).isEqualTo("token");
        assertThat(respuesta.rol()).isEqualTo(Rol.CAJERO);
        assertThat(respuesta.expiraEn()).isEqualTo(expira);
        verify(auditoriaService).registrar("cajero@vetcare.test", "INICIO_SESION", "auth", "EXITOSO");
    }

    @Test
    void hu002_contrasenaIncorrectaSeAuditaYNoEmiteToken() {
        when(usuarioRepository.findByCorreo("cajero@vetcare.test")).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.iniciarSesion(new LoginRequest("cajero@vetcare.test", "incorrecta")))
                .isInstanceOf(AutenticacionFallidaException.class)
                .hasMessage("Correo o contraseña incorrectos");
        verify(auditoriaService).registrar("cajero@vetcare.test", "INICIO_SESION", "auth", "FALLIDO");
        verify(jwtService, never()).emitir(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void hu002_correoInexistenteRespondeIgualQueContrasenaIncorrecta() {
        when(usuarioRepository.findByCorreo("nadie@vetcare.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.iniciarSesion(new LoginRequest("nadie@vetcare.test", "clave")))
                .isInstanceOf(AutenticacionFallidaException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    void hu004_usuarioDesactivadoNoPuedeIniciarSesion() {
        when(usuarioRepository.findByCorreo("cajero@vetcare.test")).thenReturn(Optional.of(usuario(EstadoUsuario.INACTIVO)));

        assertThatThrownBy(() -> authService.iniciarSesion(new LoginRequest("cajero@vetcare.test", "clave-valida")))
                .isInstanceOf(AutenticacionFallidaException.class);
    }
}
