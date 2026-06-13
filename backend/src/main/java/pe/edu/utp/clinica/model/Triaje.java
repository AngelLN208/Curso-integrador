package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa el triaje del paciente antes de la consulta.
 *
 * RF-22: Se registra presión, temperatura y peso.
 *        Solo si la cita está en estado CONFIRMADA.
 * RF-24: Se asocia a una cita previamente registrada.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "triajes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Triaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cita médica a la que pertenece este triaje */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private CitaMedica cita;

    /** Presión arterial del paciente (Ej: "120/80") */
    @Column(length = 20)
    private String presionArterial;

    /** Temperatura corporal en °C */
    @Positive(message = "La temperatura debe ser mayor a cero")
    @Column(precision = 4, scale = 1)
    private BigDecimal temperatura;

    /** Peso en kilogramos */
    @Positive(message = "El peso debe ser mayor a cero")
    @Column(precision = 5, scale = 2)
    private BigDecimal peso;

    /** Fecha y hora del registro del triaje */
    @Column(nullable = false, updatable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    protected void onCreate() {
        this.registradoEn = LocalDateTime.now();
    }
}