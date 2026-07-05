package pe.edu.utp.clinica.dto.especialidad;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

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
    private BigDecimal costo;
    private boolean activo;
}