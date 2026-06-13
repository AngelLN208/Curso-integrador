package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa un paciente de la clínica.
 *
 * RF-01: Registro con DNI, nombre, apellidos, fecha de nacimiento,
 *        celular, correo y sexo.
 * RF-02: Se pueden actualizar todos los datos excepto el DNI.
 * RF-03: Búsqueda por DNI, nombre o apellido.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "pacientes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * DNI del paciente. No puede estar duplicado (RF-01).
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

    /** Fecha de nacimiento del paciente */
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El celular es obligatorio")
    @Column(nullable = false, length = 15)
    private String celular;

    /** Correo electrónico — necesario para envío de comprobantes (RF-20) */
    @Email(message = "El correo debe tener un formato válido")
    @Column(length = 100)
    private String correo;

    /**
     * Sexo del paciente: M (Masculino) o F (Femenino).
     */
    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "[MF]", message = "El sexo debe ser M o F")
    @Column(nullable = false, length = 1)
    private String sexo;

    /** Fecha de registro en el sistema */
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}