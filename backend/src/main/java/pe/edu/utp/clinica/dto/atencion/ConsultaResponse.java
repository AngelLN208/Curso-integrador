package pe.edu.utp.clinica.dto.atencion;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta con datos de la consulta médica.
 * RF-26: Historial con fecha, diagnóstico y tratamiento.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class ConsultaResponse {

    private Long id;
    private Long citaId;
    private String pacienteNombre;
    private String medicoNombre;
    private LocalDateTime fechaCita;
    private String diagnostico;
    private String tratamiento;
    private String observaciones;
    private LocalDateTime registradoEn;
}