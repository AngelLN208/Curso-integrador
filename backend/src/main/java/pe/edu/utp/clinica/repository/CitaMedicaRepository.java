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
     * Búsqueda flexible de citas por múltiples criterios (RF-07).
     */
    @Query("""
            SELECT c FROM CitaMedica c
            WHERE (:pacienteId IS NULL OR c.paciente.id = :pacienteId)
              AND (:medicoId   IS NULL OR c.medico.id   = :medicoId)
              AND (:estado     IS NULL OR c.estado       = :estado)
              AND (:fecha      IS NULL OR CAST(c.fechaHora AS date) = CAST(:fecha AS date))
            ORDER BY c.fechaHora DESC
            """)
    List<CitaMedica> buscarPorFiltros(
            @Param("pacienteId") Long pacienteId,
            @Param("medicoId") Long medicoId,
            @Param("estado") EstadoCita estado,
            @Param("fecha") LocalDateTime fecha);

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