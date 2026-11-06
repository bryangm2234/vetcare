package com.vetcare.historia.exception;

import org.springframework.http.HttpStatus;

public class ArchivoNoAdmitidoException extends RuntimeException {

    private final HttpStatus estado;

    public ArchivoNoAdmitidoException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
