package pe.edu.utp.clinica.dto.medico;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para registrar un médico.
 * RF-37: DNI, nombres, apellidos, especialidad, celular y correo.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class MedicoRequest {

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotNull(message = "La especialidad es obligatoria")
    private Long especialidadId;

    @NotBlank(message = "El celular es obligatorio")
    private String celular;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    private String correo;

    /** Username para el portal del médico */
    @NotBlank(message = "El username es obligatorio")
    @Email(message = "El username debe ser un correo válido")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}