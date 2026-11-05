package com.vetcare.facturacion.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServicioExternoExceptionHandler {

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<ApiError> servicioExterno(ServicioExternoException ex, HttpServletRequest request) {
        ApiError error = ApiError.de(HttpStatus.BAD_GATEWAY, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(error.status()).body(error);
    }
}
