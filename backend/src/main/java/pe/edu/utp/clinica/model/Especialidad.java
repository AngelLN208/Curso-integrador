package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad que representa una especialidad médica.
 *
 * RF-39: El administrador puede registrar y modificar
 *        especialidades con nombre, descripción y costo.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "especialidades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de la especialidad. No puede estar duplicado (RF-39) */
    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    /** Descripción opcional de la especialidad */
    @Column(length = 300)
    private String descripcion;

    /** Costo de la consulta para esta especialidad */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal costo;

    /** Indica si la especialidad está activa */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}