package pe.edu.utp.clinica.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones del sistema.
 *
 * RNF-12: Ninguna excepción no controlada llega al cliente.
 * Todos los errores pasan por este handler.
 * RNF-04: Los mensajes de error son genéricos (no exponen datos sensibles).
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validación de campos (Bean Validation).
     * Ejemplo: DNI con menos de 8 dígitos, campos vacíos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Error de validación en request: {}", errores);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Error de validación en los datos enviados", 400));
    }

    /**
     * Maneja errores de validación en parámetros de ruta o query (Bean
     * Validation fuera del body, ej. @Min, @Pattern en @PathVariable).
     * RNF-12: antes caía en el catch-all genérico y devolvía 500 en vez
     * de 400 — corregido con handler específico.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex) {
        log.warn("Violación de restricción de validación: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Uno o más parámetros enviados no son válidos", 400));
    }

    /**
     * Maneja violaciones de restricciones de la base de datos.
     * Ejemplo: DNI o correo duplicado (constraint UNIQUE), llave foránea
     * inexistente. RNF-12: antes cala en el catch-all genérico y devolvía
     * 500; RNF-04: el mensaje al cliente no expone el detalle SQL/constraint.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "El dato enviado ya existe o entra en conflicto con un registro existente",
                        409));
    }

    /**
     * Maneja reglas de negocio violadas (lanzadas desde los servicios).
     * Ejemplo: médico sin disponibilidad, cita ya cancelada.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(IllegalStateException ex) {
        log.warn("Regla de negocio violada: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ex.getMessage(), 400));
    }

    /**
     * Maneja recursos no encontrados.
     * Ejemplo: paciente con DNI inexistente.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(IllegalArgumentException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), 404));
    }

    /**
     * Maneja acceso denegado por rol insuficiente (RNF-03).
     * El usuario recibe HTTP 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permisos para realizar esta acción", 403));
    }

    /**
     * Maneja credenciales inválidas en el login (RNF-02).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Intento de autenticación fallido");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Credenciales inválidas", 401));
    }

    /**
     * Captura cualquier excepción no controlada.
     * RNF-04: El mensaje al cliente es genérico, el detalle va solo al log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Error interno no controlado: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error interno del servidor. Contacte al administrador.", 500));
    }
}