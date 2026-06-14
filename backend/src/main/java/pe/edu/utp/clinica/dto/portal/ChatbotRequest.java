package pe.edu.utp.clinica.dto.portal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de solicitud para el chatbot de asistencia.
 * RF-55: El paciente envía un mensaje y recibe orientación
 *        sobre la clínica o la especialidad adecuada.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class ChatbotRequest {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
    private String mensaje;

    /**
     * Historial de mensajes anteriores para contexto (opcional).
     * Permite conversaciones multi-turno con el chatbot.
     * Formato: lista de {rol: "user"|"assistant", contenido: "..."}
     */
    private java.util.List<MensajeHistorial> historial;

    @Data
    public static class MensajeHistorial {
        private String rol;       // "user" o "assistant"
        private String contenido;
    }
}