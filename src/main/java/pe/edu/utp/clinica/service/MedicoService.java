package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.common.enums.RolUsuario;
import pe.edu.utp.clinica.dto.medico.MedicoRequest;
import pe.edu.utp.clinica.dto.medico.MedicoResponse;
import pe.edu.utp.clinica.model.Especialidad;
import pe.edu.utp.clinica.model.Medico;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.repository.MedicoRepository;
import pe.edu.utp.clinica.repository.UsuarioRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de médicos.
 *
 * RF-37: Registrar médico con datos profesionales y especialidad.
 *        El DNI no puede estar duplicado.
 *        Solo el administrador puede realizar esta acción.
 * RF-38: Base para asignación de horarios.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadService especialidadService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo médico y crea su usuario del sistema.
     * RF-37: DNI único, asociado a una especialidad.
     * RNF-01: La contraseña se cifra con BCrypt.
     *
     * @param request datos del médico
     * @return médico registrado
     */
    @Transactional
    public MedicoResponse registrar(MedicoRequest request) {
        if (medicoRepository.existsByDni(request.getDni())) {
            throw new IllegalStateException(
                    "Ya existe un médico con el DNI: " + request.getDni());
        }

        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException(
                    "Ya existe un usuario con el username: " + request.getUsername());
        }

        Especialidad especialidad = especialidadService
                .buscarPorId(request.getEspecialidadId());

        // Crea el usuario del sistema para el médico
        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombreCompleto(request.getNombres() + " " + request.getApellidos())
                .rol(RolUsuario.ROLE_MEDICO)
                .build();

        usuario = usuarioRepository.save(usuario);

        Medico medico = Medico.builder()
                .dni(request.getDni())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .especialidad(especialidad)
                .celular(request.getCelular())
                .correo(request.getCorreo())
                .usuario(usuario)
                .build();

        medico = medicoRepository.save(medico);
        log.debug("Médico registrado con ID: {}", medico.getId());

        return toResponse(medico);
    }

    /**
     * Lista todos los médicos activos.
     */
    @Transactional(readOnly = true)
    public List<MedicoResponse> listarActivos() {
        return medicoRepository.findByActivoTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista médicos activos por especialidad.
     *
     * @param especialidadId ID de la especialidad
     */
    @Transactional(readOnly = true)
    public List<MedicoResponse> listarPorEspecialidad(Long especialidadId) {
        Especialidad especialidad = especialidadService.buscarPorId(especialidadId);
        return medicoRepository.findByEspecialidadAndActivoTrue(especialidad)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un médico por su ID.
     */
    @Transactional(readOnly = true)
    public MedicoResponse obtenerPorId(Long id) {
        return toResponse(buscarEntidadPorId(id));
    }

    /**
     * Desactiva un médico del sistema.
     */
    @Transactional
    public void desactivar(Long id) {
        Medico medico = buscarEntidadPorId(id);
        medico.setActivo(false);
        medicoRepository.save(medico);
        log.debug("Médico desactivado con ID: {}", id);
    }

    // ─── Métodos internos ─────────────────────────────────────────────

    public Medico buscarEntidadPorId(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Médico no encontrado con ID: " + id));
    }

    private MedicoResponse toResponse(Medico m) {
        return MedicoResponse.builder()
                .id(m.getId())
                .dni(m.getDni())
                .nombres(m.getNombres())
                .apellidos(m.getApellidos())
                .celular(m.getCelular())
                .correo(m.getCorreo())
                .especialidadId(m.getEspecialidad().getId())
                .especialidadNombre(m.getEspecialidad().getNombre())
                .activo(m.isActivo())
                .build();
    }
}