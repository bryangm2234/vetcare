package com.vetcare.pacientes.model;

import java.time.Instant;

/**
 * Registro de un cambio de dueño (HU-018).
 */
public record TransferenciaMascota(String propietarioAnteriorId, String propietarioNuevoId, Instant fecha) {
}
