package pe.edu.utp.clinica.dto.paciente;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para la respuesta con datos del paciente.
 * RF-04: Muestra DNI, nombre, apellido, celular y correo.
 * RF-50: Incluye seguros vinculados al paciente.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class PacienteResponse {

    private Long id;
    private String dni;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String celular;
    private String correo;
    private String sexo;
    private LocalDateTime creadoEn;

    /** RF-50: Seguros médicos vinculados al paciente */
    private List<SeguroPacienteDTO> seguros;

    @Data
    @Builder
    public static class SeguroPacienteDTO {
        private Long id; // ID del vínculo PacienteSeguro (para desvincular)
        private Long seguroId;
        private String nombre;
        private String tipo;
        private java.math.BigDecimal porcentajeCobertura;
        private java.math.BigDecimal deducible;
        private boolean convenioActivo;
        private boolean activo;
        private String numeroPoliza;
    }
}