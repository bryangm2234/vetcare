package com.vetcare.historia.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ArchivoExceptionHandler {

    @ExceptionHandler(ArchivoNoAdmitidoException.class)
    public ResponseEntity<ApiError> archivoNoAdmitido(ArchivoNoAdmitidoException ex, HttpServletRequest request) {
        ApiError error = ApiError.de(ex.getEstado(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(error.status()).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> archivoGrande(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        ApiError error = ApiError.de(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera los 5 MB", request.getRequestURI());
        return ResponseEntity.status(error.status()).body(error);
    }
}
