package com.vetcare.agenda.dto;

public record OcupacionResponse(String veterinarioId, String nombre, long cupos, long citas, long porcentaje) {
}
