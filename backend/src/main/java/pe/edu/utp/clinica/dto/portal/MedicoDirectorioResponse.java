package pe.edu.utp.clinica.dto.portal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO de respuesta para el directorio médico público.
 * RF-52: Muestra médicos activos con sus horarios disponibles.
 * No expone datos sensibles (DNI, celular, correo del médico).
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class MedicoDirectorioResponse {

    private Long        id;
    private String      nombreCompleto;
    private String      especialidad;
    private Long        especialidadId;
    private boolean     activo;
    private Double      promedioValoracion;
    private Integer     totalValoraciones;

    /** Horarios disponibles del médico (día + rango horario) */
    private List<HorarioDisponibleResponse> horarios;

    @Data
    @Builder
    public static class HorarioDisponibleResponse {
        private String dia;
        private String horaInicio;
        private String horaFin;
    }
}