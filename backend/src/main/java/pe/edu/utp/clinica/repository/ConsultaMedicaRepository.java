package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
     */
    List<ConsultaMedica> findByCitaPacienteOrderByCitaFechaHoraDesc(Paciente paciente);
}