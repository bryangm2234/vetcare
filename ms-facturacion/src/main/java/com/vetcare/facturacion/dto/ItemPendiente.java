package com.vetcare.facturacion.dto;

/**
 * Ítem pendiente de facturar que devuelve cada microservicio. Farmacia informa su valor; los demás, su cantidad.
 */
public record ItemPendiente(String id, String tipo, String descripcion, String mascotaId, Integer cantidad, Long valor) {
}
