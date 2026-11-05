package com.vetcare.facturacion.model;

/**
 * Concepto facturado, con el precio vigente al generar la factura.
 */
public record ItemFactura(String origenId, TipoServicio tipo, String descripcion, int cantidad, long precioUnitario, long subtotal) {
}
