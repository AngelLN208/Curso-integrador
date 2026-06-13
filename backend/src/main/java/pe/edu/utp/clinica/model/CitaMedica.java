package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import pe.edu.utp.clinica.common.enums.EstadoCita;

import java.time.LocalDateTime;

/**
 * Entidad que representa una cita médica en el sistema.
 *
 * RF-05: Se registra asociando paciente, médico, fecha y hora.
 *        Estado inicial: PENDIENTE.
 * RF-06: Al modificar fecha u hora cambia a REPROGRAMADA.
 * RF-09: Se puede cancelar cambiando estado a CANCELADA.
 * RF-10: No se permite registrar si el médico no tiene disponibilidad.
 * RF-11: Al registrarse genera automáticamente un pago en PENDIENTE.
 * RF-13: El motivo de la cita es opcional.
 * RF-41: Toda acción queda registrada en auditoría.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "citas_medicas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Paciente al que pertenece la cita */
    @NotNull(message = "El paciente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    /** Médico asignado a la cita */
    @NotNull(message = "El médico es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    /** Fecha y hora de la cita */
    @NotNull(message = "La fecha y hora son obligatorias")
    @Column(nullable = false)
    private LocalDateTime fechaHora;

    /**
     * Motivo de la cita (opcional — RF-13).
     */
    @Column(length = 300)
    private String motivo;

    /**
     * Estado actual de la cita.
     * RNF-13: Se usa enum, no texto libre.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoCita estado = EstadoCita.PENDIENTE;

    /** Usuario que registró o modificó la cita (para auditoría RF-41) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario registradoPor;

    /** Fecha y hora de creación del registro */
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    /** Fecha y hora de la última actualización */
    @Column(nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}