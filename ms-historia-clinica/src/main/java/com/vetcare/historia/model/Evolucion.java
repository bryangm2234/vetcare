package com.vetcare.historia.model;

import java.time.Instant;
import java.util.List;

/**
 * Seguimiento de un paciente hospitalizado (HU-048).
 */
public record Evolucion(String observaciones, Double temperatura, List<String> medicamentosAdministrados, String responsable, Instant fecha) {
}
