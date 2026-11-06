package com.vetcare.pacientes.dto;

import com.vetcare.pacientes.model.EstadoMascota;
import com.vetcare.pacientes.model.Mascota;
import com.vetcare.pacientes.model.SexoMascota;
import com.vetcare.pacientes.model.TransferenciaMascota;
import java.time.LocalDate;
import java.util.List;

public record MascotaResponse(String id, String nombre, String especie, String raza, SexoMascota sexo,
                              LocalDate fechaNacimiento, double pesoKg, String propietarioId, EstadoMascota estado,
                              LocalDate fechaFallecimiento, List<String> alergias, List<String> condicionesCronicas,
                              List<TransferenciaMascota> transferencias) {

    public static MascotaResponse de(Mascota mascota) {
        return new MascotaResponse(mascota.getId(), mascota.getNombre(), mascota.getEspecie(), mascota.getRaza(),
                mascota.getSexo(), mascota.getFechaNacimiento(), mascota.getPesoKg(), mascota.getPropietarioId(),
                mascota.getEstado(), mascota.getFechaFallecimiento(), mascota.getAlergias(),
                mascota.getCondicionesCronicas(), mascota.getTransferencias());
    }
}
