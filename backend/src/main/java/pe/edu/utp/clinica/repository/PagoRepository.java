package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.CitaMedica;
import pe.edu.utp.clinica.model.Pago;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Pago.
 *
 * RF-11: Pago generado automáticamente al crear la cita.
 * RF-14: Registro del pago de una cita.
 * RF-17: Actualización del estado a PAGADO.
 * RF-35: Listar pagos por paciente (CORRECCIÓN: query JPQL directo,
 *         antes se usaba findAll() + filter en Java lo que cargaba
 *         toda la tabla en memoria).
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** Busca el pago asociado a una cita específica (RF-14) */
    Optional<Pago> findByCita(CitaMedica cita);

    /**
     * Lista todos los pagos de un paciente usando su ID.
     * RF-35: Query JPQL directo — evita cargar toda la tabla en memoria.
     * CORRECCIÓN: reemplaza el findAll() + filter que existía en PagoService.
     *
     * @param pacienteId ID del paciente
     * @return lista de pagos del paciente ordenados por fecha descendente
     */
    @Query("SELECT p FROM Pago p WHERE p.cita.paciente.id = :pacienteId ORDER BY p.creadoEn DESC")
    List<Pago> findByCitaPacienteId(@Param("pacienteId") Long pacienteId);
}