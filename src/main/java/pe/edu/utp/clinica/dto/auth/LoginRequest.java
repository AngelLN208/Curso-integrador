package pe.edu.utp.clinica.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para la petición de login.
 * RF-40: Autenticación con username y contraseña.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class LoginRequest {

    @NotBlank(message = "El username es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}