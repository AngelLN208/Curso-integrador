package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.CitaMedica;
import pe.edu.utp.clinica.model.Pago;
import pe.edu.utp.clinica.model.Paciente;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Pago.
 * RF-11: Pago generado automáticamente al crear la cita.
 * RF-14: Registro del pago de una cita.
 * RF-17: Actualización del estado a PAGADO.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** Busca el pago asociado a una cita */
    Optional<Pago> findByCita(CitaMedica cita);

    /** Lista pagos del paciente a través de la cita (RF-35) */
    List<Pago> findByCitaPaciente(Paciente paciente);
}