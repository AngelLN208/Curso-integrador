package pe.edu.utp.clinica.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Servicio de envío de correos vía API HTTPS de SendGrid.
 *
 * Se usa en reemplazo de SMTP directo (JavaMailSender) porque el hosting
 * gratuito de Render bloquea las conexiones salientes a los puertos SMTP
 * (587 y 465, ambos verificados y bloqueados — ver Plan de Despliegue).
 * La API de SendGrid funciona sobre HTTPS estándar, que sí está permitido.
 *
 * El remitente clinicastellamaris7@gmail.com fue verificado como
 * "Single Sender" en SendGrid, lo que permite enviar a cualquier
 * destinatario sin necesidad de un dominio propio verificado.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
public class EmailApiService {

        private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";

        @Value("${sendgrid.api.key}")
        private String apiKey;

        @Value("${sendgrid.sender.email}")
        private String senderEmail;

        private final RestTemplate restTemplate = new RestTemplate();

        /**
         * Envía un correo HTML, con adjunto PDF opcional, vía la API de SendGrid.
         *
         * @param destino       correo del destinatario
         * @param asunto        asunto del correo
         * @param htmlBody      cuerpo en HTML
         * @param adjuntoPdf    bytes del PDF a adjuntar (null si no aplica)
         * @param nombreAdjunto nombre del archivo adjunto (ej. "boleta-pago.pdf")
         */
        public void enviarCorreo(String destino, String asunto, String htmlBody,
                        byte[] adjuntoPdf, String nombreAdjunto) {

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = new java.util.HashMap<>();
                body.put("personalizations", List.of(Map.of(
                                "to", List.of(Map.of("email", destino)),
                                "subject", asunto)));
                body.put("from", Map.of(
                                "email", senderEmail,
                                "name", "Clínica Stella Maris"));
                body.put("content", List.of(Map.of(
                                "type", "text/html",
                                "value", htmlBody)));

                if (adjuntoPdf != null && nombreAdjunto != null) {
                        String base64 = Base64.getEncoder().encodeToString(adjuntoPdf);
                        body.put("attachments", List.of(Map.of(
                                        "content", base64,
                                        "filename", nombreAdjunto,
                                        "type", "application/pdf",
                                        "disposition", "attachment")));
                }

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                try {
                        restTemplate.postForEntity(SENDGRID_API_URL, request, String.class);
                        log.debug("Correo enviado vía SendGrid API a {} — asunto: {}", destino, asunto);
                } catch (Exception e) {
                        log.error("Error enviando correo vía SendGrid API a {}: {}", destino, e.getMessage());
                        throw e;
                }
        }
}