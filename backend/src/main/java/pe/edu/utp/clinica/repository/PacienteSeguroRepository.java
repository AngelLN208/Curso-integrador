package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.PacienteSeguro;
import pe.edu.utp.clinica.model.SeguroMedico;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad PacienteSeguro.
 *
 * RF-50: Vinculación de seguros a pacientes.
 * RF-16: Consulta del seguro activo para aplicar cobertura al pago.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface PacienteSeguroRepository extends JpaRepository<PacienteSeguro, Long> {

    /**
     * Verifica si el paciente ya tiene ese seguro vinculado.
     * RF-50: Evita duplicados al asociar seguros.
     */
    boolean existsByPacienteAndSeguro(Paciente paciente, SeguroMedico seguro);

    /**
     * Lista todos los seguros activos de un paciente.
     * RF-50: Para mostrar seguros vinculados en el perfil.
     */
    List<PacienteSeguro> findByPacienteAndActivoTrue(Paciente paciente);

    /**
     * Busca el primer seguro activo del paciente para aplicar al pago.
     * RF-16: Usado por PagoService.calcularMontoConSeguro() para
     *         descontar el porcentaje de cobertura del monto bruto.
     *
     * @param paciente paciente del cual buscar el seguro
     * @return Optional con el primer seguro activo, o vacío si no tiene
     */
    Optional<PacienteSeguro> findFirstByPacienteAndActivoTrue(Paciente paciente);
}