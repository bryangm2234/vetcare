package com.vetcare.farmacia.dto;

import java.time.LocalDate;

public record ItemPendienteResponse(String id, String tipo, String descripcion, String mascotaId, LocalDate fecha, long valor) {
}
