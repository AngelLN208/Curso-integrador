package pe.edu.utp.clinica.dto.portal;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO para que el paciente agende su propia cita desde el portal.
 * No incluye pacienteId porque se toma del usuario autenticado,
 * evitando que un paciente agende citas a nombre de otro.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class CitaPortalRequest {

    @NotNull(message = "El médico es obligatorio")
    private Long medicoId;

    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "La fecha de la cita debe ser futura")
    private LocalDateTime fechaHora;

    private String motivo;
}