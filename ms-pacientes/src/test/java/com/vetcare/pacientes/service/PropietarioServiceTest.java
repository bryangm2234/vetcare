package com.vetcare.pacientes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetcare.pacientes.dto.ContactoRequest;
import com.vetcare.pacientes.dto.PropietarioRequest;
import com.vetcare.pacientes.dto.PropietarioResponse;
import com.vetcare.pacientes.exception.ConflictoException;
import com.vetcare.pacientes.exception.RecursoNoEncontradoException;
import com.vetcare.pacientes.model.Propietario;
import com.vetcare.pacientes.repository.MascotaRepository;
import com.vetcare.pacientes.repository.PropietarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

@ExtendWith(MockitoExtension.class)
class PropietarioServiceTest {

    @Mock
    private PropietarioRepository propietarioRepository;
    @Mock
    private MascotaRepository mascotaRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private AuditoriaService auditoriaService;
    @InjectMocks
    private PropietarioService propietarioService;

    private final PropietarioRequest solicitud = new PropietarioRequest("109", "María Gómez", "300", "maria@correo.test", "Calle 10");

    @Test
    void hu011_registroGuardaElNombreNormalizadoParaBuscar() {
        when(propietarioRepository.existsByDocumento("109")).thenReturn(false);
        when(propietarioRepository.save(any(Propietario.class))).thenAnswer(i -> i.getArgument(0));

        PropietarioResponse respuesta = propietarioService.registrar(solicitud);

        ArgumentCaptor<Propietario> guardado = ArgumentCaptor.forClass(Propietario.class);
        verify(propietarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getNombreNormalizado()).isEqualTo("maria gomez");
        assertThat(respuesta.nombre()).isEqualTo("María Gómez");
    }

    @Test
    void hu011_documentoDuplicado() {
        when(propietarioRepository.existsByDocumento("109")).thenReturn(true);

        assertThatThrownBy(() -> propietarioService.registrar(solicitud))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("El documento ya está registrado");
        verify(propietarioRepository, never()).save(any());
    }

    @Test
    void hu014_actualizaContactoSinCambiarElDocumento() {
        Propietario propietario = new Propietario();
        propietario.setDocumento("109");
        when(propietarioRepository.findById("p1")).thenReturn(Optional.of(propietario));
        when(propietarioRepository.save(propietario)).thenReturn(propietario);

        PropietarioResponse respuesta = propietarioService.actualizarContacto("p1", new ContactoRequest("311", "nuevo@correo.test", "Carrera 5"));

        assertThat(respuesta.telefono()).isEqualTo("311");
        assertThat(respuesta.documento()).isEqualTo("109");
    }

    @Test
    void hu014_propietarioInexistente() {
        when(propietarioRepository.findById("p9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propietarioService.actualizarContacto("p9", new ContactoRequest("311", "nuevo@correo.test", "Carrera 5")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void hu015_propietarioInexistente() {
        when(propietarioRepository.findById("p9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propietarioService.mascotasDe("p9"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void hu013_normalizaTildesYMayusculas() {
        assertThat(TextoNormalizado.de("  MARÍA Gómez ")).isEqualTo("maria gomez");
    }
}
