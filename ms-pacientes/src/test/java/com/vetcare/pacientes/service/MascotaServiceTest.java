package com.vetcare.pacientes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetcare.pacientes.dto.EstadoMascotaRequest;
import com.vetcare.pacientes.dto.MascotaRequest;
import com.vetcare.pacientes.dto.MascotaResponse;
import com.vetcare.pacientes.exception.RecursoNoEncontradoException;
import com.vetcare.pacientes.exception.ReglaNegocioException;
import com.vetcare.pacientes.model.EstadoMascota;
import com.vetcare.pacientes.model.Mascota;
import com.vetcare.pacientes.model.SexoMascota;
import com.vetcare.pacientes.repository.MascotaRepository;
import com.vetcare.pacientes.repository.PropietarioRepository;
import com.vetcare.pacientes.security.Rol;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class MascotaServiceTest {

    @Mock
    private MascotaRepository mascotaRepository;
    @Mock
    private PropietarioRepository propietarioRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private AuditoriaService auditoriaService;
    @InjectMocks
    private MascotaService mascotaService;

    private Mascota mascota(EstadoMascota estado) {
        Mascota mascota = new Mascota();
        mascota.setId("m1");
        mascota.setPropietarioId("p1");
        mascota.setEstado(estado);
        return mascota;
    }

    @Test
    void hu012_registroValidoQuedaActiva() {
        when(propietarioRepository.existsById("p1")).thenReturn(true);
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(i -> i.getArgument(0));

        MascotaResponse respuesta = mascotaService.registrar(new MascotaRequest("Max", "Canino", "Criollo", SexoMascota.MACHO,
                LocalDate.of(2022, 3, 15), 12.5, "p1"));

        assertThat(respuesta.estado()).isEqualTo(EstadoMascota.ACTIVA);
        assertThat(respuesta.propietarioId()).isEqualTo("p1");
    }

    @Test
    void hu012_propietarioInexistente() {
        when(propietarioRepository.existsById("p9")).thenReturn(false);

        assertThatThrownBy(() -> mascotaService.registrar(new MascotaRequest("Max", "Canino", "Criollo", SexoMascota.MACHO,
                LocalDate.of(2022, 3, 15), 12.5, "p9")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(mascotaRepository, never()).save(any());
    }

    @Test
    void hu003_propietarioNoConsultaMascotaAjena() {
        when(mascotaRepository.findById("m1")).thenReturn(Optional.of(mascota(EstadoMascota.ACTIVA)));

        assertThatThrownBy(() -> mascotaService.obtener("m1", Rol.PROPIETARIO, "p2"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hu003_propietarioConsultaSuMascota() {
        when(mascotaRepository.findById("m1")).thenReturn(Optional.of(mascota(EstadoMascota.ACTIVA)));

        assertThat(mascotaService.obtener("m1", Rol.PROPIETARIO, "p1").id()).isEqualTo("m1");
    }

    @Test
    void hu017_inactivar() {
        Mascota mascota = mascota(EstadoMascota.ACTIVA);
        when(mascotaRepository.findById("m1")).thenReturn(Optional.of(mascota));
        when(mascotaRepository.save(mascota)).thenReturn(mascota);

        assertThat(mascotaService.cambiarEstado("m1", new EstadoMascotaRequest(EstadoMascota.INACTIVA, null)).estado())
                .isEqualTo(EstadoMascota.INACTIVA);
    }

    @Test
    void hu017_registrarFallecimientoGuardaLaFecha() {
        Mascota mascota = mascota(EstadoMascota.ACTIVA);
        LocalDate fecha = LocalDate.of(2026, 11, 1);
        when(mascotaRepository.findById("m1")).thenReturn(Optional.of(mascota));
        when(mascotaRepository.save(mascota)).thenReturn(mascota);

        MascotaResponse respuesta = mascotaService.cambiarEstado("m1", new EstadoMascotaRequest(EstadoMascota.FALLECIDA, fecha));

        assertThat(respuesta.estado()).isEqualTo(EstadoMascota.FALLECIDA);
        assertThat(respuesta.fechaFallecimiento()).isEqualTo(fecha);
    }

    @Test
    void hu017_fallecimientoExigeFecha() {
        when(mascotaRepository.findById("m1")).thenReturn(Optional.of(mascota(EstadoMascota.ACTIVA)));

        assertThatThrownBy(() -> mascotaService.cambiarEstado("m1", new EstadoMascotaRequest(EstadoMascota.FALLECIDA, null)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void hu017_fallecimientoNoSeRevierte() {
        when(mascotaRepository.findById("m1")).thenReturn(Optional.of(mascota(EstadoMascota.FALLECIDA)));

        assertThatThrownBy(() -> mascotaService.cambiarEstado("m1", new EstadoMascotaRequest(EstadoMascota.ACTIVA, null)))
                .isInstanceOf(ReglaNegocioException.class);
        verify(mascotaRepository, never()).save(any());
    }

    @Test
    void hu018_transferenciaConservaElPropietarioAnterior() {
        Mascota mascota = mascota(EstadoMascota.ACTIVA);
        when(mascotaRepository.findById("m1")).thenReturn(Optional.of(mascota));
        when(propietarioRepository.existsById("p2")).thenReturn(true);
        when(mascotaRepository.save(mascota)).thenReturn(mascota);

        MascotaResponse respuesta = mascotaService.transferir("m1", "p2");

        assertThat(respuesta.propietarioId()).isEqualTo("p2");
        assertThat(respuesta.transferencias()).hasSize(1);
        assertThat(respuesta.transferencias().get(0).propietarioAnteriorId()).isEqualTo("p1");
    }

    @Test
    void hu018_propietarioDestinoInexistente() {
        when(mascotaRepository.findById("m1")).thenReturn(Optional.of(mascota(EstadoMascota.ACTIVA)));
        when(propietarioRepository.existsById("p9")).thenReturn(false);

        assertThatThrownBy(() -> mascotaService.transferir("m1", "p9"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void hu018_mascotaFallecidaNoSeTransfiere() {
        when(mascotaRepository.findById("m1")).thenReturn(Optional.of(mascota(EstadoMascota.FALLECIDA)));

        assertThatThrownBy(() -> mascotaService.transferir("m1", "p2"))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
