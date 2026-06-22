package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que registra el historial de ediciones de una consulta médica.
 *
 * Regla de negocio: el médico solo puede editar diagnóstico/tratamiento
 * dentro de los 45 minutos posteriores a la hora de la cita. Cada edición
 * queda registrada aquí con los valores anteriores y nuevos.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "auditoria_consultas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaConsulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Consulta médica que fue editada */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private ConsultaMedica consulta;

    /** Médico que realizó la edición */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    private Medico medico;

    @Column(columnDefinition = "TEXT")
    private String diagnosticoAnterior;

    @Column(columnDefinition = "TEXT")
    private String diagnosticoNuevo;

    @Column(columnDefinition = "TEXT")
    private String tratamientoAnterior;

    @Column(columnDefinition = "TEXT")
    private String tratamientoNuevo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaEdicion;

    @PrePersist
    protected void onCreate() {
        this.fechaEdicion = LocalDateTime.now();
    }
}