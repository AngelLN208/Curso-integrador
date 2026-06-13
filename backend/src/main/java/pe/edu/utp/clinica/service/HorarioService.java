package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.dto.horario.HorarioRequest;
import pe.edu.utp.clinica.dto.horario.HorarioResponse;
import pe.edu.utp.clinica.model.HorarioMedico;
import pe.edu.utp.clinica.model.Medico;
import pe.edu.utp.clinica.repository.HorarioMedicoRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de horarios de médicos.
 *
 * RF-38: Asignar horarios indicando día, hora inicio y hora fin.
 *        No se permiten traslapes para un mismo médico.
 * RF-12: Base para mostrar disponibilidad al registrar cita.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HorarioService {

    private final HorarioMedicoRepository horarioRepository;
    private final MedicoService medicoService;

    /**
     * Asigna un horario a un médico.
     * RF-38: Valida que no haya traslape con horarios existentes.
     *
     * @param request datos del horario
     * @return horario asignado
     */
    @Transactional
    public HorarioResponse asignar(HorarioRequest request) {
        Medico medico = medicoService.buscarEntidadPorId(request.getMedicoId());

        // RF-38: Verificar traslape de horarios
        boolean hayTraslape = horarioRepository.existeTraslape(
                medico,
                request.getDia(),
                request.getHoraInicio(),
                request.getHoraFin()
        );

        if (hayTraslape) {
            throw new IllegalStateException(
                    "El médico ya tiene un horario asignado que se traslapa "
                    + "con el horario indicado en ese día");
        }

        if (request.getHoraInicio().isAfter(request.getHoraFin())) {
            throw new IllegalStateException(
                    "La hora de inicio no puede ser posterior a la hora de fin");
        }

        HorarioMedico horario = HorarioMedico.builder()
                .medico(medico)
                .dia(request.getDia())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .build();

        horario = horarioRepository.save(horario);
        log.debug("Horario asignado al médico ID: {}", medico.getId());

        return toResponse(horario);
    }

    /**
     * Lista los horarios de un médico.
     *
     * @param medicoId ID del médico
     */
    @Transactional(readOnly = true)
    public List<HorarioResponse> listarPorMedico(Long medicoId) {
        Medico medico = medicoService.buscarEntidadPorId(medicoId);
        return horarioRepository.findByMedico(medico)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Elimina un horario asignado.
     *
     * @param id ID del horario
     */
    @Transactional
    public void eliminar(Long id) {
        if (!horarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Horario no encontrado con ID: " + id);
        }
        horarioRepository.deleteById(id);
        log.debug("Horario eliminado con ID: {}", id);
    }

    // ─── Métodos internos ─────────────────────────────────────────────

    private HorarioResponse toResponse(HorarioMedico h) {
        return HorarioResponse.builder()
                .id(h.getId())
                .medicoId(h.getMedico().getId())
                .medicoNombre(h.getMedico().getNombres()
                        + " " + h.getMedico().getApellidos())
                .dia(h.getDia())
                .horaInicio(h.getHoraInicio())
                .horaFin(h.getHoraFin())
                .build();
    }
}