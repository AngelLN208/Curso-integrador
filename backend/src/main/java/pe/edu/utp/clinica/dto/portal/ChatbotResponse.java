package pe.edu.utp.clinica.dto.portal;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta del chatbot de asistencia.
 * RF-55: Respuesta generada por la IA con orientación al paciente.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class ChatbotResponse {

    /** Respuesta generada por el modelo de IA */
    private String respuesta;

    /**
     * Especialidad sugerida si el paciente describió síntomas.
     * Puede ser null si la pregunta no implica orientación médica.
     */
    private String especialidadSugerida;

    /**
     * JSON crudo con los datos de una cita propuesta por el chatbot,
     * pendiente de confirmación explícita del paciente. Null si no
     * hay ninguna propuesta de cita en este mensaje.
     */
    private String citaPropuesta;

    /** Indica si la respuesta fue generada por IA o es un fallback */
    private boolean generadoPorIA;
}