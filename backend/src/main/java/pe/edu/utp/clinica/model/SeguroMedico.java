package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad que representa un seguro médico disponible en la clínica.
 *
 * RF-49: El administrador puede registrar, actualizar y desactivar
 *        seguros con nombre, tipo y porcentaje de cobertura.
 * RF-16: Se usa para calcular el monto final de la cita.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "seguros_medicos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeguroMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre del seguro. No puede estar duplicado (RF-49) */
    @NotBlank(message = "El nombre del seguro es obligatorio")
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    /** Tipo de seguro: EPS, PARTICULAR, SIS, etc. */
    @NotBlank(message = "El tipo de seguro es obligatorio")
    @Column(nullable = false, length = 50)
    private String tipo;

    /**
     * Porcentaje de cobertura del seguro (0 a 100).
     * RF-16: Se aplica para calcular el monto que paga el paciente.
     */
    @NotNull(message = "El porcentaje de cobertura es obligatorio")
    @DecimalMin(value = "0.0", message = "La cobertura no puede ser negativa")
    @DecimalMax(value = "100.0", message = "La cobertura no puede superar el 100%")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeCobertura;

    /** Deducible aplicado antes del porcentaje de cobertura (RF-16) */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deducible = BigDecimal.ZERO;

    /** Solo seguros con convenio activo pueden usarse en citas (RF-49) */
    @Column(nullable = false)
    @Builder.Default
    private boolean convenioActivo = true;
}