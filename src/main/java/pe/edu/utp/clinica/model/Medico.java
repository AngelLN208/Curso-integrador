package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Entidad que representa un médico de la clínica.
 *
 * RF-37: El administrador registra médicos con sus datos
 *        profesionales y los asocia a una especialidad.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "medicos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * DNI del médico. No puede estar duplicado (RF-37).
     * Exactamente 8 dígitos.
     */
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @NotBlank(message = "Los nombres son obligatorios")
    @Column(nullable = false, length = 100)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Column(nullable = false, length = 100)
    private String apellidos;

    @NotBlank(message = "El celular es obligatorio")
    @Column(nullable = false, length = 15)
    private String celular;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    @Column(nullable = false, length = 100)
    private String correo;

    /**
     * Especialidad médica asociada (RF-37).
     * Un médico pertenece a una especialidad.
     */
    @NotNull(message = "La especialidad es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    /**
     * Usuario del sistema vinculado al médico.
     * Permite al médico autenticarse y usar su portal.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Indica si el médico está activo en el sistema */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}