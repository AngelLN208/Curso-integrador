package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Entidad que representa el horario semanal de un médico.
 *
 * RF-38: El administrador asigna horarios indicando
 *        día de la semana, hora de inicio y hora de fin.
 *        No se permiten traslapes de horario para un mismo médico.
 *
 * RF-12: Base para mostrar horarios disponibles al registrar cita.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "horarios_medico")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorarioMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Médico al que pertenece este horario */
    @NotNull(message = "El médico es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    /**
     * Día de la semana (MONDAY, TUESDAY, etc.)
     * Se almacena como texto para legibilidad en la BD.
     */
    @NotNull(message = "El día es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DayOfWeek dia;

    @NotNull(message = "La hora de inicio es obligatoria")
    @Column(nullable = false)
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    @Column(nullable = false)
    private LocalTime horaFin;
}