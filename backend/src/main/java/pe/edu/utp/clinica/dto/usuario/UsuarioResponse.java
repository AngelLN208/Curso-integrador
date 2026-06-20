package pe.edu.utp.clinica.dto.usuario;

import lombok.Builder;
import lombok.Data;
import pe.edu.utp.clinica.common.enums.RolUsuario;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta con datos de un usuario del sistema.
 * Usado en el panel de administración para listar cuentas.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class UsuarioResponse {

    private Long id;
    private String username;
    private String nombreCompleto;
    private RolUsuario rol;
    private boolean activo;
    private LocalDateTime creadoEn;
}