package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.CitaMedica;
import pe.edu.utp.clinica.model.Triaje;

import java.util.Optional;

/**
 * Repositorio para la entidad Triaje.
 * RF-22: Registro del triaje asociado a una cita.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface TriajeRepository extends JpaRepository<Triaje, Long> {

    /** Verifica si ya existe triaje para la cita */
    boolean existsByCita(CitaMedica cita);

    /** Busca el triaje de una cita específica */
    Optional<Triaje> findByCita(CitaMedica cita);
}