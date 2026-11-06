package com.vetcare.agenda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetcare.agenda.client.PacientesClient;
import com.vetcare.agenda.dto.CitaRequest;
import com.vetcare.agenda.dto.MascotaInfo;
import com.vetcare.agenda.dto.PropietarioInfo;
import com.vetcare.agenda.dto.ReprogramacionRequest;
import com.vetcare.agenda.exception.ConflictoException;
import com.vetcare.agenda.exception.ReglaNegocioException;
import com.vetcare.agenda.exception.ServicioNoDisponibleException;
import com.vetcare.agenda.model.Cita;
import com.vetcare.agenda.model.EstadoCita;
import com.vetcare.agenda.model.Horario;
import com.vetcare.agenda.repository.CitaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/**
 * Reglas de RN-03 y de los estados de la cita, sin MongoDB.
 */
@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    private static final LocalDate MANANA = LocalDate.now().plusDays(1);
    private static final LocalTime NUEVE = LocalTime.of(9, 0);

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private HorarioService horarioService;
    @Mock
    private CupoService cupoService;
    @Mock
    private PacientesClient pacientesClient;
    @Mock
    private AuditoriaService auditoriaService;
    @InjectMocks
    private CitaService citaService;

    private final CitaRequest solicitud = new CitaRequest("m1", "v1", MANANA, NUEVE, "Control");

    private Horario horario() {
        Horario horario = new Horario();
        horario.setId("v1");
        horario.setCuposDiarios(8);
        return horario;
    }

    private void mascotaEn(String estado) {
        when(pacientesClient.mascota("m1")).thenReturn(new MascotaInfo("m1", "Max", "p1", estado));
    }

    private void preparar() {
        mascotaEn("ACTIVA");
        when(pacientesClient.propietario("p1")).thenReturn(new PropietarioInfo("p1", "p@correo.test"));
        org.mockito.Mockito.lenient().when(horarioService.obtener("v1")).thenReturn(horario());
    }

    @Test
    void hu022_citaValidaConsumeUnCupo() {
        preparar();
        when(citaRepository.existsByClaveFranja(anyString())).thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

        Cita cita = citaService.agendar(solicitud);

        assertThat(cita.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(cita.getPropietarioCorreo()).isEqualTo("p@correo.test");
        verify(cupoService).reservar("v1", MANANA, 8);
    }

    @Test
    void hu022_franjaOcupadaNoConsumeCupo() {
        preparar();
        when(citaRepository.existsByClaveFranja("v1|" + MANANA + "|09:00")).thenReturn(true);

        assertThatThrownBy(() -> citaService.agendar(solicitud))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("La franja ya está ocupada");
        verify(cupoService, never()).reservar(anyString(), any(), anyInt());
    }

    @Test
    void hu022_sinCuposNoCreaLaCita() {
        preparar();
        doThrow(new ConflictoException("No hay cupos disponibles")).when(cupoService).reservar("v1", MANANA, 8);

        assertThatThrownBy(() -> citaService.agendar(solicitud))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("No hay cupos disponibles");
        verify(citaRepository, never()).save(any());
    }

    @Test
    void hu022_reservaSimultaneaDeLaFranjaLiberaElCupo() {
        preparar();
        when(citaRepository.save(any(Cita.class))).thenThrow(new DuplicateKeyException("duplicado"));

        assertThatThrownBy(() -> citaService.agendar(solicitud))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("La franja ya está ocupada");
        verify(cupoService).liberar("v1", MANANA);
    }

    @Test
    void hu022_mascotaNoActiva() {
        mascotaEn("FALLECIDA");

        assertThatThrownBy(() -> citaService.agendar(solicitud)).isInstanceOf(ReglaNegocioException.class);
        verify(citaRepository, never()).save(any());
    }

    @Test
    void hu022_fechaPasada() {
        preparar();

        assertThatThrownBy(() -> citaService.agendar(new CitaRequest("m1", "v1", LocalDate.now().minusDays(1), NUEVE, "Control")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la cita ya pasó");
    }

    @Test
    void hu022_pacientesNoDisponibleNoCreaLaCita() {
        when(pacientesClient.mascota("m1")).thenThrow(new ServicioNoDisponibleException("El servicio ms-pacientes no está disponible"));

        assertThatThrownBy(() -> citaService.agendar(solicitud)).isInstanceOf(ServicioNoDisponibleException.class);
        verify(citaRepository, never()).save(any());
    }

    private Cita citaEn(EstadoCita estado, LocalDate fecha) {
        Cita cita = new Cita();
        cita.setId("c1");
        cita.setRecursoId("v1");
        cita.setFecha(fecha);
        cita.setHora(NUEVE);
        cita.setEstado(estado);
        return cita;
    }

    @Test
    void hu025_cancelarLiberaCupoYFranja() {
        Cita cita = citaEn(EstadoCita.PROGRAMADA, MANANA);
        cita.setClaveFranja("clave");
        when(citaRepository.findById("c1")).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        Cita cancelada = citaService.cancelar("c1", "No puede asistir");

        assertThat(cancelada.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(cancelada.getClaveFranja()).isNull();
        verify(cupoService).liberar("v1", MANANA);
    }

    @Test
    void hu025_citaAtendidaNoSeCancelaNiReprograma() {
        when(citaRepository.findById("c1")).thenReturn(Optional.of(citaEn(EstadoCita.ATENDIDA, MANANA)));

        assertThatThrownBy(() -> citaService.cancelar("c1", "motivo")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> citaService.reprogramar("c1", new ReprogramacionRequest(MANANA, NUEVE))).isInstanceOf(ReglaNegocioException.class);
        verify(cupoService, never()).liberar(anyString(), any());
    }

    @Test
    void hu025_reprogramarSinCupoConservaLaFecha() {
        Cita cita = citaEn(EstadoCita.PROGRAMADA, MANANA);
        LocalDate destino = MANANA.plusDays(1);
        when(citaRepository.findById("c1")).thenReturn(Optional.of(cita));
        when(horarioService.obtener("v1")).thenReturn(horario());
        doThrow(new ConflictoException("No hay cupos disponibles")).when(cupoService).reservar("v1", destino, 8);

        assertThatThrownBy(() -> citaService.reprogramar("c1", new ReprogramacionRequest(destino, NUEVE))).isInstanceOf(ConflictoException.class);
        assertThat(cita.getFecha()).isEqualTo(MANANA);
        verify(citaRepository, never()).save(any());
    }

    @Test
    void hu025_reprogramarEnElMismoDiaNoTocaLosCupos() {
        Cita cita = citaEn(EstadoCita.PROGRAMADA, MANANA);
        when(citaRepository.findById("c1")).thenReturn(Optional.of(cita));
        when(horarioService.obtener("v1")).thenReturn(horario());
        when(citaRepository.save(cita)).thenReturn(cita);

        citaService.reprogramar("c1", new ReprogramacionRequest(MANANA, LocalTime.of(10, 0)));

        verify(cupoService, never()).reservar(anyString(), any(), anyInt());
        verify(cupoService, never()).liberar(anyString(), any());
    }

    @Test
    void hu026_llegadaDeHoyQuedaEnEspera() {
        Cita cita = citaEn(EstadoCita.PROGRAMADA, LocalDate.now());
        when(citaRepository.findById("c1")).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        Cita llegada = citaService.registrarLlegada("c1");

        assertThat(llegada.getEstado()).isEqualTo(EstadoCita.EN_ESPERA);
        assertThat(llegada.getHoraLlegada()).isNotNull();
    }

    @Test
    void hu026_citaDeOtroDiaOCancelada() {
        when(citaRepository.findById("c1")).thenReturn(Optional.of(citaEn(EstadoCita.PROGRAMADA, MANANA)));
        assertThatThrownBy(() -> citaService.registrarLlegada("c1")).isInstanceOf(ReglaNegocioException.class);

        when(citaRepository.findById("c2")).thenReturn(Optional.of(citaEn(EstadoCita.CANCELADA, LocalDate.now())));
        assertThatThrownBy(() -> citaService.registrarLlegada("c2")).isInstanceOf(ReglaNegocioException.class);
    }
}
