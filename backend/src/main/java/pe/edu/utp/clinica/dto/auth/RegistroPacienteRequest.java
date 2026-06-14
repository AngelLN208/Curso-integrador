package pe.edu.utp.clinica.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;
import pe.edu.utp.clinica.common.validation.*;

import java.time.LocalDate;

/**
 * DTO para el registro de un nuevo paciente desde el portal.
 *
 * RF-28: El paciente se registra con DNI, nombre, correo,
 *        celular y contraseña desde el portal de autoatención.
 * RF-29: Las credenciales generadas permiten el login posterior.
 *
 * Validaciones aplicadas:
 *  - @ValidDni        → 8 dígitos exactos
 *  - @ValidCelular    → 9 dígitos, empieza con 9
 *  - @ValidCorreo     → formato estricto con dominio
 *  - @ValidContrasena → mínimo 8 chars, mayúscula, número y símbolo
 *  - @ValidFechaNacimiento → fecha pasada, máximo 120 años
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class RegistroPacienteRequest {

    @NotBlank(message = "El DNI es obligatorio")
    @ValidDni
    private String dni;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(min = 2, max = 100,
          message = "Los nombres deben tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$",
             message = "Los nombres solo pueden contener letras y espacios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 100,
          message = "Los apellidos deben tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$",
             message = "Los apellidos solo pueden contener letras y espacios")
    private String apellidos;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @ValidFechaNacimiento
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El celular es obligatorio")
    @ValidCelular
    private String celular;

    @NotBlank(message = "El correo es obligatorio")
    @ValidCorreo
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @ValidContrasena
    private String contrasena;

    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String confirmarContrasena;

    /**
     * Sexo del paciente: M (Masculino) o F (Femenino).
     */
    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "[MF]",
             message = "El sexo debe ser M (Masculino) o F (Femenino)")
    private String sexo;
}