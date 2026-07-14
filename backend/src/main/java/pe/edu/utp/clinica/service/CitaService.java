package pe.edu.utp.clinica.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        private final ValoracionRepository valoracionRepository;

        /**
         * Registra una cita agendada por el propio paciente desde el portal.
         * RF-56 (extendido): el pacienteId se deriva del correo autenticado,
         * nunca se confía en un ID enviado por el cliente — así un paciente
         * no puede agendar una cita a nombre de otro.
         *
         * @param medicoId       médico seleccionado
         * @param fechaHora      fecha y hora de la cita
         * @param motivo         motivo opcional
         * @param correoPaciente correo del paciente autenticado (username del token)
         * @return cita registrada
         */
        @Transactional
        public CitaResponse registrarDesdePortal(Long medicoId, LocalDateTime fechaHora,
                        String motivo, String correoPaciente) {

                Paciente paciente = pacienteService.buscarPorCorreo(correoPaciente);
                Medico medico = medicoService.buscarEntidadPorId(medicoId);

                // RF-10: Validar disponibilidad del médico (hora exacta)
                if (citaRepository.existeConflictoHorario(medico, fechaHora)) {
                        log.warn("Conflicto de horario al agendar desde portal — médico ID: {} | fecha: {}",
                                        medicoId, fechaHora);
                        throw new IllegalStateException(
                                        "El médico no tiene disponibilidad en el horario seleccionado.");
                }

                LocalDateTime desde = fechaHora.minusMinutes(44);
                LocalDateTime hasta = fechaHora.plusMinutes(44);
                if (citaRepository.existeCitaCercana(medico, desde, hasta)) {
                        log.warn("Cita muy cercana a otra existente desde portal — médico ID: {} | fecha: {}",
                                        medicoId, fechaHora);
                        throw new IllegalStateException(
                                        "Debe haber al menos 45 minutos entre citas del mismo médico. "
                                                        + "Por favor elige otro horario.");
                }

                CitaMedica cita = CitaMedica.builder()
                                .paciente(paciente)
                                .medico(medico)
                                .fechaHora(fechaHora)
                                .motivo(motivo)
                                .estado(EstadoCita.PENDIENTE)
                                .build();

                cita = citaRepository.save(cita);

                // RF-11: el monto del pago es el costo real de la especialidad
                // del médico (antes estaba fijo en 80.00 para todas).
                BigDecimal montoBase = medico.getEspecialidad().getCosto();

                Pago pago = Pago.builder()
                                .cita(cita)
                                .monto(montoBase)
                                .montoFinal(montoBase)
                                .estado(EstadoPago.PENDIENTE)
                                .build();
                pagoRepository.save(pago);

                registrarAuditoria(cita, null, TipoAccion.CREACION, null, EstadoCita.PENDIENTE);

                registrarNotificacion(paciente, cita, "REGISTRO",
                                "Su cita médica ha sido registrada para el "
                                                + fechaHora.toLocalDate()
                                                + " a las " + fechaHora.toLocalTime());

                log.info("Cita registrada desde portal — cita ID: {} | médico ID: {}", cita.getId(), medicoId);
                return toResponse(cita);
        }

        /**
         * Lista todas las citas del paciente autenticado, sin importar estado.
         * RF-51 (extendido): sección "Mis citas" del portal — permite ver
         * historial completo (PENDIENTE, CONFIRMADA, ATENDIDA, CANCELADA).
         * El pacienteId se deriva del correo autenticado, igual que en
         * registrarDesdePortal, para que el paciente solo vea sus propias citas.
         *
         * @param correoPaciente correo del paciente autenticado (username del token)
         * @return lista de citas ordenadas por fecha descendente
         */
        @Transactional(readOnly = true)
        public List<CitaResponse> listarPorPaciente(String correoPaciente) {
                Paciente paciente = pacienteService.buscarPorCorreo(correoPaciente);

                return citaRepository.findByCitasPacienteId(paciente.getId())
                                .stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Cancela una cita agendada por el propio paciente desde el portal.
         * RF-09 (extendido): valida ownership — el paciente solo puede
         * cancelar sus propias citas, nunca las de otro paciente.
         *
         * @param citaId         ID de la cita a cancelar
         * @param correoPaciente correo del paciente autenticado (username del token)
         * @return cita cancelada
         */
        @Transactional
        public CitaResponse cancelarDesdePortal(Long citaId, String correoPaciente) {
                Paciente paciente = pacienteService.buscarPorCorreo(correoPaciente);
                CitaMedica cita = buscarEntidadPorId(citaId);

                // Validar ownership — la cita debe pertenecer al paciente autenticado
                if (!cita.getPaciente().getId().equals(paciente.getId())) {
                        log.warn("Intento de cancelar cita ajena desde portal — cita ID: {}", citaId);
                        throw new IllegalStateException(
                                        "No tienes permiso para cancelar esta cita.");
                }

                if (cita.getEstado() == EstadoCita.CANCELADA) {
                        throw new IllegalStateException("La cita ya se encuentra cancelada.");
                }

                if (cita.getEstado() == EstadoCita.ATENDIDA) {
                        throw new IllegalStateException("No se puede cancelar una cita ya atendida.");
                }

                EstadoCita estadoAnterior = cita.getEstado();
                cita.setEstado(EstadoCita.CANCELADA);
                cita = citaRepository.save(cita);

                registrarAuditoria(cita, null, TipoAccion.CANCELACION,
                                estadoAnterior, EstadoCita.CANCELADA);

                registrarNotificacion(paciente, cita, "CANCELACION",
                                "Su cita médica del "
                                                + cita.getFechaHora().toLocalDate() + " ha sido cancelada.");

                log.info("Cita cancelada desde portal — cita ID: {}", citaId);
                return toResponse(cita);
        }

        /**
         * Reprograma una cita agendada por el propio paciente desde el portal.
         * RF-06 (extendido): valida ownership y disponibilidad del médico
         * en el nuevo horario, igual que el flujo de recepcionista.
         *
         * @param citaId         ID de la cita a reprogramar
         * @param nuevaFechaHora nueva fecha y hora deseada
         * @param correoPaciente correo del paciente autenticado (username del token)
         * @return cita reprogramada
         */
        @Transactional
        public CitaResponse reprogramarDesdePortal(Long citaId, LocalDateTime nuevaFechaHora,
                        String correoPaciente) {
                Paciente paciente = pacienteService.buscarPorCorreo(correoPaciente);
                CitaMedica cita = buscarEntidadPorId(citaId);

                // Validar ownership
                if (!cita.getPaciente().getId().equals(paciente.getId())) {
                        log.warn("Intento de reprogramar cita ajena desde portal — cita ID: {}", citaId);
                        throw new IllegalStateException(
                                        "No tienes permiso para reprogramar esta cita.");
                }

                if (cita.getEstado() == EstadoCita.CANCELADA) {
                        throw new IllegalStateException("No se puede reprogramar una cita cancelada.");
                }

                if (cita.getEstado() == EstadoCita.ATENDIDA) {
                        throw new IllegalStateException("No se puede reprogramar una cita ya atendida.");
                }

                // RF-10: Validar disponibilidad en el nuevo horario
                if (citaRepository.existeConflictoHorario(cita.getMedico(), nuevaFechaHora)) {
                        log.warn("Conflicto de horario al reprogramar desde portal — cita ID: {} | nueva fecha: {}",
                                        citaId, nuevaFechaHora);
                        throw new IllegalStateException(
                                        "El médico no tiene disponibilidad en el nuevo horario seleccionado.");
                }

                LocalDateTime desde = nuevaFechaHora.minusMinutes(44);
                LocalDateTime hasta = nuevaFechaHora.plusMinutes(44);
                if (citaRepository.existeCitaCercana(cita.getMedico(), desde, hasta)) {
                        throw new IllegalStateException(
                                        "Debe haber al menos 45 minutos entre citas del mismo médico. "
                                                        + "Por favor elige otro horario.");
                }

                EstadoCita estadoAnterior = cita.getEstado();
                cita.setFechaHora(nuevaFechaHora);
                cita.setEstado(EstadoCita.REPROGRAMADA);
                cita = citaRepository.save(cita);

                registrarAuditoria(cita, null, TipoAccion.REPROGRAMACION,
                                estadoAnterior, EstadoCita.REPROGRAMADA);

                registrarNotificacion(paciente, cita, "REPROGRAMACION",
                                "Su cita ha sido reprogramada para el "
                                                + nuevaFechaHora.toLocalDate()
                                                + " a las " + nuevaFechaHora.toLocalTime());

                log.info("Cita reprogramada desde portal — cita ID: {}", citaId);
                return toResponse(cita);
        }

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

                // RF-10: Validar disponibilidad del médico (hora exacta)
                if (citaRepository.existeConflictoHorario(medico, request.getFechaHora())) {
                        log.warn("Conflicto de horario al registrar cita — médico ID: {} | fecha: {}",
                                        request.getMedicoId(), request.getFechaHora());
                        throw new IllegalStateException(
                                        "El médico no tiene disponibilidad en el horario seleccionado.");
                }

                // Separación mínima de 45 minutos entre citas del mismo médico
                LocalDateTime desde = request.getFechaHora().minusMinutes(44);
                LocalDateTime hasta = request.getFechaHora().plusMinutes(44);
                if (citaRepository.existeCitaCercana(medico, desde, hasta)) {
                        log.warn("Cita muy cercana a otra existente — médico ID: {} | fecha: {}",
                                        request.getMedicoId(), request.getFechaHora());
                        throw new IllegalStateException(
                                        "Debe haber al menos 45 minutos entre citas del mismo médico. "
                                                        + "Por favor elige otro horario.");
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

                // RF-11: Generar pago automático en PENDIENTE, con el costo
                // real de la especialidad del médico (antes fijo en 80.00).
                BigDecimal montoBase = medico.getEspecialidad().getCosto();

                Pago pago = Pago.builder()
                                .cita(cita)
                                .monto(montoBase)
                                .montoFinal(montoBase)
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

                log.info("Cita registrada — cita ID: {} | médico ID: {}", cita.getId(), request.getMedicoId());
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
                        log.warn("Conflicto de horario al reprogramar — cita ID: {} | nueva fecha: {}",
                                        id, request.getNuevaFechaHora());
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

                log.info("Cita reprogramada ID: {}", id);
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

                log.info("Cita cancelada ID: {}", id);
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

                log.info("Cita confirmada ID: {}", id);
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
         * CORRECCIÓN: se separó la consulta en dos métodos de repositorio
         * (buscarSinFecha / buscarConFecha) para evitar el bug de PostgreSQL
         * con parámetros de tipo fecha en null (errores 42846 y 42P18).
         * Así nunca se envía un parámetro de fecha null a la base de datos.
         *
         * @param pacienteId ID del paciente (opcional)
         * @param medicoId   ID del médico (opcional)
         * @param estado     estado de la cita (opcional)
         * @param fecha      fecha en formato yyyy-MM-dd (opcional)
         */
        @Transactional(readOnly = true)
        public List<CitaResponse> buscarPorFiltros(Long pacienteId, Long medicoId,
                        EstadoCita estado, String fecha) {
                List<CitaMedica> citas;

                if (fecha != null && !fecha.isBlank()) {
                        java.time.LocalDate dia = java.time.LocalDate.parse(fecha);
                        LocalDateTime fechaInicio = dia.atStartOfDay();
                        LocalDateTime fechaFin = dia.atTime(23, 59, 59);
                        citas = citaRepository.buscarConFecha(
                                        pacienteId, medicoId, estado, fechaInicio, fechaFin);
                } else {
                        citas = citaRepository.buscarSinFecha(pacienteId, medicoId, estado);
                }

                return citas.stream()
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
                // Solo consultamos valoración si la cita ya fue atendida —
                // evita una consulta innecesaria para el resto de estados.
                boolean valorada = c.getEstado() == EstadoCita.ATENDIDA
                                && valoracionRepository.existsByCita(c);

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
                                .yaValorada(valorada)
                                .build();
        }
}