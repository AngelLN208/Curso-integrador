package pe.edu.utp.clinica.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import pe.edu.utp.clinica.common.enums.RolUsuario;

/**
 * DTO para editar una cuenta de usuario existente.
 * El campo password es opcional: si viene vacío o nulo,
 * la contraseña actual no se modifica.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class EditarUsuarioRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String username;

    @NotBlank(message = "El rol es obligatorio")
    private String rol;

    /** Opcional: si es null o vacío, no se cambia la contraseña actual */
    private String password;
}