package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa la calificación de un médico por un paciente.
 *
 * RF-54: El paciente califica al médico (1-5 estrellas + comentario)
 *        después de que su cita queda en estado ATENDIDA.
 *        Una sola valoración por cita.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "valoraciones",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_valoracion_cita",
               columnNames = "cita_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Valoracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cita médica que se está valorando — una sola valoración por cita */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private CitaMedica cita;

    /** Paciente que realiza la valoración */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    /** Médico que recibe la valoración */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    /**
     * Puntuación de 1 a 5 estrellas.
     * RF-54: Escala de valoración de la atención recibida.
     */
    @NotNull
    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer puntuacion;

    /** Comentario opcional del paciente */
    @Column(length = 500)
    private String comentario;

    /** Fecha y hora en que se registró la valoración */
    @Column(nullable = false, updatable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    protected void onCreate() {
        this.registradoEn = LocalDateTime.now();
    }
}