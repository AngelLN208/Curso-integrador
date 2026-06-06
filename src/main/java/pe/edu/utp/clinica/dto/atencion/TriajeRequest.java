package pe.edu.utp.clinica.dto.atencion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para registrar el triaje del paciente.
 * RF-22: Presión, temperatura y peso.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class TriajeRequest {

    @NotNull(message = "El ID de la cita es obligatorio")
    private Long citaId;

    private String presionArterial;

    @Positive(message = "La temperatura debe ser mayor a cero")
    private BigDecimal temperatura;

    @Positive(message = "El peso debe ser mayor a cero")
    private BigDecimal peso;
}