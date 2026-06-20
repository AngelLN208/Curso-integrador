package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.clinica.repository.PacienteSeguroRepository;
import pe.edu.utp.clinica.model.PacienteSeguro;
import java.util.List;
import java.util.stream.Collectors;

import pe.edu.utp.clinica.dto.paciente.PacienteRequest;
import pe.edu.utp.clinica.dto.paciente.PacienteResponse;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.repository.PacienteRepository;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Servicio para gestión de pacientes.
 *
 * RF-01: Registrar paciente con datos obligatorios.
 * RF-02: Actualizar datos sin modificar el DNI.
 * RF-03: Buscar por DNI, nombre o apellido.
 * RF-04: Listar todos los pacientes.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PacienteSeguroRepository pacienteSeguroRepository;

    /**
     * Registra un nuevo paciente.
     * RF-01: El DNI no puede estar duplicado.
     *
     * @param request datos del paciente
     * @return paciente registrado
     */
    @Transactional
    public PacienteResponse registrar(PacienteRequest request) {
        if (pacienteRepository.existsByDni(request.getDni())) {
            throw new IllegalStateException(
                    "Ya existe un paciente registrado con el DNI: " + request.getDni());
        }

        Paciente paciente = Paciente.builder()
                .dni(request.getDni())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .fechaNacimiento(request.getFechaNacimiento())
                .celular(request.getCelular())
                .correo(request.getCorreo())
                .sexo(request.getSexo())
                .build();

        paciente = pacienteRepository.save(paciente);
        log.debug("Paciente registrado con ID: {}", paciente.getId());

        return toResponse(paciente);
    }

    /**
     * Actualiza los datos de un paciente.
     * RF-02: No se permite modificar el DNI.
     *
     * @param id      ID del paciente
     * @param request nuevos datos
     * @return paciente actualizado
     */
    @Transactional
    public PacienteResponse actualizar(Long id, PacienteRequest request) {
        Paciente paciente = buscarEntidadPorId(id);

        // RF-02: El DNI no se puede modificar
        paciente.setNombres(request.getNombres());
        paciente.setApellidos(request.getApellidos());
        paciente.setFechaNacimiento(request.getFechaNacimiento());
        paciente.setCelular(request.getCelular());
        paciente.setCorreo(request.getCorreo());
        paciente.setSexo(request.getSexo());

        return toResponse(pacienteRepository.save(paciente));
    }

    /**
     * Busca pacientes por DNI, nombre o apellido.
     * RF-03: Devuelve lista de coincidencias.
     *
     * @param criterio texto a buscar
     * @return lista de pacientes coincidentes
     */
    @Transactional(readOnly = true)
    public List<PacienteResponse> buscar(String criterio) {
        String criterioNormalizado = StringUtils.trimToEmpty(criterio);

        if (StringUtils.isBlank(criterioNormalizado)) {
            throw new IllegalArgumentException("El criterio de búsqueda no puede estar vacío");
        }

        List<Paciente> resultados = pacienteRepository.buscarPorCriterio(criterioNormalizado);

        if (resultados.isEmpty()) {
            log.debug("No se encontraron pacientes con criterio: {}", criterioNormalizado);
        }

        return resultados.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista todos los pacientes registrados.
     * RF-04: Lista completa con DNI, nombre, apellido, celular y correo.
     */
    @Transactional(readOnly = true)
    public List<PacienteResponse> listarTodos() {
        return pacienteRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un paciente por su ID.
     */
    @Transactional(readOnly = true)
    public PacienteResponse obtenerPorId(Long id) {
        return toResponse(buscarEntidadPorId(id));
    }

    // ─── Métodos internos ─────────────────────────────────────────────

    public Paciente buscarEntidadPorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Paciente no encontrado con ID: " + id));
    }

    public Paciente buscarEntidadPorDni(String dni) {
        return pacienteRepository.findByDni(dni)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Paciente no encontrado con DNI: " + dni));
    }

    private PacienteResponse toResponse(Paciente p) {
        // RF-50: Cargar seguros vinculados al paciente
        List<PacienteResponse.SeguroPacienteDTO> seguros = pacienteSeguroRepository.findByPacienteAndActivoTrue(p)
                .stream()
                .map(ps -> PacienteResponse.SeguroPacienteDTO.builder()
                        .id(ps.getId()) // ← agregar esta línea
                        .seguroId(ps.getSeguro().getId())
                        .nombre(ps.getSeguro().getNombre())
                        .tipo(ps.getSeguro().getTipo())
                        .porcentajeCobertura(ps.getSeguro().getPorcentajeCobertura())
                        .deducible(ps.getSeguro().getDeducible())
                        .convenioActivo(ps.getSeguro().isConvenioActivo())
                        .activo(ps.isActivo())
                        .numeroPoliza(ps.getNumeroPoliza())
                        .build())
                .collect(Collectors.toList());

        return PacienteResponse.builder()
                .id(p.getId())
                .dni(p.getDni())
                .nombres(p.getNombres())
                .apellidos(p.getApellidos())
                .fechaNacimiento(p.getFechaNacimiento())
                .celular(p.getCelular())
                .correo(p.getCorreo())
                .sexo(p.getSexo())
                .creadoEn(p.getCreadoEn())
                .seguros(seguros)
                .build();
    }
}