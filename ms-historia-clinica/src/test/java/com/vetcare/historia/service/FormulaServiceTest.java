package com.vetcare.historia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetcare.historia.client.FarmaciaClient;
import com.vetcare.historia.dto.FormulaResponse;
import com.vetcare.historia.dto.ItemDespachadoRequest;
import com.vetcare.historia.dto.VigenciaResponse;
import com.vetcare.historia.exception.ConflictoException;
import com.vetcare.historia.exception.ReglaNegocioException;
import com.vetcare.historia.model.EstadoFormula;
import com.vetcare.historia.model.Formula;
import com.vetcare.historia.model.ItemFormula;
import com.vetcare.historia.repository.FormulaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Reglas de RN-02 sobre la fórmula, sin MongoDB.
 */
@ExtendWith(MockitoExtension.class)
class FormulaServiceTest {

    @Mock
    private FormulaRepository formulaRepository;
    @Mock
    private ConsultaService consultaService;
    @Mock
    private FarmaciaClient farmaciaClient;
    @Mock
    private AuditoriaService auditoriaService;
    @InjectMocks
    private FormulaService formulaService;

    private Formula formula(EstadoFormula estado, LocalDate vigenteHasta, int pendiente) {
        ItemFormula item = new ItemFormula();
        item.setMedicamentoId("med1");
        item.setMedicamento("Metronidazol");
        item.setCantidad(14);
        item.setPendiente(pendiente);
        Formula formula = new Formula();
        formula.setId("F-010");
        formula.setEstado(estado);
        formula.setVigenteHasta(vigenteHasta);
        formula.setItems(List.of(item));
        return formula;
    }

    private final LocalDate manana = LocalDate.now().plusDays(1);

    private void existe(Formula formula) {
        when(formulaRepository.findById("F-010")).thenReturn(Optional.of(formula));
    }

    @Test
    void rn02_vigente() {
        existe(formula(EstadoFormula.VIGENTE, manana, 14));

        VigenciaResponse vigencia = formulaService.vigencia("F-010");

        assertThat(vigencia.despachable()).isTrue();
        assertThat(vigencia.items().get(0).pendiente()).isEqualTo(14);
    }

    @Test
    void rn02_elUltimoDiaDeVigenciaSigueVigente() {
        existe(formula(EstadoFormula.VIGENTE, LocalDate.now(), 14));

        assertThat(formulaService.vigencia("F-010").despachable()).isTrue();
    }

    @Test
    void rn02_vencidaAunqueGuardeElEstadoVigente() {
        existe(formula(EstadoFormula.VIGENTE, LocalDate.now().minusDays(1), 14));

        VigenciaResponse vigencia = formulaService.vigencia("F-010");

        assertThat(vigencia.estado()).isEqualTo(EstadoFormula.VENCIDA);
        assertThat(vigencia.despachable()).isFalse();
    }

    @Test
    void rn02_anuladaYDespachadaNoSonDespachables() {
        existe(formula(EstadoFormula.ANULADA, manana, 14));
        assertThat(formulaService.vigencia("F-010").despachable()).isFalse();

        existe(formula(EstadoFormula.DESPACHADA, manana, 0));
        assertThat(formulaService.vigencia("F-010").despachable()).isFalse();
    }

    @Test
    void rn02_despachoParcialDejaPendientes() {
        Formula formula = formula(EstadoFormula.VIGENTE, manana, 14);
        existe(formula);
        when(formulaRepository.save(formula)).thenReturn(formula);

        FormulaResponse respuesta = formulaService.registrarDespacho("F-010", List.of(new ItemDespachadoRequest("med1", 6)));

        assertThat(respuesta.estado()).isEqualTo(EstadoFormula.PARCIAL);
        assertThat(respuesta.items().get(0).getPendiente()).isEqualTo(8);
    }

    @Test
    void rn02_despachoTotalCierraLaFormula() {
        Formula formula = formula(EstadoFormula.PARCIAL, manana, 8);
        existe(formula);
        when(formulaRepository.save(formula)).thenReturn(formula);

        assertThat(formulaService.registrarDespacho("F-010", List.of(new ItemDespachadoRequest("med1", 8))).estado())
                .isEqualTo(EstadoFormula.DESPACHADA);
    }

    @Test
    void rn02_noDescuentaMasDeLoPendiente() {
        existe(formula(EstadoFormula.VIGENTE, manana, 14));

        assertThatThrownBy(() -> formulaService.registrarDespacho("F-010", List.of(new ItemDespachadoRequest("med1", 20))))
                .isInstanceOf(ReglaNegocioException.class);
        verify(formulaRepository, never()).save(any());
    }

    @Test
    void rn02_formulaVencidaNoRegistraDespacho() {
        existe(formula(EstadoFormula.VIGENTE, LocalDate.now().minusDays(1), 14));

        assertThatThrownBy(() -> formulaService.registrarDespacho("F-010", List.of(new ItemDespachadoRequest("med1", 1))))
                .hasMessage("No se puede despachar sin fórmula vigente");
    }

    @Test
    void rn02_despachosSimultaneosRechazanAlSegundo() {
        Formula formula = formula(EstadoFormula.VIGENTE, manana, 14);
        existe(formula);
        when(formulaRepository.save(formula)).thenThrow(new OptimisticLockingFailureException("versión"));

        assertThatThrownBy(() -> formulaService.registrarDespacho("F-010", List.of(new ItemDespachadoRequest("med1", 1))))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void hu045_anulacionYRechazoDeLaYaDespachada() {
        Formula vigente = formula(EstadoFormula.VIGENTE, manana, 14);
        existe(vigente);
        when(formulaRepository.save(vigente)).thenReturn(vigente);
        assertThat(formulaService.anular("F-010", "Cambio de tratamiento").estado()).isEqualTo(EstadoFormula.ANULADA);

        existe(formula(EstadoFormula.DESPACHADA, manana, 0));
        assertThatThrownBy(() -> formulaService.anular("F-010", "Tarde")).isInstanceOf(ReglaNegocioException.class);
    }
}
