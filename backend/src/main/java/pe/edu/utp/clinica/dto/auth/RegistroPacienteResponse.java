package pe.edu.utp.clinica.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO de respuesta tras el registro exitoso de un paciente.
 * RF-28: Confirma los datos registrados y entrega el token JWT
 *        para que el paciente pueda acceder al portal inmediatamente.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class RegistroPacienteResponse {

    private Long      pacienteId;
    private String    dni;
    private String    nombreCompleto;
    private String    correo;
    private LocalDate fechaNacimiento;

    /**
     * Token JWT generado automáticamente tras el registro.
     * Permite al paciente acceder al portal sin hacer login por separado.
     */
    private String    token;

    private String    mensaje;
}