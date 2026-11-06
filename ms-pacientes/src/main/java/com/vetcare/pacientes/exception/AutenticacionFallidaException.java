package com.vetcare.pacientes.exception;

public class AutenticacionFallidaException extends RuntimeException {

    public AutenticacionFallidaException() {
        super("Correo o contraseña incorrectos");
    }
}
