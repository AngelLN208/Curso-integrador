package pe.edu.utp.clinica.dto.seguro;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para la respuesta con datos del seguro médico.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class SeguroResponse {

    private Long id;
    private String nombre;
    private String tipo;
    private BigDecimal porcentajeCobertura;
    private BigDecimal deducible;
    private boolean convenioActivo;
}