package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.dto.atencion.ConsultaRequest;
import pe.edu.utp.clinica.dto.atencion.ConsultaResponse;
import pe.edu.utp.clinica.dto.atencion.TriajeRequest;
import pe.edu.utp.clinica.model.*;
import pe.edu.utp.clinica.repository.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de atención médica.
 *
 * RF-21: Solo se puede atender citas CONFIRMADAS.
 * RF-22: Registrar triaje con presión, temperatura y peso.
 * RF-23: Registrar consulta con diagnóstico y tratamiento.
 * RF-24: Asociar triaje y consulta a la cita.
 * RF-25: Impedir múltiples consultas por cita.
 * RF-26: Consultar historial médico del paciente.
 * RF-27: Historial ordenado por fecha descendente.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AtencionService {

    private final TriajeRepository triajeRepository;
    private final ConsultaMedicaRepository consultaRepository;
    private final CitaService citaService;
    private final PacienteService pacienteService;

    /**
     * Registra el triaje de un paciente.
     * RF-22: Solo si la cita está CONFIRMADA.
     *
     * @param request datos del triaje
     * @return triaje registrado
     */
    @Transactional
    public Triaje registrarTriaje(TriajeRequest request) {
        CitaMedica cita = citaService.buscarEntidadPorId(request.getCitaId());

        // RF-21: Solo citas CONFIRMADAS
        if (cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new IllegalStateException(
                    "Solo se puede registrar triaje en citas con estado CONFIRMADA.");
        }

        if (triajeRepository.existsByCita(cita)) {
            throw new IllegalStateException(
                    "Ya existe un triaje registrado para esta cita.");
        }

        Triaje triaje = Triaje.builder()
                .cita(cita)
                .presionArterial(request.getPresionArterial())
                .temperatura(request.getTemperatura())
                .peso(request.getPeso())
                .build();

        triaje = triajeRepository.save(triaje);
        log.debug("Triaje registrado para cita ID: {}", cita.getId());
        return triaje;
    }

    /**
     * Registra la consulta médica.
     * RF-23, RF-24, RF-25: Una sola consulta por cita.
     *
     * @param request datos de la consulta
     * @return consulta registrada
     */
    @Transactional
    public ConsultaResponse registrarConsulta(ConsultaRequest request) {
        CitaMedica cita = citaService.buscarEntidadPorId(request.getCitaId());

        // RF-21: Solo citas CONFIRMADAS
        if (cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new IllegalStateException(
                    "Solo se puede registrar una consulta en citas con estado CONFIRMADA.");
        }

        // RF-25: Impedir múltiples consultas
        if (consultaRepository.existsByCita(cita)) {
            throw new IllegalStateException(
                    "Ya existe una consulta médica registrada para esta cita.");
        }

        ConsultaMedica consulta = ConsultaMedica.builder()
                .cita(cita)
                .diagnostico(request.getDiagnostico())
                .tratamiento(request.getTratamiento())
                .observaciones(request.getObservaciones())
                .build();

        consulta = consultaRepository.save(consulta);

        // Marcar cita como ATENDIDA
        cita.setEstado(EstadoCita.ATENDIDA);

        log.debug("Consulta registrada para cita ID: {}", cita.getId());
        return toResponse(consulta);
    }

    /**
     * Consulta el historial médico de un paciente.
     * RF-26 y RF-27: Ordenado por fecha descendente.
     *
     * @param pacienteId ID del paciente
     * @return lista de consultas del paciente
     */
    @Transactional(readOnly = true)
    public List<ConsultaResponse> obtenerHistorial(Long pacienteId) {
        Paciente paciente = pacienteService.buscarEntidadPorId(pacienteId);

        List<ConsultaMedica> historial = consultaRepository
                .findByCitaPacienteOrderByCitaFechaHoraDesc(paciente);

        if (historial.isEmpty()) {
            log.debug("El paciente ID: {} no tiene consultas registradas", pacienteId);
        }

        return historial.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Métodos internos ─────────────────────────────────────────────

    private ConsultaResponse toResponse(ConsultaMedica c) {
        return ConsultaResponse.builder()
                .id(c.getId())
                .citaId(c.getCita().getId())
                .pacienteNombre(c.getCita().getPaciente().getNombres()
                        + " " + c.getCita().getPaciente().getApellidos())
                .medicoNombre(c.getCita().getMedico().getNombres()
                        + " " + c.getCita().getMedico().getApellidos())
                .fechaCita(c.getCita().getFechaHora())
                .diagnostico(c.getDiagnostico())
                .tratamiento(c.getTratamiento())
                .observaciones(c.getObservaciones())
                .registradoEn(c.getRegistradoEn())
                .build();
    }
}