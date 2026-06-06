package pe.edu.utp.clinica.dto.paciente;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO para registrar o actualizar un paciente.
 * RF-01: Campos obligatorios para registro.
 * RF-02: Mismos campos para actualización (sin DNI).
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class PacienteRequest {

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El celular es obligatorio")
    private String celular;

    @Email(message = "El correo debe tener un formato válido")
    private String correo;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "[MF]", message = "El sexo debe ser M o F")
    private String sexo;
}