package pe.edu.utp.clinica.dto.especialidad;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para registrar o actualizar una especialidad.
 * RF-39: Nombre, descripción y costo.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class EspecialidadRequest {

    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El costo es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo no puede ser negativo")
    private BigDecimal costo;
}