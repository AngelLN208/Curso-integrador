package pe.edu.utp.clinica.dto.cita;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO para reprogramar una cita médica.
 * RF-06: Solo se actualiza la fecha y hora.
 *        El estado cambia automáticamente a REPROGRAMADA.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class CitaReprogramarRequest {

    @NotNull(message = "La nueva fecha y hora son obligatorias")
    @Future(message = "La nueva fecha debe ser futura")
    private LocalDateTime nuevaFechaHora;
}