package pe.edu.utp.clinica.dto.cita;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO para registrar una cita médica.
 * RF-05: Paciente, médico, fecha y hora.
 * RF-13: Motivo opcional.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class CitaRequest {

    @NotNull(message = "El paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El médico es obligatorio")
    private Long medicoId;

    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "La fecha de la cita debe ser futura")
    private LocalDateTime fechaHora;

    /** Opcional (RF-13) */
    private String motivo;

    /** ID del seguro a aplicar, si corresponde (RF-16) */
    private Long seguroId;
}