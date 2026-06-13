package pe.edu.utp.clinica.common.enums;

/**
 * Estados posibles de un pago asociado a una cita médica.
 * RNF-13: Enumeración para evitar texto libre en campos de estado.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public enum EstadoPago {
    PENDIENTE,  // Generado automáticamente al crear cita (RF-11)
    PAGADO,     // Transacción confirmada (RF-17)
    ANULADO     // Pago anulado por cancelación de cita
}