package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.dto.especialidad.EspecialidadRequest;
import pe.edu.utp.clinica.dto.especialidad.EspecialidadResponse;
import pe.edu.utp.clinica.model.Especialidad;
import pe.edu.utp.clinica.repository.EspecialidadRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de especialidades médicas.
 *
 * RF-39: Registrar y modificar especialidades con nombre, descripción y costo.
 * No se permiten nombres duplicados.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    /**
     * Registra una nueva especialidad médica.
     */
    @Transactional
    public EspecialidadResponse registrar(EspecialidadRequest request) {
        if (especialidadRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new IllegalStateException(
                    "Ya existe una especialidad con el nombre: " + request.getNombre());
        }

        Especialidad especialidad = Especialidad.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .costo(request.getCosto())
                .build();

        especialidad = especialidadRepository.save(especialidad);
        log.debug("Especialidad registrada con ID: {}", especialidad.getId());

        return toResponse(especialidad);
    }

    /**
     * Actualiza una especialidad existente.
     */
    @Transactional
    public EspecialidadResponse actualizar(Long id, EspecialidadRequest request) {
        Especialidad especialidad = buscarPorId(id);

        if (!especialidad.getNombre().equalsIgnoreCase(request.getNombre())
                && especialidadRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new IllegalStateException(
                    "Ya existe una especialidad con el nombre: " + request.getNombre());
        }

        especialidad.setNombre(request.getNombre());
        especialidad.setDescripcion(request.getDescripcion());
        especialidad.setCosto(request.getCosto());

        return toResponse(especialidadRepository.save(especialidad));
    }

    /**
     * Lista todas las especialidades activas (uso general: selects, portal
     * paciente, etc).
     */
    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listarActivas() {
        return especialidadRepository.findByActivoTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista TODAS las especialidades (activas e inactivas). Solo para el panel de
     * administración.
     */
    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listarTodas() {
        return especialidadRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Busca una especialidad por ID.
     */
    @Transactional(readOnly = true)
    public EspecialidadResponse obtenerPorId(Long id) {
        return toResponse(buscarPorId(id));
    }

    /**
     * Desactiva una especialidad (ya no podrá asignarse a nuevos médicos).
     */
    @Transactional
    public void desactivar(Long id) {
        Especialidad especialidad = buscarPorId(id);
        especialidad.setActivo(false);
        especialidadRepository.save(especialidad);
        log.debug("Especialidad desactivada con ID: {}", id);
    }

    /**
     * Reactiva una especialidad previamente desactivada.
     */
    @Transactional
    public void activar(Long id) {
        Especialidad especialidad = buscarPorId(id);
        especialidad.setActivo(true);
        especialidadRepository.save(especialidad);
        log.debug("Especialidad activada con ID: {}", id);
    }

    // ─── Métodos internos ─────────────────────────────────────────────

    public Especialidad buscarPorId(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Especialidad no encontrada con ID: " + id));
    }

    private EspecialidadResponse toResponse(Especialidad e) {
        return EspecialidadResponse.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .costo(e.getCosto())
                .activo(e.isActivo())
                .build();
    }
}