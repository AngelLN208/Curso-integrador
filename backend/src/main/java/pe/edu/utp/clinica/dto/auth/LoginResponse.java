package pe.edu.utp.clinica.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta del login exitoso.
 * Devuelve el token JWT y datos básicos del usuario.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String username;
    private String nombreCompleto;
    private String rol;
}