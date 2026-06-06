package pe.edu.utp.clinica.common.enums;

/**
 * Estados posibles de una cita médica.
 * RNF-13: Se usan enumeraciones para evitar valores de estado en texto libre.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public enum EstadoCita {
    PENDIENTE,      // Cita registrada, sin confirmar pago (RF-05)
    CONFIRMADA,     // Pago validado (RF-15)
    REPROGRAMADA,   // Fecha u hora modificada (RF-06)
    CANCELADA,      // Cancelada por recepcionista o paciente (RF-09)
    ATENDIDA        // Consulta médica registrada (RF-23)
}