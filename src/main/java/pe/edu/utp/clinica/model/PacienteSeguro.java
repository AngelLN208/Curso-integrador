package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa la relación entre un paciente y su seguro médico.
 *
 * RF-50: El administrador puede asociar uno o más seguros a un paciente.
 *        No se puede duplicar el mismo seguro para el mismo paciente.
 *        Solo se aceptan seguros con convenio activo.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(
    name = "paciente_seguros",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"paciente_id", "seguro_id"},
        name = "uk_paciente_seguro"
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteSeguro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seguro_id", nullable = false)
    private SeguroMedico seguro;

    /** Número de póliza del paciente con este seguro */
    @Column(length = 50)
    private String numeroPoliza;

    /** Indica si esta vinculación está activa */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}