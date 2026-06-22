package pe.edu.utp.clinica.dto.atencion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para editar una consulta médica ya registrada.
 * Solo permitido dentro de la ventana de 45 min desde la cita.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class ConsultaEditarRequest {

    @NotBlank(message = "El diagnóstico es obligatorio")
    private String diagnostico;

    @NotBlank(message = "El tratamiento es obligatorio")
    private String tratamiento;

    private String observaciones;
}