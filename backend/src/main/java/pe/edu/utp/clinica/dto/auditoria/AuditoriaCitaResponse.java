package pe.edu.utp.clinica.dto.auditoria;

import lombok.Builder;
import lombok.Data;
import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.TipoAccion;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta de auditoría de citas.
 * Evita exponer las entidades JPA directamente (proxies de Hibernate
 * con relaciones LAZY no son serializables por Jackson sin esto).
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
public class AuditoriaCitaResponse {

    private Long id;
    private Long citaId;
    private Long pacienteId;
    private String pacienteNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private TipoAccion tipoAccion;
    private EstadoCita estadoAnterior;
    private EstadoCita estadoNuevo;
    private LocalDateTime fechaAccion;
}