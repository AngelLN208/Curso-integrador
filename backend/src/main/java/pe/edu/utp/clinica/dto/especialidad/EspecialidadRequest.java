package pe.edu.utp.clinica.dto.especialidad;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para registrar o actualizar una especialidad.
 * RF-39: Nombre y descripción.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class EspecialidadRequest {

    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    private String nombre;

    private String descripcion;
}