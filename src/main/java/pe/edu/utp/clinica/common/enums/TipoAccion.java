package pe.edu.utp.clinica.common.enums;

/**
 * Tipos de acción registrados en la auditoría de citas.
 * RF-41: Toda acción sobre una cita queda registrada.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public enum TipoAccion {
    CREACION,
    CONFIRMACION,
    REPROGRAMACION,
    CANCELACION,
    ATENDIDA
}