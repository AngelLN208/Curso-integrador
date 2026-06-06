package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.PacienteSeguro;
import pe.edu.utp.clinica.model.SeguroMedico;

import java.util.List;

/**
 * Repositorio para la entidad PacienteSeguro.
 * RF-50: Vinculación de seguros a pacientes.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface PacienteSeguroRepository extends JpaRepository<PacienteSeguro, Long> {

    /** Verifica si el paciente ya tiene ese seguro vinculado (RF-50) */
    boolean existsByPacienteAndSeguro(Paciente paciente, SeguroMedico seguro);

    /** Lista seguros activos de un paciente */
    List<PacienteSeguro> findByPacienteAndActivoTrue(Paciente paciente);
}