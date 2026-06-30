package pe.edu.utp.clinica.dto.portal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para que el paciente cambie su contraseña desde el portal.
 * Requiere la contraseña actual para verificar identidad.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class CambiarPasswordRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
    private String passwordNueva;

    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String passwordNuevaConfirmacion;
}