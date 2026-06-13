package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.TipoAccion;

import java.time.LocalDateTime;

/**
 * Entidad que registra el historial de cambios de una cita médica.
 *
 * RF-41: Toda acción (creación, confirmación, reprogramación,
 *        cancelación) queda registrada con usuario, tipo de acción,
 *        fecha, estado anterior y estado nuevo.
 * RF-42: Se puede consultar el historial de auditoría por cita.
 * RF-43: Se puede filtrar por usuario, tipo de acción o rango de fechas.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "auditoria_citas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cita médica sobre la que se realizó la acción */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false)
    private CitaMedica cita;

    /** Usuario que realizó la acción (RF-41) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /**
     * Tipo de acción realizada.
     * RNF-13: Se usa enum, no texto libre.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAccion tipoAccion;

    /** Estado anterior de la cita antes del cambio */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoCita estadoAnterior;

    /** Estado nuevo de la cita después del cambio */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoCita estadoNuevo;

    /** Fecha y hora en que ocurrió la acción */
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaAccion;

    @PrePersist
    protected void onCreate() {
        this.fechaAccion = LocalDateTime.now();
    }
}