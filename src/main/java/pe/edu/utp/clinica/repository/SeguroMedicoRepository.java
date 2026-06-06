package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.SeguroMedico;

import java.util.List;

/**
 * Repositorio para la entidad SeguroMedico.
 * RF-49: Gestión de seguros médicos disponibles.
 * RF-50: Solo seguros con convenio activo pueden vincularse.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface SeguroMedicoRepository extends JpaRepository<SeguroMedico, Long> {

    /** Verifica si ya existe un seguro con ese nombre (RF-49) */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Lista solo seguros con convenio activo (RF-50) */
    List<SeguroMedico> findByConvenioActivoTrue();
}