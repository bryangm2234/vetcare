package com.vetcare.facturacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetcare.facturacion.dto.AjustesFacturaRequest;
import com.vetcare.facturacion.exception.DatosInvalidosException;
import org.junit.jupiter.api.Test;

class CalculosFacturaTest {

    @Test
    void hu063_elTotalDelEjemploDelSdd() {
        long descuento = FacturaService.descuento(100000, new AjustesFacturaRequest(10, null));
        long impuesto = FacturaService.impuesto(100000, descuento, 19);

        assertThat(descuento).isEqualTo(10000);
        assertThat(impuesto).isEqualTo(17100);
        assertThat(100000 - descuento + impuesto).isEqualTo(107100);
    }

    @Test
    void hu063_elValorTienePrioridadSobreElPorcentaje() {
        assertThat(FacturaService.descuento(100000, new AjustesFacturaRequest(10, 5000L))).isEqualTo(5000);
    }

    @Test
    void hu063_sinDescuentoIndicado() {
        assertThatThrownBy(() -> FacturaService.descuento(100000, new AjustesFacturaRequest(null, null)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void hu069_repartoProporcionalDelPago() {
        assertThat(ReporteIngresosService.parteDe(100, 30, 100)).isEqualTo(30);
        assertThat(ReporteIngresosService.parteDe(100, 30, 0)).isZero();
    }
}
