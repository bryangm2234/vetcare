package com.vetcare.farmacia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetcare.farmacia.client.HistoriaClient;
import com.vetcare.farmacia.dto.DespachoRequest;
import com.vetcare.farmacia.dto.FormulaVigencia;
import com.vetcare.farmacia.dto.ItemDespachoRequest;
import com.vetcare.farmacia.exception.ConflictoException;
import com.vetcare.farmacia.exception.DatosInvalidosException;
import com.vetcare.farmacia.exception.ReglaNegocioException;
import com.vetcare.farmacia.exception.ServicioNoDisponibleException;
import com.vetcare.farmacia.model.Despacho;
import com.vetcare.farmacia.model.Medicamento;
import com.vetcare.farmacia.model.TipoMovimiento;
import com.vetcare.farmacia.repository.DespachoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reglas de RN-02 en el despacho, sin MongoDB.
 */
@ExtendWith(MockitoExtension.class)
class DespachoServiceTest {

    @Mock
    private HistoriaClient historiaClient;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private MedicamentoService medicamentoService;
    @Mock
    private DespachoRepository despachoRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @InjectMocks
    private DespachoService despachoService;

    private DespachoRequest solicitud(int cantidad) {
        return new DespachoRequest("F-010", List.of(new ItemDespachoRequest("med1", cantidad)));
    }

    private FormulaVigencia formula(String estado, boolean despachable, int pendiente) {
        return new FormulaVigencia("F-010", "m1", estado, despachable, List.of(new FormulaVigencia.ItemPendiente("med1", "Metronidazol", pendiente)));
    }

    @Test
    void rn02_formulaVencidaNoSeDespacha() {
        when(historiaClient.vigencia("F-010")).thenReturn(formula("VENCIDA", false, 14));

        assertThatThrownBy(() -> despachoService.despachar(solicitud(14), "farmacia"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede despachar sin fórmula vigente");
        verify(inventarioService, never()).descontarFefo(anyString(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void rn02_formulaAnuladaNoSeDespacha() {
        when(historiaClient.vigencia("F-010")).thenReturn(formula("ANULADA", false, 14));

        assertThatThrownBy(() -> despachoService.despachar(solicitud(14), "farmacia"))
                .hasMessage("No se puede despachar sin fórmula vigente");
    }

    @Test
    void rn02_noDespachaMasDeLoFormulado() {
        when(historiaClient.vigencia("F-010")).thenReturn(formula("VIGENTE", true, 14));

        assertThatThrownBy(() -> despachoService.despachar(solicitud(20), "farmacia"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(inventarioService, never()).descontarFefo(anyString(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void rn02_medicamentoFueraDeLaFormula() {
        when(historiaClient.vigencia("F-010")).thenReturn(formula("VIGENTE", true, 14));

        assertThatThrownBy(() -> despachoService.despachar(new DespachoRequest("F-010", List.of(new ItemDespachoRequest("otro", 1))), "farmacia"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El medicamento no está en la fórmula");
    }

    @Test
    void hu053_medicamentoRepetidoEnLaSolicitud() {
        assertThatThrownBy(() -> despachoService.despachar(new DespachoRequest("F-010",
                List.of(new ItemDespachoRequest("med1", 1), new ItemDespachoRequest("med1", 1))), "farmacia"))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void rn04_stockInsuficienteNoModificaNada() {
        when(historiaClient.vigencia("F-010")).thenReturn(formula("VIGENTE", true, 14));
        when(inventarioService.stockDisponible("med1")).thenReturn(10L);

        assertThatThrownBy(() -> despachoService.despachar(solicitud(14), "farmacia"))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Stock insuficiente");
        verify(inventarioService, never()).descontarFefo(anyString(), anyInt(), any(), anyString(), anyString());
        verify(historiaClient, never()).registrarDespacho(anyString(), any());
    }

    @Test
    void hu053_historiaCaidaAlConsultarNoTocaElInventario() {
        when(historiaClient.vigencia("F-010")).thenThrow(new ServicioNoDisponibleException("El servicio ms-historia-clinica no está disponible"));

        assertThatThrownBy(() -> despachoService.despachar(solicitud(14), "farmacia")).isInstanceOf(ServicioNoDisponibleException.class);
        verify(inventarioService, never()).descontarFefo(anyString(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void hu053_siFallaActualizarLaFormulaSeRevierteElStock() {
        List<InventarioService.Consumo> consumos = List.of(new InventarioService.Consumo("med1", "lote1", 14));
        when(historiaClient.vigencia("F-010")).thenReturn(formula("VIGENTE", true, 14));
        when(inventarioService.stockDisponible("med1")).thenReturn(50L);
        when(inventarioService.descontarFefo(eq("med1"), eq(14), eq(TipoMovimiento.DESPACHO), anyString(), anyString())).thenReturn(consumos);
        doThrow(new ServicioNoDisponibleException("El servicio ms-historia-clinica no está disponible"))
                .when(historiaClient).registrarDespacho(anyString(), any());

        assertThatThrownBy(() -> despachoService.despachar(solicitud(14), "farmacia")).isInstanceOf(ServicioNoDisponibleException.class);

        verify(inventarioService).revertir(eq(consumos), anyString(), anyString());
        verify(despachoRepository, never()).save(any());
    }

    @Test
    void hu053_despachoValidoGuardaElPrecioVigente() {
        Medicamento medicamento = new Medicamento();
        medicamento.setNombre("Metronidazol");
        medicamento.setPrecio(1500);
        when(historiaClient.vigencia("F-010")).thenReturn(formula("VIGENTE", true, 14));
        when(inventarioService.stockDisponible("med1")).thenReturn(50L);
        when(medicamentoService.buscar("med1")).thenReturn(medicamento);
        when(despachoRepository.save(any(Despacho.class))).thenAnswer(i -> i.getArgument(0));

        Despacho despacho = despachoService.despachar(solicitud(14), "farmacia");

        assertThat(despacho.getItems().get(0).precioUnitario()).isEqualTo(1500);
        assertThat(despacho.getMascotaId()).isEqualTo("m1");
        verify(historiaClient).registrarDespacho("F-010", solicitud(14).items());
    }
}
