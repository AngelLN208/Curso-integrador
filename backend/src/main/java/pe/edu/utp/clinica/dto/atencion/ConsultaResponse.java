package pe.edu.utp.clinica.dto.atencion;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para la respuesta con datos de la consulta médica.
 * RF-26: Historial con fecha, diagnóstico y tratamiento.
 * Incluye también los datos de triaje asociados a la misma cita.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class ConsultaResponse {

    private Long id;
    private Long citaId;
    private Long medicoId;
    private String pacienteNombre;
    private String medicoNombre;
    private LocalDateTime fechaCita;
    private String diagnostico;
    private String tratamiento;
    private String observaciones;
    private LocalDateTime registradoEn;

    /** Datos del triaje asociado a la cita (puede venir vacío si no se registró) */
    private String presionArterial;
    private BigDecimal temperatura;
    private BigDecimal peso;

    /**
     * Indica si esta consulta aún puede editarse (dentro de la ventana de 45 min)
     */
    private boolean editable;
}