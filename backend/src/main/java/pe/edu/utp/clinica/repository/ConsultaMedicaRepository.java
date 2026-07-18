package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.CitaMedica;
import pe.edu.utp.clinica.model.ConsultaMedica;
import pe.edu.utp.clinica.model.Paciente;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad ConsultaMedica.
 * RF-23: Registro de consulta médica.
 * RF-25: Verificar si ya existe consulta para la cita.
 * RF-26: Historial médico del paciente.
 * RF-27: Ordenado por fecha descendente.
 * RNF-19: Historial en PDF generado en menos de 3 segundos —
 * findByCitaPacienteOrderByCitaFechaHoraDesc() causaba N+1
 * queries al recorrer cita → medico → especialidad de forma
 * lazy en el bucle de HistorialPdfService. Corregido con
 * JOIN FETCH explícito.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface ConsultaMedicaRepository extends JpaRepository<ConsultaMedica, Long> {

    /** Verifica si ya existe una consulta para esa cita (RF-25) */
    boolean existsByCita(CitaMedica cita);

    /** Busca la consulta de una cita específica */
    Optional<ConsultaMedica> findByCita(CitaMedica cita);

    /**
     * Historial médico del paciente ordenado por fecha descendente.
     * RF-26 y RF-27.
     *
     * @deprecated usar {@link #findConHistorialCompletoPorPaciente(Paciente)} —
     *             este método deja cita/médico/especialidad en lazy loading,
     *             causando
     *             N+1 queries en HistorialPdfService (RNF-19).
     */
    @Deprecated
    List<ConsultaMedica> findByCitaPacienteOrderByCitaFechaHoraDesc(Paciente paciente);

    /**
     * Historial médico del paciente ordenado por fecha descendente,
     * con cita, médico y especialidad ya cargados en la misma consulta.
     * RF-26, RF-27, RNF-19 (evita N+1 queries en la generación del PDF).
     */
    @Query("SELECT c FROM ConsultaMedica c " +
            "JOIN FETCH c.cita ci " +
            "JOIN FETCH ci.medico m " +
            "JOIN FETCH m.especialidad " +
            "WHERE ci.paciente = :paciente " +
            "ORDER BY ci.fechaHora DESC")
    List<ConsultaMedica> findConHistorialCompletoPorPaciente(@Param("paciente") Paciente paciente);

    /**
     * Busca la consulta más reciente de un paciente.
     * RF-51: Último diagnóstico mostrado en el dashboard.
     */
    Optional<ConsultaMedica> findTopByCitaPacienteOrderByCitaFechaHoraDesc(
            Paciente paciente);
}