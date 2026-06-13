package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.TipoAccion;
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
        private final CitaMedicaRepository citaMedicaRepository;
        private final AuditoriaCitaRepository auditoriaRepository;
        private final CitaService citaService;
        private final PacienteService pacienteService;

        /**
         * Registra el triaje de un paciente antes de la consulta.
         * RF-22: Solo si la cita está CONFIRMADA.
         *
         * @param request datos del triaje (citaId, presión, temperatura, peso)
         * @return entidad Triaje persistida
         * @throws IllegalStateException si la cita no está CONFIRMADA o ya tiene triaje
         */
        @Transactional
        public Triaje registrarTriaje(TriajeRequest request) {
                CitaMedica cita = citaService.buscarEntidadPorId(request.getCitaId());

                // RF-21: Solo citas CONFIRMADAS pueden iniciar atención
                if (cita.getEstado() != EstadoCita.CONFIRMADA) {
                        throw new IllegalStateException(
                                        "Solo se puede registrar triaje en citas con estado CONFIRMADA. " +
                                                        "Estado actual: " + cita.getEstado());
                }

                // RF-22: Solo un triaje por cita
                if (triajeRepository.existsByCita(cita)) {
                        throw new IllegalStateException(
                                        "Ya existe un triaje registrado para esta cita (ID: " + cita.getId() + ").");
                }

                Triaje triaje = Triaje.builder()
                                .cita(cita)
                                .presionArterial(request.getPresionArterial())
                                .temperatura(request.getTemperatura())
                                .peso(request.getPeso())
                                .build();

                triaje = triajeRepository.save(triaje);
                log.info("Triaje registrado — cita ID: {}", cita.getId());
                return triaje;
        }

        /**
         * Registra la consulta médica y marca la cita como ATENDIDA.
         *
         * RF-23: Registrar diagnóstico y tratamiento.
         * RF-24: Consulta queda asociada a la cita.
         * RF-25: Solo una consulta por cita.
         * BUG CORREGIDO: se agrega citaMedicaRepository.save(cita) para
         * persistir el cambio de estado a ATENDIDA en la base de datos.
         *
         * @param request datos de la consulta (citaId, diagnóstico, tratamiento)
         * @return DTO con los datos de la consulta registrada
         * @throws IllegalStateException si la cita no está CONFIRMADA o ya tiene
         *                               consulta
         */
        @Transactional
        public ConsultaResponse registrarConsulta(ConsultaRequest request) {
                CitaMedica cita = citaService.buscarEntidadPorId(request.getCitaId());

                // RF-21: Solo citas CONFIRMADAS
                if (cita.getEstado() != EstadoCita.CONFIRMADA) {
                        throw new IllegalStateException(
                                        "Solo se puede registrar una consulta en citas con estado CONFIRMADA. " +
                                                        "Estado actual: " + cita.getEstado());
                }

                // RF-25: Impedir múltiples consultas por cita
                if (consultaRepository.existsByCita(cita)) {
                        throw new IllegalStateException(
                                        "Ya existe una consulta médica registrada para esta cita (ID: " + cita.getId()
                                                        + ").");
                }

                // Guardar consulta
                ConsultaMedica consulta = ConsultaMedica.builder()
                                .cita(cita)
                                .diagnostico(request.getDiagnostico())
                                .tratamiento(request.getTratamiento())
                                .observaciones(request.getObservaciones())
                                .build();
                consulta = consultaRepository.save(consulta);

                // RF-23: Marcar cita como ATENDIDA y PERSISTIR el cambio
                // CORRECCIÓN: antes faltaba esta línea — el estado se cambiaba
                // en memoria pero nunca se guardaba en la base de datos.
                EstadoCita estadoAnterior = cita.getEstado();
                cita.setEstado(EstadoCita.ATENDIDA);
                citaMedicaRepository.save(cita); // ← línea crítica que faltaba

                // Registrar en auditoría el cambio de estado
                AuditoriaCita auditoria = AuditoriaCita.builder()
                                .cita(cita)
                                .tipoAccion(TipoAccion.ATENDIDA)
                                .estadoAnterior(estadoAnterior)
                                .estadoNuevo(EstadoCita.ATENDIDA)
                                .build();
                auditoriaRepository.save(auditoria);

                log.info("Consulta registrada — cita ID: {} → estado ATENDIDA", cita.getId());
                return toResponse(consulta);
        }

        /**
         * Obtiene el historial médico completo de un paciente.
         * RF-26: Lista todas las consultas del paciente.
         * RF-27: Ordenado de más reciente a más antiguo.
         *
         * @param pacienteId ID del paciente
         * @return lista de consultas ordenadas por fecha descendente
         */
        @Transactional(readOnly = true)
        public List<ConsultaResponse> obtenerHistorial(Long pacienteId) {
                Paciente paciente = pacienteService.buscarEntidadPorId(pacienteId);

                List<ConsultaMedica> historial = consultaRepository
                                .findByCitaPacienteOrderByCitaFechaHoraDesc(paciente);

                if (historial.isEmpty()) {
                        log.debug("Paciente ID: {} no tiene consultas registradas", pacienteId);
                }

                return historial.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        // ─── Métodos privados ─────────────────────────────────────────────────────

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