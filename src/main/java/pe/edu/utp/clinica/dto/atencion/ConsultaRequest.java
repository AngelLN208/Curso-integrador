package pe.edu.utp.clinica.dto.atencion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para registrar la consulta médica.
 * RF-23: Diagnóstico y tratamiento obligatorios.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class ConsultaRequest {

    @NotNull(message = "El ID de la cita es obligatorio")
    private Long citaId;

    @NotBlank(message = "El diagnóstico es obligatorio")
    private String diagnostico;

    @NotBlank(message = "El tratamiento es obligatorio")
    private String tratamiento;

    private String observaciones;
}