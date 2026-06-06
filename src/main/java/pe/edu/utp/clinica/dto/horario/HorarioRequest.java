package pe.edu.utp.clinica.dto.horario;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DTO para asignar un horario a un médico.
 * RF-38: Día de la semana, hora de inicio y hora de fin.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class HorarioRequest {

    @NotNull(message = "El médico es obligatorio")
    private Long medicoId;

    @NotNull(message = "El día es obligatorio")
    private DayOfWeek dia;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;
}