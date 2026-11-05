package com.vetcare.facturacion.security;

/**
 * Expresiones de autorización compartidas por los controladores.
 */
public final class Permisos {

    public static final String CAJERO = "hasRole('CAJERO')";
    public static final String ADMINISTRADOR = "hasRole('ADMINISTRADOR')";
    public static final String PROPIETARIO = "hasRole('PROPIETARIO')";
    public static final String CONSULTA_TARIFAS = "hasAnyRole('CAJERO','ADMINISTRADOR')";

    private Permisos() {
    }
}
