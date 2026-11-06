package com.vetcare.historia.dto;

import java.time.LocalDate;

public record AlertaVacunaResponse(String mascotaId, String mascotaNombre, String propietarioId, String vacuna, int dosisAplicada,
                                   LocalDate proximaDosis, String estado) {
}
