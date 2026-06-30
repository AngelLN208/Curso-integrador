package pe.edu.utp.clinica.dto.portal;

import jakarta.validation.constraints.*;
import lombok.Data;
import pe.edu.utp.clinica.common.validation.ValidCelular;
import pe.edu.utp.clinica.common.validation.ValidEdad;

import java.time.LocalDate;

/**
 * DTO para que el paciente actualice su propio perfil desde el portal.
 * RF-02 (extendido): el DNI nunca se incluye aquí — es intocable.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class PerfilPacienteRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    @ValidEdad
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El celular es obligatorio")
    @ValidCelular
    private String celular;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    private String correo;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "[MF]", message = "El sexo debe ser M o F")
    private String sexo;
}