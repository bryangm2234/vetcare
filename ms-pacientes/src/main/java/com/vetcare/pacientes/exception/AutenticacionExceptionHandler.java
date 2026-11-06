package com.vetcare.pacientes.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AutenticacionExceptionHandler {

    @ExceptionHandler(AutenticacionFallidaException.class)
    public ResponseEntity<ApiError> autenticacionFallida(AutenticacionFallidaException ex, HttpServletRequest request) {
        ApiError error = ApiError.de(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(error.status()).body(error);
    }
}
