package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.common.enums.TipoAccion;
import pe.edu.utp.clinica.model.AuditoriaCita;
import pe.edu.utp.clinica.model.CitaMedica;

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

        /**
         * Lista toda la auditoría de una cita (RF-42).
         * Usa JOIN FETCH para precargar cita, paciente y usuario y evitar
         * problemas de serialización con proxies lazy de Hibernate.
         */
        @Query("""
                        SELECT a FROM AuditoriaCita a
                        LEFT JOIN FETCH a.cita c
                        LEFT JOIN FETCH c.paciente
                        LEFT JOIN FETCH a.usuario
                        WHERE a.cita = :cita
                        ORDER BY a.fechaAccion ASC
                        """)
        List<AuditoriaCita> findByCitaOrderByFechaAccionAsc(@Param("cita") CitaMedica cita);

        /**
         * Filtrado de auditoría por usuario y/o tipo de acción (RF-43).
         *
         * NOTA: se eliminaron los parámetros de rango de fechas (desde/hasta)
         * porque PostgreSQL + Hibernate 6 no logran inferir el tipo del
         * parámetro cuando se pasa null en una comparación con timestamp
         * (error 42P18 y luego conversión errónea a bytea con CAST).
         * El frontend nunca envía esos filtros, así que se remueven para
         * evitar el bug sin perder funcionalidad real.
         *
         * Usa JOIN FETCH para precargar cita, paciente y usuario y evitar
         * problemas de serialización con proxies lazy de Hibernate.
         */
        @Query("""
                        SELECT a FROM AuditoriaCita a
                        LEFT JOIN FETCH a.cita c
                        LEFT JOIN FETCH c.paciente
                        LEFT JOIN FETCH a.usuario
                        WHERE (:usuarioId  IS NULL OR a.usuario.id  = :usuarioId)
                          AND (:tipoAccion IS NULL OR a.tipoAccion   = :tipoAccion)
                        ORDER BY a.fechaAccion DESC
                        """)
        List<AuditoriaCita> filtrar(
                        @Param("usuarioId")  Long usuarioId,
                        @Param("tipoAccion") TipoAccion tipoAccion
        );
}

