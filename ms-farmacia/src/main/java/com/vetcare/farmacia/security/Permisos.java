package com.vetcare.farmacia.security;

/**
 * Expresiones de autorización compartidas por los controladores.
 */
public final class Permisos {

    public static final String FARMACIA = "hasRole('FARMACIA')";
    public static final String ADMINISTRADOR = "hasRole('ADMINISTRADOR')";
    public static final String PROVEEDOR = "hasRole('PROVEEDOR')";
    public static final String VETERINARIO = "hasRole('VETERINARIO')";
    public static final String CAJERO = "hasAnyRole('CAJERO','ADMINISTRADOR')";
    public static final String CONSULTA_CATALOGO = "hasAnyRole('FARMACIA','VETERINARIO','ADMINISTRADOR')";
    public static final String CONSULTA_PROVEEDORES = "hasAnyRole('FARMACIA','ADMINISTRADOR')";

    private Permisos() {
    }
}
