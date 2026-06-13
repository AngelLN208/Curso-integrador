package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa la consulta médica registrada por el doctor.
 *
 * RF-23: Se registra diagnóstico y tratamiento.
 *        No se permite más de una consulta por cita.
 * RF-24: Se asocia a una cita mediante su ID.
 * RF-25: El sistema impide registrar más de una consulta por cita.
 * RF-26: Base para consultar el historial médico del paciente.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "consultas_medicas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cita médica a la que corresponde esta consulta (uno a uno) */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private CitaMedica cita;

    /** Diagnóstico emitido por el médico */
    @NotBlank(message = "El diagnóstico es obligatorio")
    @Column(nullable = false, length = 500)
    private String diagnostico;

    /** Tratamiento indicado por el médico */
    @NotBlank(message = "El tratamiento es obligatorio")
    @Column(nullable = false, length = 500)
    private String tratamiento;

    /** Observaciones adicionales opcionales */
    @Column(length = 500)
    private String observaciones;

    /** Fecha y hora del registro de la consulta */
    @Column(nullable = false, updatable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    protected void onCreate() {
        this.registradoEn = LocalDateTime.now();
    }
}