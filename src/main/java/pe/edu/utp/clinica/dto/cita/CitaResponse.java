package pe.edu.utp.clinica.dto.cita;

import lombok.Builder;
import lombok.Data;
import pe.edu.utp.clinica.common.enums.EstadoCita;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta con datos de una cita médica.
 * RF-08: ID, fecha, hora y estado actual.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class CitaResponse {

    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteDni;
    private Long medicoId;
    private String medicoNombre;
    private String especialidad;
    private LocalDateTime fechaHora;
    private String motivo;
    private EstadoCita estado;
    private LocalDateTime creadoEn;
}