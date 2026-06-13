package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.common.enums.TipoAccion;
import pe.edu.utp.clinica.model.AuditoriaCita;
import pe.edu.utp.clinica.model.CitaMedica;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad AuditoriaCita.
 * RF-42: Consulta del historial de auditoría por cita.
 * RF-43: Filtrado por usuario, tipo de acción o rango de fechas.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface AuditoriaCitaRepository extends JpaRepository<AuditoriaCita, Long> {

    /** Lista toda la auditoría de una cita (RF-42) */
    List<AuditoriaCita> findByCitaOrderByFechaAccionAsc(CitaMedica cita);

    /**
     * Filtrado de auditoría por múltiples criterios (RF-43).
     */
    @Query("""
        SELECT a FROM AuditoriaCita a
        WHERE (:usuarioId  IS NULL OR a.usuario.id   = :usuarioId)
          AND (:tipoAccion IS NULL OR a.tipoAccion    = :tipoAccion)
          AND (:desde      IS NULL OR a.fechaAccion  >= :desde)
          AND (:hasta      IS NULL OR a.fechaAccion  <= :hasta)
        ORDER BY a.fechaAccion DESC
        """)
    List<AuditoriaCita> filtrar(
            @Param("usuarioId")  Long usuarioId,
            @Param("tipoAccion") TipoAccion tipoAccion,
            @Param("desde")      LocalDateTime desde,
            @Param("hasta")      LocalDateTime hasta
    );
}