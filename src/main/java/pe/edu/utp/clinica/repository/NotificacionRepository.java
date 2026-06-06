package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.Notificacion;

import java.util.List;

/**
 * Repositorio para la entidad Notificacion.
 * RNF-07: El scheduler consulta las notificaciones pendientes
 *         cada 60 segundos para procesarlas.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Lista notificaciones pendientes de envío (para el scheduler) */
    List<Notificacion> findByEstado(String estado);
}