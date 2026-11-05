package com.vetcare.agenda.dto;

import java.time.LocalTime;
import java.util.List;

public record DisponibilidadResponse(String recursoId, String nombre, int cuposRestantes, List<LocalTime> franjasLibres) {
}
