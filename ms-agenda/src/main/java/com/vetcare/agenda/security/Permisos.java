package com.vetcare.agenda.security;

/**
 * Expresiones de autorización compartidas por los controladores.
 */
public final class Permisos {

    public static final String ADMINISTRADOR = "hasRole('ADMINISTRADOR')";
    public static final String RECEPCIONISTA = "hasRole('RECEPCIONISTA')";
    public static final String VETERINARIO = "hasRole('VETERINARIO')";
    public static final String PELUQUERIA = "hasRole('PELUQUERIA')";
    public static final String PROPIETARIO = "hasRole('PROPIETARIO')";
    public static final String CAJERO = "hasAnyRole('CAJERO','ADMINISTRADOR')";
    public static final String CONSULTA_DISPONIBILIDAD = "hasAnyRole('RECEPCIONISTA','ADMINISTRADOR','PROPIETARIO','PELUQUERIA')";
    public static final String CONSULTA_CITA = "hasAnyRole('RECEPCIONISTA','VETERINARIO','AUXILIAR_VETERINARIO','ADMINISTRADOR')";

    private Permisos() {
    }
}
