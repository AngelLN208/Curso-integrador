package pe.edu.utp.clinica.dto.horario;

import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DTO para la respuesta con datos del horario del médico.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class HorarioResponse {

    private Long id;
    private Long medicoId;
    private String medicoNombre;
    private DayOfWeek dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}