package pe.edu.utp.clinica.service;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.model.CitaMedica;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import pe.edu.utp.clinica.model.CitaMedica;
import pe.edu.utp.clinica.model.Notificacion;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.repository.CitaMedicaRepository;
import pe.edu.utp.clinica.repository.NotificacionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;

/**
 * Scheduler para procesamiento automático de notificaciones.
 *
 * RNF-07: Ejecuta el procesamiento cada 60 segundos.
 * RF-47: Envía recordatorio 24 horas antes de la cita.
 * RF-44 al 46: Procesa notificaciones pendientes de registro,
 * reprogramación y cancelación.
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
        private final CitaMedicaRepository citaRepository;
        private final JavaMailSender mailSender;

        @Value("${spring.mail.username}")
        private String correoRemitente;

        private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

        private final PagoService pagoService;

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

                if (pendientes.isEmpty())
                        return;

                log.info("Procesando {} notificaciones pendientes", pendientes.size());

                for (Notificacion notificacion : pendientes) {
                        try {
                                enviarCorreo(notificacion);

                                notificacion.setEstado("ENVIADO");
                                notificacion.setEnviadoEn(LocalDateTime.now());
                                notificacionRepository.save(notificacion);

                                log.info("Notificación [{}] enviada → paciente ID: {} — {}",
                                                notificacion.getTipo(),
                                                notificacion.getPaciente().getId(),
                                                notificacion.getMensaje());

                        } catch (Exception ex) {
                                log.error("Error al procesar notificación ID: {} — {}",
                                                notificacion.getId(), ex.getMessage());
                                // No relanzamos la excepción — una notificación fallida
                                // no debe detener el procesamiento de las demás
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

        /**
         * Cancela automáticamente las citas que pasaron su hora + 15 min de tolerancia.
         * Se ejecuta cada 5 minutos para mantener los estados actualizados.
         *
         * Ejemplo: cita a las 15:00 → se cancela automáticamente a las 15:15
         * si aún está en PENDIENTE o CONFIRMADA.
         */
        // @Scheduled(fixedDelay = 300_000) // DESACTIVADO — cancelación manual para
        // proyecto académico
        @Transactional
        public void cancelarCitasVencidas() {
                // Límite = ahora - 15 minutos de tolerancia
                LocalDateTime limite = LocalDateTime.now().minusMinutes(15);

                List<CitaMedica> vencidas = citaRepository.findCitasVencidas(limite);

                if (vencidas.isEmpty())
                        return;

                log.info("Cancelando {} citas vencidas (tolerancia 15 min)", vencidas.size());

                for (CitaMedica cita : vencidas) {
                        try {
                                EstadoCita estadoAnterior = cita.getEstado();
                                cita.setEstado(EstadoCita.CANCELADA);
                                citaRepository.save(cita);

                                // Notificar al paciente
                                Notificacion notif = Notificacion.builder()
                                                .paciente(cita.getPaciente())
                                                .cita(cita)
                                                .tipo("CANCELACION_AUTOMATICA")
                                                .mensaje("Tu cita del "
                                                                + cita.getFechaHora().toLocalDate()
                                                                + " a las " + cita.getFechaHora().toLocalTime()
                                                                + " fue cancelada automáticamente por inasistencia.")
                                                .estado("PENDIENTE")
                                                .build();
                                notificacionRepository.save(notif);

                                log.info("Cita ID: {} cancelada automáticamente — estado anterior: {}",
                                                cita.getId(), estadoAnterior);

                        } catch (Exception ex) {
                                log.error("Error al cancelar cita ID: {} — {}", cita.getId(), ex.getMessage());
                        }
                }
        }

        /**
         * Envía el correo real al paciente según el tipo de notificación.
         * RF-20: Notificaciones por correo electrónico.
         * RF-44: Confirmación de registro de cita.
         * RF-45: Aviso de reprogramación.
         * RF-46: Aviso de cancelación.
         * RF-47: Recordatorio 24 horas antes.
         */
        private void enviarCorreo(Notificacion notificacion) {
                Paciente paciente = notificacion.getPaciente();
                String correoDestino = paciente.getCorreo();
                String nombrePaciente = paciente.getNombres() + " " + paciente.getApellidos();

                String asunto;
                String cuerpo;

                // Armar asunto y cuerpo según el tipo de notificación
                switch (notificacion.getTipo()) {
                        case "REGISTRO" -> {
                                CitaMedica cita = notificacion.getCita();
                                String fecha = cita.getFechaHora().format(FMT_FECHA);
                                String hora = cita.getFechaHora().format(FMT_HORA);
                                String medico = "Dr(a). " + cita.getMedico().getNombres()
                                                + " " + cita.getMedico().getApellidos();
                                String especialidad = cita.getMedico().getEspecialidad().getNombre();

                                asunto = "✅ Cita registrada — Clínica Stella Maris";
                                cuerpo = """
                                                Estimado(a) %s,

                                                Su cita médica ha sido registrada exitosamente.

                                                📋 DETALLES DE LA CITA
                                                ─────────────────────────────
                                                Médico:       %s
                                                Especialidad: %s
                                                Fecha:        %s
                                                Hora:         %s
                                                Costo:        S/ 80.00 (puede variar según seguro)

                                                ℹ️  Su cita quedará CONFIRMADA una vez registrado el pago.
                                                Puede gestionar su cita desde el portal de pacientes.

                                                ─────────────────────────────
                                                Clínica Stella Maris
                                                Tel: (01) 234-5678
                                                Horario: Lunes a Sábado, 7:00 AM – 8:00 PM
                                                """.formatted(nombrePaciente, medico, especialidad, fecha, hora);
                        }
                        case "REPROGRAMACION" -> {
                                CitaMedica cita = notificacion.getCita();
                                String fecha = cita.getFechaHora().format(FMT_FECHA);
                                String hora = cita.getFechaHora().format(FMT_HORA);

                                asunto = "🔄 Cita reprogramada — Clínica Stella Maris";
                                cuerpo = """
                                                Estimado(a) %s,

                                                Su cita médica ha sido reprogramada.

                                                📋 NUEVA FECHA Y HORA
                                                ─────────────────────────────
                                                Fecha: %s
                                                Hora:  %s

                                                Si no solicitó este cambio, comuníquese con nosotros al (01) 234-5678.

                                                ─────────────────────────────
                                                Clínica Stella Maris
                                                """.formatted(nombrePaciente, fecha, hora);
                        }
                        case "CANCELACION", "CANCELACION_AUTOMATICA" -> {
                                asunto = "❌ Cita cancelada — Clínica Stella Maris";
                                cuerpo = """
                                                Estimado(a) %s,

                                                Le informamos que su cita médica ha sido cancelada.

                                                %s

                                                Si desea agendar una nueva cita, puede hacerlo desde el
                                                portal de pacientes o llamando al (01) 234-5678.

                                                ─────────────────────────────
                                                Clínica Stella Maris
                                                """.formatted(nombrePaciente, notificacion.getMensaje());
                        }
                        case "RECORDATORIO" -> {
                                CitaMedica cita = notificacion.getCita();
                                String fecha = cita.getFechaHora().format(FMT_FECHA);
                                String hora = cita.getFechaHora().format(FMT_HORA);
                                String medico = "Dr(a). " + cita.getMedico().getNombres()
                                                + " " + cita.getMedico().getApellidos();

                                asunto = "🔔 Recordatorio de cita mañana — Clínica Stella Maris";
                                cuerpo = """
                                                Estimado(a) %s,

                                                Le recordamos que tiene una cita médica mañana.

                                                📋 DETALLES
                                                ─────────────────────────────
                                                Médico: %s
                                                Fecha:  %s
                                                Hora:   %s

                                                Por favor llegue 10 minutos antes de su cita.
                                                Si no puede asistir, cancele desde el portal con anticipación.

                                                ─────────────────────────────
                                                Clínica Stella Maris
                                                Tel: (01) 234-5678
                                                """.formatted(nombrePaciente, medico, fecha, hora);
                        }
                        case "PAGO_CONFIRMADO" -> {
                                CitaMedica cita = notificacion.getCita();
                                String fecha = cita.getFechaHora().format(FMT_FECHA);
                                String hora = cita.getFechaHora().format(FMT_HORA);
                                String medico = "Dr(a). " + cita.getMedico().getNombres()
                                                + " " + cita.getMedico().getApellidos();

                                asunto = "💳 Pago confirmado — Clínica Stella Maris";
                                cuerpo = """
                                                Estimado(a) %s,

                                                Su pago ha sido registrado exitosamente y su cita queda CONFIRMADA.

                                                📋 DETALLES
                                                ─────────────────────────────
                                                Médico: %s
                                                Fecha:  %s
                                                Hora:   %s
                                                %s

                                                Recuerde llegar 10 minutos antes de su cita.

                                                ─────────────────────────────
                                                Clínica Stella Maris
                                                Tel: (01) 234-5678
                                                """.formatted(nombrePaciente, medico, fecha, hora,
                                                notificacion.getMensaje());
                        }
                        case "CONSULTA_REGISTRADA" -> {
                                asunto = "📋 Consulta registrada — Clínica Stella Maris";
                                cuerpo = """
                                                Estimado(a) %s,

                                                %s

                                                Puede descargar su historial médico completo desde el
                                                portal de pacientes en cualquier momento.

                                                ─────────────────────────────
                                                Clínica Stella Maris
                                                Tel: (01) 234-5678
                                                """.formatted(nombrePaciente, notificacion.getMensaje());
                        }
                        default -> {
                                asunto = "Notificación — Clínica Stella Maris";
                                cuerpo = "Estimado(a) " + nombrePaciente + ",\n\n"
                                                + notificacion.getMensaje() + "\n\n"
                                                + "— Clínica Stella Maris";
                        }
                }

                // Si es pago confirmado, adjuntar la boleta PDF
                if ("PAGO_CONFIRMADO".equals(notificacion.getTipo()) && notificacion.getCita() != null) {
                        try {
                                MimeMessage mimeMsg = mailSender.createMimeMessage();
                                MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, true, "UTF-8");
                                helper.setFrom("Clínica Stella Maris <" + correoRemitente + ">");
                                helper.setTo(correoDestino);
                                helper.setSubject(asunto);
                                helper.setText(cuerpo);

                                // Buscar el pago de la cita para generar la boleta
                                pe.edu.utp.clinica.repository.PagoRepository pagoRepo = pagoService.getPagoRepository();
                                pe.edu.utp.clinica.model.Pago pago = pagoRepo
                                                .findByCita(notificacion.getCita()).orElse(null);

                                if (pago != null) {
                                        byte[] pdfBytes = pagoService.generarBoletaPdf(pago);
                                        helper.addAttachment("boleta-pago.pdf",
                                                        new org.springframework.core.io.ByteArrayResource(pdfBytes));
                                }

                                mailSender.send(mimeMsg);
                        } catch (Exception e) {
                                log.error("Error enviando correo con adjunto: {}", e.getMessage());
                                // Fallback: enviar sin adjunto
                                SimpleMailMessage simple = new SimpleMailMessage();
                                simple.setFrom("Clínica Stella Maris <" + correoRemitente + ">");
                                simple.setTo(correoDestino);
                                simple.setSubject(asunto);
                                simple.setText(cuerpo);
                                mailSender.send(simple);
                        }
                } else {
                        SimpleMailMessage mensaje = new SimpleMailMessage();
                        mensaje.setFrom("Clínica Stella Maris <" + correoRemitente + ">");
                        mensaje.setTo(correoDestino);
                        mensaje.setSubject(asunto);
                        mensaje.setText(cuerpo);
                        mailSender.send(mensaje);
                }
                log.debug("Correo enviado a {} — asunto: {}", correoDestino, asunto);
        }
}