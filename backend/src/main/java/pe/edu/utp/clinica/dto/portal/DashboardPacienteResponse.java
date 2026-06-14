package pe.edu.utp.clinica.dto.portal;

import lombok.Builder;
import lombok.Data;
import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para el dashboard personal del paciente.
 * RF-51: Muestra próxima cita, último diagnóstico y pagos pendientes.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class DashboardPacienteResponse {

    /** Datos del paciente autenticado */
    private String nombrePaciente;
    private String dni;
    private String correo;

    /** Próxima cita confirmada (puede ser null si no hay) */
    private ProximaCitaDTO proximaCita;

    /** Último diagnóstico registrado (puede ser null) */
    private UltimoDiagnosticoDTO ultimoDiagnostico;

    /** Pago pendiente más reciente (puede ser null) */
    private PagoPendienteDTO pagoPendiente;

    /** Totales de resumen */
    private int totalCitas;
    private int citasPendientes;
    private int citasConfirmadas;
    private int citasAtendidas;

    @Data
    @Builder
    public static class ProximaCitaDTO {
        private Long          citaId;
        private LocalDateTime fechaHora;
        private String        medicoNombre;
        private String        especialidad;
        private EstadoCita    estado;
        private Long          pagoId;
        private EstadoPago    estadoPago;
    }

    @Data
    @Builder
    public static class UltimoDiagnosticoDTO {
        private Long          consultaId;
        private LocalDateTime fechaCita;
        private String        medicoNombre;
        private String        especialidad;
        private String        diagnostico;
        private String        tratamiento;
    }

    @Data
    @Builder
    public static class PagoPendienteDTO {
        private Long       pagoId;
        private Long       citaId;
        private BigDecimal monto;
        private BigDecimal montoFinal;
        private String     metodoPago;
    }
}