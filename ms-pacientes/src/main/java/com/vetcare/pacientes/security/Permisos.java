package com.vetcare.pacientes.security;

/**
 * Expresiones de autorización compartidas por los controladores.
 */
public final class Permisos {

    public static final String ADMINISTRADOR = "hasRole('ADMINISTRADOR')";
    public static final String RECEPCIONISTA = "hasRole('RECEPCIONISTA')";
    public static final String VETERINARIO = "hasRole('VETERINARIO')";
    public static final String CONSULTA_PROPIETARIOS =
            "hasAnyRole('RECEPCIONISTA','ADMINISTRADOR','VETERINARIO','AUXILIAR_VETERINARIO','PELUQUERIA','CAJERO')";
    public static final String CONSULTA_MASCOTA =
            "hasAnyRole('RECEPCIONISTA','ADMINISTRADOR','VETERINARIO','AUXILIAR_VETERINARIO','LABORATORIO','FARMACIA','PELUQUERIA','CAJERO','PROPIETARIO')";

    private Permisos() {
    }
}
