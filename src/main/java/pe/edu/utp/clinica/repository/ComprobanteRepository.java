package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.Comprobante;
import pe.edu.utp.clinica.model.Pago;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Comprobante.
 * RF-18: Generación de comprobante al confirmar pago.
 * RF-36: Consulta de comprobantes del paciente.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    /** Busca comprobante por pago */
    Optional<Comprobante> findByPago(Pago pago);

    /** Lista comprobantes de un paciente a través del pago */
    List<Comprobante> findByPagoCitaPacienteId(Long pacienteId);

    /** Cuenta comprobantes para generar número único (RF-18) */
    long count();
}