package pe.edu.utp.clinica.service;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.model.CitaMedica;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Scheduler para procesamiento automático de notificaciones.
 *
 * RNF-07: Ejecuta el procesamiento cada 60 segundos.
 * RF-47: Envía recordatorio 24 horas antes de la cita.
 * RF-44 al 46: Procesa notificaciones pendientes de registro,
 * reprogramación y cancelación.
 * RF-20: Notificaciones por correo electrónico con plantilla HTML
 * (identidad visual Stella Maris — header navy, tarjeta central, footer).
 *
 * NOTA DE DESPLIEGUE: el envío de correo se realiza vía la API HTTPS de
 * Brevo (EmailApiService), no vía SMTP directo. Esto se debe a que el
 * hosting gratuito de Render bloquea las conexiones salientes a los
 * puertos SMTP estándar (587 y 465), confirmado durante las pruebas de
 * despliegue — ver Plan de Despliegue.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionScheduler {

        private final NotificacionRepository notificacionRepository;
        private final CitaMedicaRepository citaRepository;
        private final EmailApiService emailApiService;

        @Value("${app.portal.paciente.url}")
        private String portalPacienteUrl;

        @Value("${app.portal.staff.url}")
        private String portalStaffUrl;

        private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

        // Paleta Stella Maris (misma que Tailwind, en hex — los correos no soportan CSS
        // externo)
        private static final String COLOR_TINTA = "#14213D";
        private static final String COLOR_TINTA_CLARO = "#B9C2D6";
        private static final String COLOR_GUIA = "#FF7A45";
        private static final String COLOR_GUIA_BG = "#FFF4EE";
        private static final String COLOR_GUIA_TEXTO = "#993C1D";
        private static final String COLOR_RUMBO = "#2F9E6E";
        private static final String COLOR_RUMBO_BG = "#EAF7F1";
        private static final String COLOR_RUMBO_TEXTO = "#1F6E4C";
        private static final String COLOR_ALERTA = "#E5484D";
        private static final String COLOR_ALERTA_BG = "#FDEEEE";
        private static final String COLOR_ALERTA_TEXTO = "#A72E32";
        private static final String COLOR_NEBLINA = "#8A94A6";
        private static final String COLOR_LIENZO = "#F7F8FA";
        private static final String COLOR_BORDE = "#E4E7EC";

        private final PagoService pagoService;

        /**
         * Procesa todas las notificaciones en estado PENDIENTE.
         * RNF-07: Se ejecuta cada 60 segundos.
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
         */
        // @Scheduled(fixedDelay = 300_000) // DESACTIVADO — cancelación manual para
        // proyecto académico
        @Transactional
        public void cancelarCitasVencidas() {
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

        // ==========================================================
        // PLANTILLA HTML — helpers de construcción
        // ==========================================================

        private String plantillaCorreo(String subtituloHeader, String nombrePaciente, String introHtml,
                        String tablaHtml, String notaHtml, String ctaTexto, String ctaUrl) {

                String cta = (ctaTexto == null) ? ""
                                : """
                                                <div style="text-align:center; margin-bottom:6px;">
                                                  <a href="%s" style="display:inline-block; background:%s; color:#FFFFFF; font-size:13px; font-weight:bold; padding:10px 22px; border-radius:6px; text-decoration:none;">%s</a>
                                                </div>
                                                """
                                                .formatted(ctaUrl, COLOR_TINTA, ctaTexto);

                return """
                                <div style="background:%s; padding:32px 16px; font-family:Arial, Helvetica, sans-serif;">
                                <div style="max-width:480px; margin:0 auto; background:#FFFFFF; border-radius:10px; overflow:hidden;">

                                  <div style="background:%s; padding:28px 24px; text-align:center;">
                                    <div style="color:#FFFFFF; font-size:18px; font-weight:bold;">Clínica Stella Maris</div>
                                    <div style="color:%s; font-size:12px; margin-top:4px;">%s</div>
                                  </div>

                                  <div style="padding:24px;">
                                    <p style="font-size:14px; color:%s; margin:0 0 4px; line-height:1.5;">Estimado(a) <strong>%s</strong>,</p>
                                    <p style="font-size:13px; color:%s; line-height:1.6; margin:0 0 20px;">%s</p>

                                    %s

                                    %s

                                    %s
                                  </div>

                                  <div style="border-top:1px solid %s; padding:16px 24px; text-align:center;">
                                    <div style="font-size:11px; color:%s; line-height:1.6;">Clínica Stella Maris &middot; Tel (01) 234-5678<br/>Lunes a sábado, 7:00 am &ndash; 8:00 pm</div>
                                  </div>

                                </div>
                                </div>
                                """
                                .formatted(
                                                COLOR_LIENZO, COLOR_TINTA, COLOR_TINTA_CLARO, subtituloHeader,
                                                COLOR_TINTA, nombrePaciente,
                                                COLOR_NEBLINA, introHtml,
                                                tablaHtml, notaHtml, cta,
                                                COLOR_BORDE, COLOR_NEBLINA);
        }

        private String filaDetalle(String label, String valor, boolean ultima) {
                String borde = ultima ? "" : "border-bottom:1px solid " + COLOR_BORDE + ";";
                return """
                                <tr>
                                  <td style="padding:6px 0; %s font-size:13px; color:%s;">%s</td>
                                  <td style="padding:6px 0; %s font-size:13px; color:%s; font-weight:bold; text-align:right;">%s</td>
                                </tr>
                                """
                                .formatted(borde, COLOR_NEBLINA, label, borde, COLOR_TINTA, valor);
        }

        private String tablaDetalles(String filas) {
                return """
                                <table role="presentation" width="100%%" style="background:%s; border-radius:8px; padding:6px 12px; margin-bottom:20px; border-collapse:collapse;" cellpadding="0" cellspacing="0">
                                  <tr><td>
                                    <table role="presentation" width="100%%" style="border-collapse:collapse;" cellpadding="0" cellspacing="0">
                                      %s
                                    </table>
                                  </td></tr>
                                </table>
                                """
                                .formatted(COLOR_LIENZO, filas);
        }

        private String cajaNota(String texto, String colorBorde, String colorBg, String colorTexto) {
                return """
                                <div style="background:%s; border-left:3px solid %s; border-radius:0 6px 6px 0; padding:10px 14px; margin-bottom:22px;">
                                  <span style="font-size:12px; color:%s; line-height:1.5;">%s</span>
                                </div>
                                """
                                .formatted(colorBg, colorBorde, colorTexto, texto);
        }

        // ==========================================================
        // ENVÍO DE CORREO
        // ==========================================================

        /**
         * Envía el correo real al paciente según el tipo de notificación.
         * RF-20: Notificaciones por correo electrónico (HTML).
         * Envío vía Brevo API (HTTPS) — ver nota de clase.
         */
        private void enviarCorreo(Notificacion notificacion) {
                Paciente paciente = notificacion.getPaciente();
                String correoDestino = paciente.getCorreo();
                String nombrePaciente = paciente.getNombres() + " " + paciente.getApellidos();

                String asunto;
                String cuerpoHtml;
                byte[] adjuntoPdf = null;
                String nombreAdjunto = null;

                switch (notificacion.getTipo()) {
                        case "REGISTRO" -> {
                                CitaMedica cita = notificacion.getCita();
                                String fecha = cita.getFechaHora().format(FMT_FECHA);
                                String hora = cita.getFechaHora().format(FMT_HORA);
                                String medico = "Dr(a). " + cita.getMedico().getNombres()
                                                + " " + cita.getMedico().getApellidos();
                                String especialidad = cita.getMedico().getEspecialidad().getNombre();

                                asunto = "Cita registrada — Clínica Stella Maris";

                                String filas = filaDetalle("Médico", medico, false)
                                                + filaDetalle("Especialidad", especialidad, false)
                                                + filaDetalle("Fecha", fecha, false)
                                                + filaDetalle("Hora", hora, false)
                                                + filaDetalle("Costo", "S/ 80.00 (puede variar según seguro)", true);

                                cuerpoHtml = plantillaCorreo(
                                                "Confirmación de cita médica",
                                                nombrePaciente,
                                                "Su cita médica ha sido registrada exitosamente. Estos son los detalles:",
                                                tablaDetalles(filas),
                                                cajaNota("Su cita quedará <strong>confirmada</strong> una vez registrado el pago. Puede gestionarla desde el portal de pacientes.",
                                                                COLOR_GUIA, COLOR_GUIA_BG, COLOR_GUIA_TEXTO),
                                                "Ver mi cita", portalPacienteUrl + "/views/citas.html");
                        }
                        case "REPROGRAMACION" -> {
                                CitaMedica cita = notificacion.getCita();
                                String fecha = cita.getFechaHora().format(FMT_FECHA);
                                String hora = cita.getFechaHora().format(FMT_HORA);

                                asunto = "Cita reprogramada — Clínica Stella Maris";

                                String filas = filaDetalle("Nueva fecha", fecha, false)
                                                + filaDetalle("Nueva hora", hora, true);

                                cuerpoHtml = plantillaCorreo(
                                                "Cita reprogramada",
                                                nombrePaciente,
                                                "Su cita médica ha sido reprogramada. Estos son los nuevos datos:",
                                                tablaDetalles(filas),
                                                cajaNota("Si usted no solicitó este cambio, comuníquese con nosotros al (01) 234-5678.",
                                                                COLOR_GUIA, COLOR_GUIA_BG, COLOR_GUIA_TEXTO),
                                                "Ver mi cita", portalPacienteUrl + "/views/citas.html");
                        }
                        case "CANCELACION", "CANCELACION_AUTOMATICA" -> {
                                asunto = "Cita cancelada — Clínica Stella Maris";

                                cuerpoHtml = plantillaCorreo(
                                                "Cita cancelada",
                                                nombrePaciente,
                                                "Le informamos que su cita médica ha sido cancelada.",
                                                "",
                                                cajaNota(notificacion.getMensaje(),
                                                                COLOR_ALERTA, COLOR_ALERTA_BG, COLOR_ALERTA_TEXTO),
                                                "Agendar nueva cita", portalPacienteUrl + "/views/citas.html");
                        }
                        case "RECORDATORIO" -> {
                                CitaMedica cita = notificacion.getCita();
                                String fecha = cita.getFechaHora().format(FMT_FECHA);
                                String hora = cita.getFechaHora().format(FMT_HORA);
                                String medico = "Dr(a). " + cita.getMedico().getNombres()
                                                + " " + cita.getMedico().getApellidos();

                                asunto = "Recordatorio de cita mañana — Clínica Stella Maris";

                                String filas = filaDetalle("Médico", medico, false)
                                                + filaDetalle("Fecha", fecha, false)
                                                + filaDetalle("Hora", hora, true);

                                cuerpoHtml = plantillaCorreo(
                                                "Recordatorio de cita",
                                                nombrePaciente,
                                                "Le recordamos que tiene una cita médica mañana.",
                                                tablaDetalles(filas),
                                                cajaNota("Por favor llegue 10 minutos antes de su cita. Si no puede asistir, cancele desde el portal con anticipación.",
                                                                COLOR_GUIA, COLOR_GUIA_BG, COLOR_GUIA_TEXTO),
                                                "Ver mi cita", portalPacienteUrl + "/views/citas.html");
                        }
                        case "PAGO_CONFIRMADO" -> {
                                CitaMedica cita = notificacion.getCita();
                                String fecha = cita.getFechaHora().format(FMT_FECHA);
                                String hora = cita.getFechaHora().format(FMT_HORA);
                                String medico = "Dr(a). " + cita.getMedico().getNombres()
                                                + " " + cita.getMedico().getApellidos();

                                asunto = "Pago confirmado — Clínica Stella Maris";

                                String filas = filaDetalle("Médico", medico, false)
                                                + filaDetalle("Fecha", fecha, false)
                                                + filaDetalle("Hora", hora, true);

                                cuerpoHtml = plantillaCorreo(
                                                "Pago confirmado",
                                                nombrePaciente,
                                                "Su pago ha sido registrado exitosamente y su cita queda confirmada.",
                                                tablaDetalles(filas),
                                                cajaNota(notificacion.getMensaje()
                                                                + " Adjuntamos su boleta en PDF. Recuerde llegar 10 minutos antes de su cita.",
                                                                COLOR_RUMBO, COLOR_RUMBO_BG, COLOR_RUMBO_TEXTO),
                                                null, null);

                                // Adjuntar boleta PDF
                                pe.edu.utp.clinica.repository.PagoRepository pagoRepo = pagoService.getPagoRepository();
                                pe.edu.utp.clinica.model.Pago pago = pagoRepo
                                                .findByCita(cita).orElse(null);
                                if (pago != null) {
                                        adjuntoPdf = pagoService.generarBoletaPdf(pago);
                                        nombreAdjunto = "boleta-pago.pdf";
                                }
                        }
                        case "CONSULTA_REGISTRADA" -> {
                                asunto = "Consulta registrada — Clínica Stella Maris";

                                cuerpoHtml = plantillaCorreo(
                                                "Consulta registrada",
                                                nombrePaciente,
                                                notificacion.getMensaje(),
                                                "",
                                                "",
                                                "Ver historial médico", portalPacienteUrl + "/views/citas.html");
                        }
                        default -> {
                                asunto = "Notificación — Clínica Stella Maris";

                                cuerpoHtml = plantillaCorreo(
                                                "Notificación",
                                                nombrePaciente,
                                                notificacion.getMensaje(),
                                                "",
                                                "",
                                                null, null);
                        }
                }

                emailApiService.enviarCorreo(correoDestino, asunto, cuerpoHtml, adjuntoPdf, nombreAdjunto);
                log.debug("Correo enviado a {} — asunto: {}", correoDestino, asunto);
        }
}