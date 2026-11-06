package com.vetcare.pacientes.dto;

import com.vetcare.pacientes.model.EstadoUsuario;
import com.vetcare.pacientes.model.Usuario;
import com.vetcare.pacientes.security.Rol;

public record UsuarioResponse(String id, String nombre, String correo, Rol rol, EstadoUsuario estado,
                              String propietarioId, String proveedorId) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol(),
                usuario.getEstado(), usuario.getPropietarioId(), usuario.getProveedorId());
    }
}
