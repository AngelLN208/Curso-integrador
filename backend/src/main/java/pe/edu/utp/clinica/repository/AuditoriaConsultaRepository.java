package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.AuditoriaConsulta;
import pe.edu.utp.clinica.model.ConsultaMedica;

import java.util.List;

/**
 * Repositorio para la entidad AuditoriaConsulta.
 * Historial de ediciones sobre diagnóstico/tratamiento de consultas.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface AuditoriaConsultaRepository extends JpaRepository<AuditoriaConsulta, Long> {

    @Query("""
            SELECT a FROM AuditoriaConsulta a
            LEFT JOIN FETCH a.medico
            WHERE a.consulta = :consulta
            ORDER BY a.fechaEdicion DESC
            """)
    List<AuditoriaConsulta> findByConsultaOrderByFechaEdicionDesc(@Param("consulta") ConsultaMedica consulta);
}