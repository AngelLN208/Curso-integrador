package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.Especialidad;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Especialidad.
 * RF-39: Gestión de especialidades médicas.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    /** Verifica si ya existe una especialidad con ese nombre (RF-39) */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Lista solo las especialidades activas */
    List<Especialidad> findByActivoTrue();

    /** Busca especialidad por nombre exacto */
    Optional<Especialidad> findByNombreIgnoreCase(String nombre);
}