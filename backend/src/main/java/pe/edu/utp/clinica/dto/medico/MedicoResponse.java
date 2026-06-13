package pe.edu.utp.clinica.dto.medico;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para la respuesta con datos del médico.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class MedicoResponse {

    private Long id;
    private String dni;
    private String nombres;
    private String apellidos;
    private String celular;
    private String correo;
    private Long especialidadId;
    private String especialidadNombre;
    private boolean activo;
}