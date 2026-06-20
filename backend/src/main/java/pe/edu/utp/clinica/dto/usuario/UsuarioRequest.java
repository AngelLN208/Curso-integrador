package pe.edu.utp.clinica.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pe.edu.utp.clinica.common.enums.RolUsuario;

/**
 * DTO para registrar una nueva cuenta de usuario del sistema
 * (administrador o recepcionista). Los médicos se registran
 * mediante MedicoController, que crea su propio usuario asociado.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class UsuarioRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;
}