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
 * RNF-07: Ejecuta el procesamiento cada 60 segundos.
 * RF-47:  Envía recordatorio 24 horas antes de la cita.
 * RF-44 al 46: Procesa notificaciones pendientes de registro,
 *              reprogramación y cancelación.
 *
 * BUG CORREGIDO (RF-47): generarRecordatorios() cargaba TODAS las
 * notificaciones con estado ENVIADO en memoria para buscar duplicados
 * con stream().anyMatch(). Con el tiempo esa lista crece sin límite.
 * CORRECCIÓN: se agrega un método en NotificacionRepository que verifica
 * directamente en BD si ya existe un recordatorio para esa cita,
 * sin traer datos a memoria.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionScheduler {

    private final NotificacionRepository notificacionRepository;
    private final CitaMedicaRepository   citaRepository;

    /**
     * Procesa todas las notificaciones en estado PENDIENTE.
     * RNF-07: Se ejecuta cada 60 segundos.
     *
     * En producción aquí se integra el envío real por correo (RF-20).
     * Por ahora marca las notificaciones como ENVIADO y las registra en log.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void procesarNotificacionesPendientes() {
        List<Notificacion> pendientes = notificacionRepository
                .findByEstado("PENDIENTE");

        if (pendientes.isEmpty()) return;

        log.info("Procesando {} notificaciones pendientes", pendientes.size());

        for (Notificacion notificacion : pendientes) {
            try {
                // En producción: llamar aquí a JavaMailSender (RF-20)
                log.info("Notificación [{}] → paciente ID: {} — {}",
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
     *
     * BUG CORREGIDO: antes verificaba duplicados con findByEstado("ENVIADO")
     * + stream().anyMatch() — cargaba toda la tabla en memoria.
     * CORRECCIÓN: usa existsByCitaIdAndTipo() que ejecuta un COUNT en BD.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void generarRecordatorios() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin = LocalDateTime.now().plusDays(1)
                .withHour(23).withMinute(59).withSecond(59).withNano(0);

        List<CitaMedica> citasManana = citaRepository
                .findCitasParaRecordatorio(inicio, fin);

        log.info("Generando recordatorios para {} citas de mañana",
                citasManana.size());

        for (CitaMedica cita : citasManana) {
            Paciente paciente = cita.getPaciente();

            // CORRECCIÓN: consulta directa en BD — sin cargar tabla en memoria
            boolean yaNotificado = notificacionRepository
                    .existsByCitaIdAndTipo(cita.getId(), "RECORDATORIO");

            if (!yaNotificado) {
                Notificacion recordatorio = Notificacion.builder()
                        .paciente(paciente)
                        .cita(cita)
                        .tipo("RECORDATORIO")
                        .mensaje("Recordatorio: tiene una cita médica mañana "
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