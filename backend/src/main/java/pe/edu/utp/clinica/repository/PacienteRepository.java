package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.Paciente;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Paciente.
 * RF-01: Registro de pacientes.
 * RF-03: Búsqueda por DNI, nombre o apellido.
 * RF-04: Listado de todos los pacientes.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    /** Verifica si ya existe un paciente con ese DNI (RF-01) */
    boolean existsByDni(String dni);

    /** Busca paciente por DNI exacto (RF-03) */
    Optional<Paciente> findByDni(String dni);

    /**
     * Búsqueda flexible por DNI, nombre o apellido (RF-03).
     * Usa ILIKE para búsqueda insensible a mayúsculas en PostgreSQL.
     */
    @Query("""
        SELECT p FROM Paciente p
        WHERE p.dni LIKE %:criterio%
           OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', :criterio, '%'))
           OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :criterio, '%'))
        """)
    List<Paciente> buscarPorCriterio(@Param("criterio") String criterio);
}