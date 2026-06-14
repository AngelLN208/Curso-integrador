package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.CitaMedica;
import pe.edu.utp.clinica.model.Valoracion;

/**
 * Repositorio para la entidad Valoracion.
 * RF-54: Calificación post-consulta de médicos por pacientes.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    /** Verifica si ya existe valoración para una cita (RF-54: evita duplicados) */
    boolean existsByCita(CitaMedica cita);

    /**
     * Calcula el promedio de valoraciones de un médico.
     * RF-52: Se muestra en el directorio médico público.
     */
    @Query("SELECT AVG(v.puntuacion) FROM Valoracion v WHERE v.medico.id = :medicoId")
    Double promedioByMedico(@Param("medicoId") Long medicoId);

    /**
     * Cuenta el total de valoraciones recibidas por un médico.
     * RF-52: Se muestra junto al promedio en el directorio.
     */
    @Query("SELECT COUNT(v) FROM Valoracion v WHERE v.medico.id = :medicoId")
    Integer countByMedicoId(@Param("medicoId") Long medicoId);
}