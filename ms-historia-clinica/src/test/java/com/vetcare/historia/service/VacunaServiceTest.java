package com.vetcare.historia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetcare.historia.client.FarmaciaClient;
import com.vetcare.historia.client.PacientesClient;
import com.vetcare.historia.dto.AlertaVacunaResponse;
import com.vetcare.historia.dto.MascotaInfo;
import com.vetcare.historia.dto.VacunaRequest;
import com.vetcare.historia.dto.VacunaResponse;
import com.vetcare.historia.exception.ConflictoException;
import com.vetcare.historia.exception.ReglaNegocioException;
import com.vetcare.historia.model.EsquemaVacunacion;
import com.vetcare.historia.model.Vacuna;
import com.vetcare.historia.repository.EsquemaVacunacionRepository;
import com.vetcare.historia.repository.VacunaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reglas de RN-01, sin MongoDB.
 */
@ExtendWith(MockitoExtension.class)
class VacunaServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 12);

    @Mock
    private EsquemaVacunacionRepository esquemaRepository;
    @Mock
    private VacunaRepository vacunaRepository;
    @Mock
    private PacientesClient pacientesClient;
    @Mock
    private FarmaciaClient farmaciaClient;
    @Mock
    private AuditoriaService auditoriaService;
    @InjectMocks
    private VacunaService vacunaService;

    private final VacunaRequest solicitud = new VacunaRequest("m1", "Rabia", "L-100", FECHA);

    private EsquemaVacunacion esquema() {
        EsquemaVacunacion esquema = new EsquemaVacunacion();
        esquema.setEspecie("Canino");
        esquema.setVacuna("Rabia");
        esquema.setDosis(2);
        esquema.setIntervaloDias(365);
        return esquema;
    }

    private Vacuna dosisAplicada(int dosis) {
        Vacuna vacuna = new Vacuna();
        vacuna.setDosis(dosis);
        return vacuna;
    }

    private void mascotaConEsquema() {
        when(pacientesClient.mascota("m1")).thenReturn(new MascotaInfo("m1", "Max", "Canino", "p1", List.of(), List.of()));
        when(esquemaRepository.findByEspecieAndVacuna("Canino", "Rabia")).thenReturn(Optional.of(esquema()));
    }

    @Test
    void rn01_laDosisUnoCalculaLaSiguienteConElIntervalo() {
        mascotaConEsquema();
        when(vacunaRepository.findByMascotaIdAndVacunaOrderByDosisDesc("m1", "Rabia")).thenReturn(List.of());
        when(vacunaRepository.save(any(Vacuna.class))).thenAnswer(i -> i.getArgument(0));

        VacunaResponse respuesta = vacunaService.registrar(solicitud, "v1");

        assertThat(respuesta.dosis()).isEqualTo(1);
        assertThat(respuesta.proximaDosis()).isEqualTo(LocalDate.of(2027, 11, 12));
        verify(farmaciaClient).descontarVacuna("L-100");
    }

    @Test
    void rn01_laUltimaDosisCompletaElEsquemaYAtiendeLaAlertaAnterior() {
        mascotaConEsquema();
        Vacuna anterior = dosisAplicada(1);
        when(vacunaRepository.findByMascotaIdAndVacunaOrderByDosisDesc("m1", "Rabia")).thenReturn(List.of(anterior));
        when(vacunaRepository.save(any(Vacuna.class))).thenAnswer(i -> i.getArgument(0));

        VacunaResponse respuesta = vacunaService.registrar(solicitud, "v1");

        assertThat(respuesta.dosis()).isEqualTo(2);
        assertThat(respuesta.proximaDosis()).isNull();
        assertThat(anterior.isVigente()).isFalse();
    }

    @Test
    void rn01_noRegistraMasDosisQueElEsquema() {
        mascotaConEsquema();
        when(vacunaRepository.findByMascotaIdAndVacunaOrderByDosisDesc("m1", "Rabia")).thenReturn(List.of(dosisAplicada(2)));

        assertThatThrownBy(() -> vacunaService.registrar(solicitud, "v1")).isInstanceOf(ReglaNegocioException.class);
        verify(farmaciaClient, never()).descontarVacuna(any());
    }

    @Test
    void hu037_sinEsquema() {
        when(pacientesClient.mascota("m1")).thenReturn(new MascotaInfo("m1", "Max", "Canino", "p1", List.of(), List.of()));
        when(esquemaRepository.findByEspecieAndVacuna("Canino", "Rabia")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacunaService.registrar(solicitud, "v1"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La vacuna no tiene esquema para la especie");
    }

    @Test
    void hu037_loteSinStockNoRegistraLaDosis() {
        mascotaConEsquema();
        when(vacunaRepository.findByMascotaIdAndVacunaOrderByDosisDesc("m1", "Rabia")).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new ConflictoException("Stock insuficiente")).when(farmaciaClient).descontarVacuna("L-100");

        assertThatThrownBy(() -> vacunaService.registrar(solicitud, "v1")).isInstanceOf(ConflictoException.class);
        verify(vacunaRepository, never()).save(any());
    }

    @Test
    void rn01_alertasDistinguenProximasYVencidas() {
        LocalDate hoy = LocalDate.now();
        Vacuna proxima = dosisAplicada(1);
        proxima.setProximaDosis(hoy.plusDays(10));
        Vacuna vencida = dosisAplicada(1);
        vencida.setProximaDosis(hoy.minusDays(5));
        Vacuna hoyMismo = dosisAplicada(1);
        hoyMismo.setProximaDosis(hoy);
        when(vacunaRepository.findByVigenteTrueAndProximaDosisLessThanEqualOrderByProximaDosis(hoy.plusDays(30)))
                .thenReturn(List.of(vencida, hoyMismo, proxima));

        List<AlertaVacunaResponse> alertas = vacunaService.alertas(30);

        assertThat(alertas).extracting(AlertaVacunaResponse::estado).containsExactly("VENCIDA", "PROXIMA", "PROXIMA");
    }
}
