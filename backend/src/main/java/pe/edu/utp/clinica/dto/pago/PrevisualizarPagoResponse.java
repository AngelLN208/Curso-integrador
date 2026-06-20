package pe.edu.utp.clinica.dto.pago;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para previsualizar el cálculo de un pago antes de confirmarlo.
 * RF-16: Muestra a la recepcionista si el paciente tiene seguro,
 *        cuánto descuento aplica y el monto final, antes de cobrar.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class PrevisualizarPagoResponse {

    private BigDecimal monto;
    private boolean     tieneSeguro;
    private String      nombreSeguro;
    private BigDecimal  porcentajeCobertura;
    private BigDecimal  descuento;
    private BigDecimal  montoFinal;
}