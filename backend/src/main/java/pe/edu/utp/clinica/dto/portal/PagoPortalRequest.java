package pe.edu.utp.clinica.dto.portal;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para el pago de una cita desde el portal del paciente.
 *
 * RF-56: El paciente paga su cita directamente desde el portal.
 *        Solo puede pagar citas que le pertenezcan y estén PENDIENTE.
 *
 * Validaciones:
 *  - citaId obligatorio y positivo
 *  - metodoPago obligatorio (EFECTIVO, TARJETA, TRANSFERENCIA, YAPE, PLIN)
 *  - numeroTarjeta: 16 dígitos si método es TARJETA (validación Luhn)
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class PagoPortalRequest {

    @NotNull(message = "El ID de la cita es obligatorio")
    @Positive(message = "El ID de la cita debe ser un número positivo")
    private Long citaId;

    @NotBlank(message = "El método de pago es obligatorio")
    @Pattern(regexp = "EFECTIVO|TARJETA|TRANSFERENCIA|YAPE|PLIN",
             message = "Método de pago no válido. " +
                       "Opciones: EFECTIVO, TARJETA, TRANSFERENCIA, YAPE, PLIN")
    private String metodoPago;

    /**
     * Número de tarjeta — solo requerido si metodoPago = TARJETA.
     * 16 dígitos numéricos. Se valida algoritmo de Luhn en el service.
     */
    @Pattern(regexp = "\\d{16}",
             message = "El número de tarjeta debe tener exactamente 16 dígitos")
    private String numeroTarjeta;

    /**
     * Nombre del titular de la tarjeta.
     * Requerido si metodoPago = TARJETA.
     */
    @Size(max = 100, message = "El nombre del titular no puede superar 100 caracteres")
    private String titularTarjeta;
}