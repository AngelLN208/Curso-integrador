package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.model.CitaMedica;
import pe.edu.utp.clinica.model.Notificacion;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.repository.CitaMedicaRepository;
import pe.edu.utp.clinica.repository.NotificacionRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler para procesamiento automático de notificaciones.
 *
 * RNF-07: Ejecuta el procesamiento cada 60 segundos como máximo.
 * RF-47: Envía recordatorio 24 horas antes de la cita.
 * RF-44 al 46: Procesa notificaciones pendientes de registro,
 *              reprogramación y cancelación.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionScheduler {

    private final NotificacionRepository notificacionRepository;
    private final CitaMedicaRepository citaRepository;

    /**
     * Procesa todas las notificaciones en estado PENDIENTE.
     * RNF-07: Se ejecuta cada 60 segundos.
     *
     * En producción aquí se integraría el envío real por correo.
     * Por ahora marca las notificaciones como ENVIADO y las registra en el log.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void procesarNotificacionesPendientes() {
        List<Notificacion> pendientes = notificacionRepository
                .findByEstado("PENDIENTE");

        if (pendientes.isEmpty()) {
            return;
        }

        log.info("Procesando {} notificaciones pendientes", pendientes.size());

        for (Notificacion notificacion : pendientes) {
            try {
                // Simula el envío — en producción aquí va el servicio de correo
                log.info("Notificación [{}] enviada al paciente ID: {} — Mensaje: {}",
                        notificacion.getTipo(),
                        notificacion.getPaciente().getId(),
                        notificacion.getMensaje());

                notificacion.setEstado("ENVIADO");
                notificacion.setEnviadoEn(LocalDateTime.now());
                notificacionRepository.save(notificacion);

            } catch (Exception ex) {
                log.error("Error al procesar notificación ID: {} — {}",
                        notificacion.getId(), ex.getMessage());
            }
        }
    }

    /**
     * Genera recordatorios para citas del día siguiente.
     * RF-47: Se ejecuta una vez al día a las 8:00 AM.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void generarRecordatorios() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1)
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime fin = LocalDateTime.now().plusDays(1)
                .withHour(23).withMinute(59).withSecond(59);

        List<CitaMedica> citasManana = citaRepository
                .findCitasParaRecordatorio(inicio, fin);

        log.info("Generando recordatorios para {} citas de mañana", citasManana.size());

        for (CitaMedica cita : citasManana) {
            Paciente paciente = cita.getPaciente();

            // Verifica que no se haya enviado ya un recordatorio para esta cita
            boolean yaNotificado = notificacionRepository
                    .findByEstado("ENVIADO")
                    .stream()
                    .anyMatch(n -> n.getCita() != null
                            && n.getCita().getId().equals(cita.getId())
                            && "RECORDATORIO".equals(n.getTipo()));

            if (!yaNotificado) {
                Notificacion recordatorio = Notificacion.builder()
                        .paciente(paciente)
                        .cita(cita)
                        .tipo("RECORDATORIO")
                        .mensaje("Recordatorio: Tiene una cita médica mañana "
                                + cita.getFechaHora().toLocalDate()
                                + " a las " + cita.getFechaHora().toLocalTime()
                                + " con el Dr. " + cita.getMedico().getNombres()
                                + " " + cita.getMedico().getApellidos())
                        .estado("PENDIENTE")
                        .build();

                notificacionRepository.save(recordatorio);
                log.debug("Recordatorio generado para cita ID: {}", cita.getId());
            }
        }
    }
}