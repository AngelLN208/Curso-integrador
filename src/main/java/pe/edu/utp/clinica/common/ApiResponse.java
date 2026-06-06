package pe.edu.utp.clinica.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase genérica para respuestas uniformes de la API.
 *
 * RNF-09: Todas las respuestas incluyen los campos:
 *         success, data, message y status.
 *
 * Ejemplo de uso:
 *   return ResponseEntity.ok(ApiResponse.success("Paciente registrado", pacienteDto));
 *
 * @param <T> Tipo del objeto retornado en el campo data
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** Indica si la operación fue exitosa */
    private boolean success;

    /** Mensaje descriptivo del resultado */
    private String message;

    /** Código HTTP de la respuesta */
    private int status;

    /** Datos retornados (puede ser null en errores) */
    private T data;

    // ─── Métodos de fábrica ───────────────────────────────────────────

    /**
     * Respuesta exitosa con datos.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .status(200)
                .data(data)
                .build();
    }

    /**
     * Respuesta exitosa sin datos (ej: eliminaciones).
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .status(200)
                .data(null)
                .build();
    }

    /**
     * Respuesta de creación exitosa (HTTP 201).
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .status(201)
                .data(data)
                .build();
    }

    /**
     * Respuesta de error con código personalizado.
     */
    public static <T> ApiResponse<T> error(String message, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .status(status)
                .data(null)
                .build();
    }
}