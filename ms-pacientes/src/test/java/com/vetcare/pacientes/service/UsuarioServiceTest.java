package com.vetcare.pacientes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetcare.pacientes.dto.CambioContrasenaRequest;
import com.vetcare.pacientes.dto.UsuarioRequest;
import com.vetcare.pacientes.dto.UsuarioResponse;
import com.vetcare.pacientes.exception.ConflictoException;
import com.vetcare.pacientes.exception.RecursoNoEncontradoException;
import com.vetcare.pacientes.exception.ReglaNegocioException;
import com.vetcare.pacientes.model.EstadoUsuario;
import com.vetcare.pacientes.model.Usuario;
import com.vetcare.pacientes.repository.PropietarioRepository;
import com.vetcare.pacientes.repository.UsuarioRepository;
import com.vetcare.pacientes.security.Rol;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PropietarioRepository propietarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private AuditoriaService auditoriaService;
    @InjectMocks
    private UsuarioService usuarioService;

    private UsuarioRequest solicitud(Rol rol, String propietarioId, String proveedorId) {
        return new UsuarioRequest("Ana Torres", "Ana@VetCare.test", "clave-segura", rol, propietarioId, proveedorId);
    }

    @Test
    void hu001_guardaLaContrasenaCifradaYNormalizaElCorreo() {
        when(usuarioRepository.existsByCorreo("ana@vetcare.test")).thenReturn(false);
        when(passwordEncoder.encode("clave-segura")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = usuarioService.crear(solicitud(Rol.CAJERO, null, null));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getContrasenaHash()).isEqualTo("hash");
        assertThat(respuesta.correo()).isEqualTo("ana@vetcare.test");
        assertThat(respuesta.estado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void hu001_correoDuplicado() {
        when(usuarioRepository.existsByCorreo("ana@vetcare.test")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(solicitud(Rol.CAJERO, null, null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("El correo ya está registrado");
    }

    @Test
    void hu001_propietarioSinVinculo() {
        assertThatThrownBy(() -> usuarioService.crear(solicitud(Rol.PROPIETARIO, " ", null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El usuario debe estar vinculado a un propietario");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void hu001_propietarioVinculadoInexistente() {
        when(propietarioRepository.existsById("p9")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.crear(solicitud(Rol.PROPIETARIO, "p9", null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void hu001_proveedorSinVinculo() {
        assertThatThrownBy(() -> usuarioService.crear(solicitud(Rol.PROVEEDOR, null, null)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void hu004_desactivaYAudita() {
        Usuario usuario = new Usuario();
        usuario.setId("u2");
        usuario.setRol(Rol.CAJERO);
        when(usuarioRepository.findById("u2")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        assertThat(usuarioService.desactivar("u2", "admin").estado()).isEqualTo(EstadoUsuario.INACTIVO);
        verify(auditoriaService).registrar("DESACTIVAR_USUARIO", "usuarios/u2", "EXITOSO");
    }

    @Test
    void hu004_noPuedeDesactivarseASiMismo() {
        assertThatThrownBy(() -> usuarioService.desactivar("admin", "admin"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No puede desactivar su propio usuario");
    }

    @Test
    void hu004_usuarioInexistente() {
        when(usuarioRepository.findById("u9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.desactivar("u9", "admin"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void hu005_contrasenaActualIncorrectaConservaLaAnterior() {
        Usuario usuario = new Usuario();
        usuario.setContrasenaHash("hash");
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.cambiarContrasena("u1", new CambioContrasenaRequest("mala", "nueva-clave")))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(usuario.getContrasenaHash()).isEqualTo("hash");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void hu005_cambioValido() {
        Usuario usuario = new Usuario();
        usuario.setContrasenaHash("hash");
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("actual", "hash")).thenReturn(true);
        when(passwordEncoder.encode("nueva-clave")).thenReturn("hash-nuevo");

        usuarioService.cambiarContrasena("u1", new CambioContrasenaRequest("actual", "nueva-clave"));

        assertThat(usuario.getContrasenaHash()).isEqualTo("hash-nuevo");
        verify(usuarioRepository).save(usuario);
    }
}
