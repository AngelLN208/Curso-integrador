package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.Especialidad;
import pe.edu.utp.clinica.model.Medico;
import pe.edu.utp.clinica.model.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Medico.
 * RF-37: Registro y consulta de médicos.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    /** Verifica si ya existe un médico con ese DNI (RF-37) */
    boolean existsByDni(String dni);

    /** Busca médico por DNI */
    Optional<Medico> findByDni(String dni);

    /** Lista médicos activos por especialidad */
    List<Medico> findByEspecialidadAndActivoTrue(Especialidad especialidad);

    /** Lista todos los médicos activos */
    List<Medico> findByActivoTrue();

    /**
     * Lista médicos activos de una especialidad.
     * RF-52: Usado en el directorio público del portal paciente.
     */
    List<Medico> findByEspecialidadIdAndActivoTrue(Long especialidadId);

    /** Busca el médico asociado a un usuario del sistema (usado en login) */
    Optional<Medico> findByUsuario(Usuario usuario);
}