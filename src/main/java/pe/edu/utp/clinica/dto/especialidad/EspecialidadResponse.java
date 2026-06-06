package pe.edu.utp.clinica.dto.especialidad;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para la respuesta con datos de la especialidad.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class EspecialidadResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private boolean activo;
}