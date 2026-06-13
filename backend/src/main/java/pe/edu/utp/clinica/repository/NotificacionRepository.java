package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.Notificacion;

import java.util.List;

/**
 * Repositorio para la entidad Notificacion.
 *
 * RNF-07: El scheduler consulta notificaciones pendientes cada 60 segundos.
 * RF-47:  Verificación de recordatorios duplicados directamente en BD.
 *
 * BUG CORREGIDO: se agrega existsByCitaIdAndTipo() para que el scheduler
 * verifique duplicados con un COUNT en BD en lugar de cargar toda la tabla
 * de notificaciones ENVIADAS en memoria (stream().anyMatch()).
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Lista notificaciones por estado para el scheduler.
     * RNF-07: Usado cada 60 segundos para procesar las PENDIENTES.
     *
     * @param estado "PENDIENTE" o "ENVIADO"
     */
    List<Notificacion> findByEstado(String estado);

    /**
     * Verifica si ya existe una notificación de un tipo específico para una cita.
     * RF-47: Evita generar recordatorios duplicados para la misma cita.
     *
     * BUG CORREGIDO: reemplaza el findByEstado("ENVIADO") + stream().anyMatch()
     * que cargaba toda la tabla en memoria. Este método ejecuta un COUNT
     * directo en BD independientemente del tamaño de la tabla.
     *
     * @param citaId ID de la cita
     * @param tipo   tipo de notificación ("RECORDATORIO", "REGISTRO", etc.)
     * @return true si ya existe una notificación de ese tipo para esa cita
     */
    @Query("SELECT COUNT(n) > 0 FROM Notificacion n WHERE n.cita.id = :citaId AND n.tipo = :tipo")
    boolean existsByCitaIdAndTipo(@Param("citaId") Long citaId, @Param("tipo") String tipo);
}