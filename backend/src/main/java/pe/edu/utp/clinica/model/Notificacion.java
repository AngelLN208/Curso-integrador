package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa una notificación enviada al paciente.
 *
 * RF-44: Notificación al registrar una cita.
 * RF-45: Notificación al reprogramar una cita.
 * RF-46: Notificación al cancelar una cita.
 * RF-47: Recordatorio 24 horas antes de la cita.
 * RF-48: Alerta de conflicto de horario.
 * RNF-07: El scheduler procesa notificaciones cada 60 segundos.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "notificaciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Paciente destinatario de la notificación */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    /** Cita médica relacionada a la notificación */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private CitaMedica cita;

    /** Tipo: REGISTRO, REPROGRAMACION, CANCELACION, RECORDATORIO */
    @Column(nullable = false, length = 30)
    private String tipo;

    /** Mensaje de la notificación */
    @Column(nullable = false, length = 500)
    private String mensaje;

    /** Estado: PENDIENTE o ENVIADO */
    @Column(nullable = false, length = 15)
    @Builder.Default
    private String estado = "PENDIENTE";

    /** Fecha y hora de creación */
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    /** Fecha y hora en que fue enviada */
    @Column
    private LocalDateTime enviadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}