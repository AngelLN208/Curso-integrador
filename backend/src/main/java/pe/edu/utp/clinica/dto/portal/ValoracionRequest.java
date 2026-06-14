package pe.edu.utp.clinica.dto.portal;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de solicitud para calificar a un médico post-consulta.
 * RF-54: El paciente califica al médico (1-5 estrellas + comentario)
 *        después de que su cita queda en estado ATENDIDA.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class ValoracionRequest {

    @NotNull(message = "El ID de la cita es obligatorio")
    private Long citaId;

    /**
     * Puntuación del 1 al 5.
     * RF-54: Escala de valoración de la atención recibida.
     */
    @NotNull(message = "La puntuación es obligatoria")
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    private Integer puntuacion;

    /** Comentario opcional sobre la atención */
    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    private String comentario;
}