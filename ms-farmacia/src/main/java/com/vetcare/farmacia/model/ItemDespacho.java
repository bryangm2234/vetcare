package com.vetcare.farmacia.model;

/**
 * Medicamento entregado en un despacho, con el precio vigente en ese momento.
 */
public record ItemDespacho(String medicamentoId, String nombre, int cantidad, long precioUnitario) {
}
