package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.HorarioMedico;
import pe.edu.utp.clinica.model.Medico;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Repositorio para la entidad HorarioMedico.
 * RF-38: Asignación de horarios a médicos.
 * RF-12: Base para mostrar horarios disponibles.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface HorarioMedicoRepository extends JpaRepository<HorarioMedico, Long> {

    /** Lista los horarios de un médico específico */
    List<HorarioMedico> findByMedico(Medico medico);

    /** Lista horarios de un médico en un día específico */
    List<HorarioMedico> findByMedicoAndDia(Medico medico, DayOfWeek dia);

    /**
     * Detecta traslape de horarios para un médico en el mismo día.
     * RF-38: No se permiten traslapes de horario para un mismo médico.
     */
    @Query("""
        SELECT COUNT(h) > 0 FROM HorarioMedico h
        WHERE h.medico = :medico
          AND h.dia = :dia
          AND h.horaInicio < :horaFin
          AND h.horaFin > :horaInicio
        """)
    boolean existeTraslape(
            @Param("medico") Medico medico,
            @Param("dia") DayOfWeek dia,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );
}