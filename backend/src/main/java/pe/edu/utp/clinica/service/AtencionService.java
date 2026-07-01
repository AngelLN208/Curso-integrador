package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.TipoAccion;
import pe.edu.utp.clinica.dto.atencion.ConsultaRequest;
import pe.edu.utp.clinica.dto.atencion.ConsultaResponse;
import pe.edu.utp.clinica.dto.atencion.ConsultaEditarRequest;
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
 * Edición de consultas: solo el médico que atendió la cita puede editar
 * diagnóstico/tratamiento, y solo dentro de los 45 minutos posteriores
 * a la hora programada de la cita. Cada edición queda registrada en
 * auditoria_consultas con los valores anteriores y nuevos.
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
        private final AuditoriaConsultaRepository auditoriaConsultaRepository;
        private final CitaService citaService;
        private final PacienteService pacienteService;
        private final MedicoRepository medicoRepository;
        private final UsuarioRepository usuarioRepository;
        private final NotificacionRepository notificacionRepository;

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

                if (cita.getEstado() != EstadoCita.CONFIRMADA) {
                        throw new IllegalStateException(
                                        "Solo se puede registrar triaje en citas con estado CONFIRMADA. " +
                                                        "Estado actual: " + cita.getEstado());
                }

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
         *
         * @param request datos de la consulta (citaId, diagnóstico, tratamiento)
         * @return DTO con los datos de la consulta registrada
         * @throws IllegalStateException si la cita no está CONFIRMADA o ya tiene
         *                               consulta
         */
        @Transactional
        public ConsultaResponse registrarConsulta(ConsultaRequest request) {
                CitaMedica cita = citaService.buscarEntidadPorId(request.getCitaId());

                if (cita.getEstado() != EstadoCita.CONFIRMADA) {
                        throw new IllegalStateException(
                                        "Solo se puede registrar una consulta en citas con estado CONFIRMADA. " +
                                                        "Estado actual: " + cita.getEstado());
                }

                if (consultaRepository.existsByCita(cita)) {
                        throw new IllegalStateException(
                                        "Ya existe una consulta médica registrada para esta cita (ID: " + cita.getId()
                                                        + ").");
                }

                ConsultaMedica consulta = ConsultaMedica.builder()
                                .cita(cita)
                                .diagnostico(request.getDiagnostico())
                                .tratamiento(request.getTratamiento())
                                .observaciones(request.getObservaciones())
                                .build();
                consulta = consultaRepository.save(consulta);

                EstadoCita estadoAnterior = cita.getEstado();
                cita.setEstado(EstadoCita.ATENDIDA);
                citaMedicaRepository.save(cita);

                AuditoriaCita auditoria = AuditoriaCita.builder()
                                .cita(cita)
                                .tipoAccion(TipoAccion.ATENDIDA)
                                .estadoAnterior(estadoAnterior)
                                .estadoNuevo(EstadoCita.ATENDIDA)
                                .build();
                auditoriaRepository.save(auditoria);

                // RF-20: Notificar al paciente que su consulta fue registrada
                Notificacion notifAtendida = Notificacion.builder()
                                .paciente(cita.getPaciente())
                                .cita(cita)
                                .tipo("CONSULTA_REGISTRADA")
                                .mensaje("Su consulta médica del "
                                                + cita.getFechaHora().toLocalDate()
                                                + " con el Dr. " + cita.getMedico().getNombres()
                                                + " " + cita.getMedico().getApellidos()
                                                + " ha sido registrada. Puede ver su diagnóstico en el historial médico.")
                                .estado("PENDIENTE")
                                .build();
                notificacionRepository.save(notifAtendida);

                log.info("Consulta registrada — cita ID: {} → estado ATENDIDA", cita.getId());
                return toResponse(consulta);
        }

        /**
         * Edita una consulta médica ya registrada.
         * Solo permitido si:
         * - El médico autenticado es el mismo que registró la consulta original.
         * - Está dentro de los 45 minutos posteriores a la hora de la cita.
         * Registra el cambio en auditoria_consultas con valores antes/después.
         *
         * @param citaId   ID de la cita cuya consulta se va a editar
         * @param request  nuevos valores de diagnóstico/tratamiento/observaciones
         * @param username usuario autenticado que realiza la edición
         * @return DTO actualizado de la consulta
         */
        @Transactional
        public ConsultaResponse editarConsulta(Long citaId, ConsultaEditarRequest request, String username) {
                CitaMedica cita = citaService.buscarEntidadPorId(citaId);

                ConsultaMedica consulta = consultaRepository.findByCita(cita)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No existe una consulta registrada para la cita ID: " + citaId));

                Usuario usuarioAutenticado = usuarioRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                Medico medicoAutenticado = medicoRepository.findByUsuario(usuarioAutenticado)
                                .orElseThrow(() -> new IllegalStateException(
                                                "El usuario autenticado no tiene un perfil de médico asociado."));

                if (!cita.getMedico().getId().equals(medicoAutenticado.getId())) {
                        throw new IllegalStateException(
                                        "Solo el médico que atendió esta cita puede editar la consulta.");
                }

                if (!esEditable(cita)) {
                        throw new IllegalStateException(
                                        "El tiempo para editar esta consulta ha expirado. "
                                                        + "Solo se permite editar hasta 45 minutos después de la hora de la cita.");
                }

                String diagnosticoAnterior = consulta.getDiagnostico();
                String tratamientoAnterior = consulta.getTratamiento();

                consulta.setDiagnostico(request.getDiagnostico());
                consulta.setTratamiento(request.getTratamiento());
                consulta.setObservaciones(request.getObservaciones());
                consulta = consultaRepository.save(consulta);

                AuditoriaConsulta auditoria = AuditoriaConsulta.builder()
                                .consulta(consulta)
                                .medico(medicoAutenticado)
                                .diagnosticoAnterior(diagnosticoAnterior)
                                .diagnosticoNuevo(request.getDiagnostico())
                                .tratamientoAnterior(tratamientoAnterior)
                                .tratamientoNuevo(request.getTratamiento())
                                .build();
                auditoriaConsultaRepository.save(auditoria);

                log.info("Consulta editada — cita ID: {} por médico ID: {}", citaId, medicoAutenticado.getId());
                return toResponse(consulta);
        }

        /**
         * Edita el triaje de una cita ya registrado.
         * Misma regla que editarConsulta: solo el médico que atendió la cita,
         * y solo dentro de los 45 minutos posteriores a la hora de la cita.
         *
         * @param citaId   ID de la cita cuyo triaje se va a editar
         * @param request  nuevos valores de presión/temperatura/peso
         * @param username usuario autenticado que realiza la edición
         * @return entidad Triaje actualizada
         */
        @Transactional
        public Triaje editarTriaje(Long citaId, pe.edu.utp.clinica.dto.atencion.TriajeEditarRequest request,
                        String username) {
                CitaMedica cita = citaService.buscarEntidadPorId(citaId);

                Triaje triaje = triajeRepository.findByCita(cita)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No existe un triaje registrado para la cita ID: " + citaId));

                Usuario usuarioAutenticado = usuarioRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                Medico medicoAutenticado = medicoRepository.findByUsuario(usuarioAutenticado)
                                .orElseThrow(() -> new IllegalStateException(
                                                "El usuario autenticado no tiene un perfil de médico asociado."));

                if (!cita.getMedico().getId().equals(medicoAutenticado.getId())) {
                        throw new IllegalStateException(
                                        "Solo el médico que atendió esta cita puede editar el triaje.");
                }

                if (!esEditable(cita)) {
                        throw new IllegalStateException(
                                        "El tiempo para editar este triaje ha expirado. "
                                                        + "Solo se permite editar hasta 45 minutos después de la hora de la cita.");
                }

                triaje.setPresionArterial(request.getPresionArterial());
                triaje.setTemperatura(request.getTemperatura());
                triaje.setPeso(request.getPeso());
                triaje = triajeRepository.save(triaje);

                log.info("Triaje editado — cita ID: {} por médico ID: {}", citaId, medicoAutenticado.getId());
                return triaje;
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
                Triaje triaje = triajeRepository.findByCita(c.getCita()).orElse(null);
                boolean editable = esEditable(c.getCita());

                return ConsultaResponse.builder()
                                .id(c.getId())
                                .citaId(c.getCita().getId())
                                .medicoId(c.getCita().getMedico().getId())
                                .pacienteNombre(c.getCita().getPaciente().getNombres()
                                                + " " + c.getCita().getPaciente().getApellidos())
                                .medicoNombre(c.getCita().getMedico().getNombres()
                                                + " " + c.getCita().getMedico().getApellidos())
                                .fechaCita(c.getCita().getFechaHora())
                                .diagnostico(c.getDiagnostico())
                                .tratamiento(c.getTratamiento())
                                .observaciones(c.getObservaciones())
                                .registradoEn(c.getRegistradoEn())
                                .presionArterial(triaje != null ? triaje.getPresionArterial() : null)
                                .temperatura(triaje != null ? triaje.getTemperatura() : null)
                                .peso(triaje != null ? triaje.getPeso() : null)
                                .editable(editable)
                                .build();
        }

        /**
         * Determina si una consulta aún puede editarse.
         * Regla de negocio: solo dentro de los 45 minutos posteriores a la
         * hora programada de la cita (misma ventana usada para separar citas).
         */
        private boolean esEditable(CitaMedica cita) {
                java.time.LocalDateTime limite = cita.getFechaHora().plusMinutes(45);
                return java.time.LocalDateTime.now().isBefore(limite);
        }
}