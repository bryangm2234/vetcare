package com.vetcare.agenda.dto;

/**
 * Datos de la mascota que devuelve ms-pacientes.
 */
public record MascotaInfo(String id, String nombre, String propietarioId, String estado) {
}
