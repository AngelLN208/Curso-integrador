package pe.edu.utp.clinica.dto.portal;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una valoración registrada.
 * RF-54: Confirmación de la calificación enviada por el paciente.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class ValoracionResponse {

    private Long          id;
    private Long          citaId;
    private String        medicoNombre;
    private String        especialidad;
    private Integer       puntuacion;
    private String        comentario;
    private LocalDateTime registradoEn;
}