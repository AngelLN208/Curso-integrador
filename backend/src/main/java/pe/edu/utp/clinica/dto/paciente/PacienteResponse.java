package pe.edu.utp.clinica.dto.paciente;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para la respuesta con datos del paciente.
 * RF-04: Muestra DNI, nombre, apellido, celular y correo.
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
}