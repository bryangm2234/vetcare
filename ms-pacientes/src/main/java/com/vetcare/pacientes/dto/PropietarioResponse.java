package com.vetcare.pacientes.dto;

import com.vetcare.pacientes.model.Propietario;

public record PropietarioResponse(String id, String documento, String nombre, String telefono, String correo,
                                  String direccion) {

    public static PropietarioResponse de(Propietario propietario) {
        return new PropietarioResponse(propietario.getId(), propietario.getDocumento(), propietario.getNombre(),
                propietario.getTelefono(), propietario.getCorreo(), propietario.getDireccion());
    }
}
