package com.vetcare.historia.security;

/**
 * Expresiones de autorización compartidas por los controladores.
 */
public final class Permisos {

    public static final String VETERINARIO = "hasRole('VETERINARIO')";
    public static final String AUXILIAR = "hasRole('AUXILIAR_VETERINARIO')";
    public static final String RECEPCIONISTA = "hasRole('RECEPCIONISTA')";
    public static final String PROPIETARIO = "hasRole('PROPIETARIO')";
    public static final String CAJERO = "hasAnyRole('CAJERO','ADMINISTRADOR')";
    public static final String CONSULTA_ESQUEMAS = "hasAnyRole('VETERINARIO','RECEPCIONISTA','AUXILIAR_VETERINARIO')";

    private Permisos() {
    }
}
