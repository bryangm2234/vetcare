package com.vetcare.historia.dto;

import com.vetcare.historia.model.Vacuna;
import java.time.LocalDate;

public record VacunaResponse(String id, String mascotaId, String vacuna, int dosis, String lote, LocalDate fecha,
                             LocalDate proximaDosis) {

    public static VacunaResponse de(Vacuna vacuna) {
        return new VacunaResponse(vacuna.getId(), vacuna.getMascotaId(), vacuna.getVacuna(), vacuna.getDosis(), vacuna.getLote(),
                vacuna.getFecha(), vacuna.getProximaDosis());
    }
}
