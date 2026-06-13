package pe.edu.utp.clinica.service;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.EstadoPago;
import pe.edu.utp.clinica.common.enums.TipoAccion;
import pe.edu.utp.clinica.dto.cita.CitaRequest;
import pe.edu.utp.clinica.dto.cita.CitaReprogramarRequest;
import pe.edu.utp.clinica.dto.cita.CitaResponse;
import pe.edu.utp.clinica.model.*;
import pe.edu.utp.clinica.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de citas médicas.
 *
 * RF-05: Registrar cita en estado PENDIENTE.
 * RF-06: Reprogramar cita cambiando estado a REPROGRAMADA.
 * RF-07: Buscar citas por filtros.
 * RF-08: Listar citas con estado actual.
 * RF-09: Cancelar cita cambiando estado a CANCELADA.
 * RF-10: Validar disponibilidad del médico.
 * RF-11: Generar pago automático al registrar cita.
 * RF-41: Registrar auditoría en cada acción.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CitaService {

        private final CitaMedicaRepository citaRepository;
        private final PagoRepository pagoRepository;
        private final AuditoriaCitaRepository auditoriaRepository;
        private final NotificacionRepository notificacionRepository;
        private final PacienteService pacienteService;
        private final MedicoService medicoService;
        private final UsuarioRepository usuarioRepository;

        /** Monto base de consulta en la clínica */
        private static final BigDecimal MONTO_BASE = new BigDecimal("80.00");

        /**
         * Registra una nueva cita médica.
         * RF-05, RF-10, RF-11, RF-41, RF-44.
         *
         * @param request  datos de la cita
         * @param username usuario que realiza la acción
         * @return cita registrada
         */
        @Transactional
        public CitaResponse registrar(CitaRequest request, String username) {
                Paciente paciente = pacienteService.buscarEntidadPorId(request.getPacienteId());
                Medico medico = medicoService.buscarEntidadPorId(request.getMedicoId());
                Usuario usuario = buscarUsuario(username);

                // RF-10: Validar disponibilidad del médico
                if (citaRepository.existeConflictoHorario(medico, request.getFechaHora())) {
                        throw new IllegalStateException(
                                        "El médico no tiene disponibilidad en el horario seleccionado. "
                                                        + "Por favor elija otro horario.");
                }

                CitaMedica cita = CitaMedica.builder()
                                .paciente(paciente)
                                .medico(medico)
                                .fechaHora(request.getFechaHora())
                                .motivo(request.getMotivo())
                                .estado(EstadoCita.PENDIENTE)
                                .registradoPor(usuario)
                                .build();

                cita = citaRepository.save(cita);

                // RF-11: Generar pago automático en PENDIENTE
                Pago pago = Pago.builder()
                                .cita(cita)
                                .monto(MONTO_BASE)
                                .montoFinal(MONTO_BASE)
                                .estado(EstadoPago.PENDIENTE)
                                .build();
                pagoRepository.save(pago);

                // RF-41: Registrar auditoría de creación
                registrarAuditoria(cita, usuario, TipoAccion.CREACION, null, EstadoCita.PENDIENTE);

                // RF-44: Generar notificación de registro
                registrarNotificacion(paciente, cita, "REGISTRO",
                                "Su cita médica ha sido registrada para el "
                                                + request.getFechaHora().toLocalDate()
                                                + " a las " + request.getFechaHora().toLocalTime());

                log.debug("Cita registrada con ID: {}", cita.getId());
                return toResponse(cita);
        }

        /**
         * Reprograma una cita médica.
         * RF-06: Cambia estado a REPROGRAMADA. Registra en auditoría.
         *
         * @param id       ID de la cita
         * @param request  nueva fecha y hora
         * @param username usuario que realiza la acción
         * @return cita reprogramada
         */
        @Transactional
        public CitaResponse reprogramar(Long id, CitaReprogramarRequest request, String username) {
                CitaMedica cita = buscarEntidadPorId(id);
                Usuario usuario = buscarUsuario(username);

                if (cita.getEstado() == EstadoCita.CANCELADA) {
                        throw new IllegalStateException("No se puede reprogramar una cita cancelada.");
                }

                // RF-10: Validar disponibilidad en nuevo horario
                if (citaRepository.existeConflictoHorario(cita.getMedico(), request.getNuevaFechaHora())) {
                        throw new IllegalStateException(
                                        "El médico no tiene disponibilidad en el nuevo horario seleccionado.");
                }

                EstadoCita estadoAnterior = cita.getEstado();
                cita.setFechaHora(request.getNuevaFechaHora());
                cita.setEstado(EstadoCita.REPROGRAMADA);
                cita = citaRepository.save(cita);

                // RF-41: Auditoría
                registrarAuditoria(cita, usuario, TipoAccion.REPROGRAMACION,
                                estadoAnterior, EstadoCita.REPROGRAMADA);

                // RF-45: Notificación de reprogramación
                registrarNotificacion(cita.getPaciente(), cita, "REPROGRAMACION",
                                "Su cita ha sido reprogramada para el "
                                                + request.getNuevaFechaHora().toLocalDate()
                                                + " a las " + request.getNuevaFechaHora().toLocalTime());

                log.debug("Cita reprogramada ID: {}", id);
                return toResponse(cita);
        }

        /**
         * Cancela una cita médica.
         * RF-09: Cambia estado a CANCELADA. No permite doble cancelación.
         *
         * @param id       ID de la cita
         * @param username usuario que realiza la acción
         */
        @Transactional
        public CitaResponse cancelar(Long id, String username) {
                CitaMedica cita = buscarEntidadPorId(id);
                Usuario usuario = buscarUsuario(username);

                // RF-09: No se puede cancelar una cita ya cancelada
                if (cita.getEstado() == EstadoCita.CANCELADA) {
                        throw new IllegalStateException("La cita ya se encuentra cancelada.");
                }

                EstadoCita estadoAnterior = cita.getEstado();
                cita.setEstado(EstadoCita.CANCELADA);
                cita = citaRepository.save(cita);

                // RF-41: Auditoría
                registrarAuditoria(cita, usuario, TipoAccion.CANCELACION,
                                estadoAnterior, EstadoCita.CANCELADA);

                // RF-46: Notificación de cancelación
                registrarNotificacion(cita.getPaciente(), cita, "CANCELACION",
                                "Su cita médica del "
                                                + cita.getFechaHora().toLocalDate() + " ha sido cancelada.");

                log.debug("Cita cancelada ID: {}", id);
                return toResponse(cita);
        }

        /**
         * Confirma una cita al validar el pago.
         * RF-15: Estado cambia a CONFIRMADA.
         *
         * @param id       ID de la cita
         * @param username usuario que realiza la acción
         * @return cita confirmada
         */
        @Transactional
        public CitaResponse confirmar(Long id, String username) {
                CitaMedica cita = buscarEntidadPorId(id);
                Usuario usuario = buscarUsuario(username);

                EstadoCita estadoAnterior = cita.getEstado();
                cita.setEstado(EstadoCita.CONFIRMADA);
                cita = citaRepository.save(cita);

                // RF-41: Auditoría
                registrarAuditoria(cita, usuario, TipoAccion.CONFIRMACION,
                                estadoAnterior, EstadoCita.CONFIRMADA);

                log.debug("Cita confirmada ID: {}", id);
                return toResponse(cita);
        }

        /**
         * Lista todas las citas médicas.
         * RF-08: Muestra ID, fecha, hora y estado actual.
         */
        @Transactional(readOnly = true)
        public List<CitaResponse> listarTodas() {
                return citaRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Busca citas por filtros múltiples.
         * RF-07: Filtra por paciente, médico, fecha o estado.
         *
         * CORRECCIÓN: el parámetro fecha se convertía a LocalDateTime correctamente
         * y se pasaba al repository (antes siempre se enviaba null).
         *
         * @param pacienteId ID del paciente (opcional)
         * @param medicoId   ID del médico (opcional)
         * @param estado     estado de la cita (opcional)
         * @param fecha      fecha en formato yyyy-MM-dd (opcional)
         */
        @Transactional(readOnly = true)
        public List<CitaResponse> buscarPorFiltros(Long pacienteId, Long medicoId,
                        EstadoCita estado, String fecha) {
                // CORRECCIÓN: convertir String "yyyy-MM-dd" a LocalDateTime para el query
                // Antes: siempre se pasaba null al repository, ignorando el filtro de fecha
                LocalDateTime fechaDateTime = null;
                if (fecha != null && !fecha.isBlank()) {
                        fechaDateTime = java.time.LocalDate.parse(fecha).atStartOfDay();
                }

                return citaRepository.buscarPorFiltros(pacienteId, medicoId, estado, fechaDateTime)
                                .stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Obtiene una cita por su ID.
         */
        @Transactional(readOnly = true)
        public CitaResponse obtenerPorId(Long id) {
                return toResponse(buscarEntidadPorId(id));
        }

        // ─── Métodos internos ─────────────────────────────────────────────

        public CitaMedica buscarEntidadPorId(Long id) {
                return citaRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Cita no encontrada con ID: " + id));
        }

        private Usuario buscarUsuario(String username) {
                return usuarioRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Usuario no encontrado"));
        }

        private void registrarAuditoria(CitaMedica cita, Usuario usuario,
                        TipoAccion accion,
                        EstadoCita anterior, EstadoCita nuevo) {
                AuditoriaCita auditoria = AuditoriaCita.builder()
                                .cita(cita)
                                .usuario(usuario)
                                .tipoAccion(accion)
                                .estadoAnterior(anterior)
                                .estadoNuevo(nuevo)
                                .build();
                auditoriaRepository.save(auditoria);
        }

        private void registrarNotificacion(Paciente paciente, CitaMedica cita,
                        String tipo, String mensaje) {
                Notificacion notificacion = Notificacion.builder()
                                .paciente(paciente)
                                .cita(cita)
                                .tipo(tipo)
                                .mensaje(mensaje)
                                .estado("PENDIENTE")
                                .build();
                notificacionRepository.save(notificacion);
        }

        private CitaResponse toResponse(CitaMedica c) {
                return CitaResponse.builder()
                                .id(c.getId())
                                .pacienteId(c.getPaciente().getId())
                                .pacienteNombre(c.getPaciente().getNombres()
                                                + " " + c.getPaciente().getApellidos())
                                .pacienteDni(c.getPaciente().getDni())
                                .medicoId(c.getMedico().getId())
                                .medicoNombre(c.getMedico().getNombres()
                                                + " " + c.getMedico().getApellidos())
                                .especialidad(c.getMedico().getEspecialidad().getNombre())
                                .fechaHora(c.getFechaHora())
                                .motivo(c.getMotivo())
                                .estado(c.getEstado())
                                .creadoEn(c.getCreadoEn())
                                .build();
        }
}