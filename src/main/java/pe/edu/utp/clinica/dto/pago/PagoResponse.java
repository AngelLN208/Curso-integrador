package pe.edu.utp.clinica.dto.pago;

import lombok.Builder;
import lombok.Data;
import pe.edu.utp.clinica.common.enums.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para la respuesta con datos del pago.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class PagoResponse {

    private Long id;
    private Long citaId;
    private String pacienteNombre;
    private BigDecimal monto;
    private BigDecimal montoFinal;
    private String metodoPago;
    private LocalDateTime fechaPago;
    private EstadoPago estado;
    private LocalDateTime creadoEn;
}