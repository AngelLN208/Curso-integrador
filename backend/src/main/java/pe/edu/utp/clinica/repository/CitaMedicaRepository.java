package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.model.CitaMedica;
import pe.edu.utp.clinica.model.Medico;
import pe.edu.utp.clinica.model.Paciente;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad CitaMedica.
 * RF-05 al RF-13: Gestión completa de citas médicas.
 * RF-10: Validación de disponibilidad del médico.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface CitaMedicaRepository extends JpaRepository<CitaMedica, Long> {

    /** Lista citas de un paciente (RF-08) */
    List<CitaMedica> findByPaciente(Paciente paciente);

    /** Lista citas de un médico */
    List<CitaMedica> findByMedico(Medico medico);

    /** Lista citas por estado (RF-08) */
    List<CitaMedica> findByEstado(EstadoCita estado);

    /** Lista citas de un paciente por estado (RF-07) */
    List<CitaMedica> findByPacienteAndEstado(Paciente paciente, EstadoCita estado);

    /**
     * Verifica conflicto de horario para el médico (RF-10).
     * Detecta si ya existe una cita en el mismo horario
     * que no esté cancelada.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM CitaMedica c
            WHERE c.medico = :medico
              AND c.fechaHora = :fechaHora
              AND c.estado NOT IN ('CANCELADA')
            """)
    boolean existeConflictoHorario(
            @Param("medico") Medico medico,
            @Param("fechaHora") LocalDateTime fechaHora);

    /**
     * Búsqueda flexible de citas SIN filtro de fecha (RF-07).
     *
     * BUG CORREGIDO: el método original combinaba el filtro de fecha
     * (CAST o BETWEEN) con un parámetro que podía ser null, lo que
     * PostgreSQL + Hibernate 6 no logran resolver (errores 42846 y 42P18:
     * "no se puede convertir bytea a date" / "no se pudo determinar el
     * tipo del parámetro"). CORRECCIÓN: se separó la búsqueda en dos
     * métodos — este, sin fecha, y buscarConFecha() para cuando sí se
     * especifica una fecha concreta. Así ningún parámetro de tipo fecha
     * llega nunca como null a PostgreSQL.
     */
    @Query("""
            SELECT c FROM CitaMedica c
            WHERE (:pacienteId IS NULL OR c.paciente.id = :pacienteId)
              AND (:medicoId   IS NULL OR c.medico.id   = :medicoId)
              AND (:estado     IS NULL OR c.estado       = :estado)
            ORDER BY c.fechaHora DESC
            """)
    List<CitaMedica> buscarSinFecha(
            @Param("pacienteId") Long pacienteId,
            @Param("medicoId") Long medicoId,
            @Param("estado") EstadoCita estado);

    /**
     * Búsqueda flexible de citas CON filtro de fecha obligatorio (RF-07).
     * Usado solo cuando el frontend sí especifica una fecha concreta;
     * en ese caso fechaInicio/fechaFin nunca son null.
     */
    @Query("""
            SELECT c FROM CitaMedica c
            WHERE (:pacienteId IS NULL OR c.paciente.id = :pacienteId)
              AND (:medicoId   IS NULL OR c.medico.id   = :medicoId)
              AND (:estado     IS NULL OR c.estado       = :estado)
              AND c.fechaHora BETWEEN :fechaInicio AND :fechaFin
            ORDER BY c.fechaHora DESC
            """)
    List<CitaMedica> buscarConFecha(
            @Param("pacienteId") Long pacienteId,
            @Param("medicoId") Long medicoId,
            @Param("estado") EstadoCita estado,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Lista citas confirmadas del día siguiente para recordatorios (RF-47).
     */
    @Query("""
            SELECT c FROM CitaMedica c
            WHERE c.estado = 'CONFIRMADA'
              AND c.fechaHora BETWEEN :inicio AND :fin
            """)
    List<CitaMedica> findCitasParaRecordatorio(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Busca las citas activas (no canceladas) de un médico en un rango
     * de fechas. Usado por DisponibilidadService para excluir slots ya
     * ocupados — debe usar el mismo criterio que existeConflictoHorario
     * (RF-10), es decir, cualquier cita PENDIENTE, CONFIRMADA o
     * REPROGRAMADA bloquea el horario, no solo las CONFIRMADA.
     */
    @Query("""
            SELECT c FROM CitaMedica c
            WHERE c.medico = :medico
              AND c.estado NOT IN ('CANCELADA')
              AND c.fechaHora BETWEEN :inicio AND :fin
            """)
    List<CitaMedica> findCitasActivasPorMedicoYRango(
            @Param("medico") Medico medico,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Lista todas las citas de un paciente por su ID.
     * RF-51: Usado en el dashboard del paciente.
     */
    @Query("SELECT c FROM CitaMedica c WHERE c.paciente.id = :pacienteId ORDER BY c.fechaHora DESC")
    List<CitaMedica> findByCitasPacienteId(@Param("pacienteId") Long pacienteId);

    /**
     * Busca la próxima cita activa del paciente (PENDIENTE o CONFIRMADA).
     * RF-51: Destacada en el dashboard del paciente.
     */
    @Query("""
            SELECT c FROM CitaMedica c
            WHERE c.paciente.id = :pacienteId
              AND c.estado IN ('PENDIENTE', 'CONFIRMADA')
              AND c.fechaHora >= CURRENT_TIMESTAMP
            ORDER BY c.fechaHora ASC
            LIMIT 1
            """)
    java.util.Optional<CitaMedica> findProximaCitaPaciente(
            @Param("pacienteId") Long pacienteId);

    /**
     * Verifica separación mínima de 45 minutos entre citas del mismo médico.
     * Evita registrar citas demasiado cercanas entre sí.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM CitaMedica c
            WHERE c.medico = :medico
              AND c.estado NOT IN ('CANCELADA')
              AND c.fechaHora BETWEEN :desde AND :hasta
            """)
    boolean existeCitaCercana(
            @Param("medico") Medico medico,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /**
     * Busca citas activas cuya hora ya pasó + tolerancia (para auto-cancelar).
     */
    @Query("""
            SELECT c FROM CitaMedica c
            WHERE c.estado IN ('PENDIENTE', 'CONFIRMADA')
              AND c.fechaHora < :limite
            """)
    List<CitaMedica> findCitasVencidas(@Param("limite") LocalDateTime limite);

}