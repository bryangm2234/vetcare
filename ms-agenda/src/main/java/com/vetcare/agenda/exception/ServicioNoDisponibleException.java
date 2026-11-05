package com.vetcare.agenda.exception;

public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
