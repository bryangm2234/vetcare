package com.vetcare.farmacia.dto;

import com.vetcare.farmacia.model.Medicamento;
import com.vetcare.farmacia.model.TipoProducto;

public record MedicamentoResponse(String id, String codigo, String nombre, TipoProducto tipo, String presentacion, long precio,
                                  int stockMinimo, long stockDisponible) {

    public static MedicamentoResponse de(Medicamento medicamento, long stockDisponible) {
        return new MedicamentoResponse(medicamento.getId(), medicamento.getCodigo(), medicamento.getNombre(), medicamento.getTipo(),
                medicamento.getPresentacion(), medicamento.getPrecio(), medicamento.getStockMinimo(), stockDisponible);
    }
}
