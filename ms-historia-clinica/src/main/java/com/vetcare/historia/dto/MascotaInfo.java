package com.vetcare.historia.dto;

import java.util.List;

/**
 * Datos de la mascota que devuelve ms-pacientes.
 */
public record MascotaInfo(String id, String nombre, String especie, String propietarioId, List<String> alergias,
                          List<String> condicionesCronicas) {
}
