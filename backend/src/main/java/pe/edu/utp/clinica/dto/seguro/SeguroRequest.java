package pe.edu.utp.clinica.dto.seguro;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para registrar o actualizar un seguro médico.
 * RF-49: Nombre, tipo y porcentaje de cobertura.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class SeguroRequest {

    @NotBlank(message = "El nombre del seguro es obligatorio")
    private String nombre;

    @NotBlank(message = "El tipo de seguro es obligatorio")
    private String tipo;

    @NotNull(message = "El porcentaje de cobertura es obligatorio")
    @DecimalMin(value = "0.0", message = "La cobertura no puede ser negativa")
    @DecimalMax(value = "100.0", message = "La cobertura no puede superar el 100%")
    private BigDecimal porcentajeCobertura;

    private BigDecimal deducible;
}