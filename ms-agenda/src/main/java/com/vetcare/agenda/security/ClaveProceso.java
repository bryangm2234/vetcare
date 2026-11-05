package com.vetcare.agenda.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Autentica la ejecución diaria externa de los recordatorios (D12).
 */
@Component
public class ClaveProceso {

    private final byte[] clave;

    public ClaveProceso(@Value("${vetcare.clave-proceso:}") String clave) {
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
    }

    public void validar(String recibida) {
        boolean valida = clave.length > 0 && recibida != null
                && MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8));
        if (!valida) {
            throw new AccessDeniedException("Clave de proceso no válida");
        }
    }
}
