package com.vetcare.historia.dto;

import java.time.Instant;

public record AdjuntoResponse(String id, String nombre, String tipo, long tamano, Instant fecha) {
}
