package com.vetcare.pacientes.exception;

import com.mongodb.MongoTimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Convierte toda excepción en una respuesta JSON estándar (HU-079).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> detalles.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return responder(ApiError.de(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos",
                request.getRequestURI(), detalles));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> solicitudInvalida(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "La solicitud tiene un formato o un valor no válido", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación", request);
    }

    @ExceptionHandler({RecursoNoEncontradoException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> noEncontrado(Exception ex, HttpServletRequest request) {
        String mensaje = ex instanceof RecursoNoEncontradoException ? ex.getMessage() : "El recurso no existe";
        return error(HttpStatus.NOT_FOUND, mensaje, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodoNoPermitido(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "El método no está permitido en esta ruta", request);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiError> conflicto(ConflictoException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiError> duplicado(DuplicateKeyException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "Ya existe un registro con esos datos", request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ApiError> servicioNoDisponible(ServicioNoDisponibleException ex, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, MongoTimeoutException.class})
    public ResponseEntity<ApiError> baseDeDatosNoDisponible(Exception ex, HttpServletRequest request) {
        log.error("MongoDB no está disponible en {}", request.getRequestURI(), ex);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "El servicio no está disponible temporalmente", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus estado, String mensaje, HttpServletRequest request) {
        return responder(ApiError.de(estado, mensaje, request.getRequestURI()));
    }

    private ResponseEntity<ApiError> responder(ApiError error) {
        return ResponseEntity.status(error.status()).body(error);
    }
}
