package pe.edu.utp.clinica.dto.portal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO de respuesta para el directorio de especialidades.
 * RF-52: Lista especialidades activas con sus médicos.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class EspecialidadDirectorioResponse {

    private Long                          id;
    private String                        nombre;
    private String                        descripcion;
    private int                           totalMedicos;
    private List<MedicoDirectorioResponse> medicos;
}