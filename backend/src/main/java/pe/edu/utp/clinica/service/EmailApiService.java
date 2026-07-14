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
 * Servicio de envío de correos vía API HTTPS de Brevo.
 *
 * Se usa en reemplazo de SMTP directo (JavaMailSender) porque el hosting
 * gratuito de Render bloquea las conexiones salientes a los puertos SMTP
 * (587 y 465, ambos verificados y bloqueados — ver Plan de Despliegue).
 * La API de Brevo funciona sobre HTTPS estándar, que sí está permitido.
 *
 * El remitente clinicastellamaris7@gmail.com debe estar verificado en
 * Brevo (Settings → Senders & IP) para poder enviar a cualquier
 * destinatario.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
public class EmailApiService {

        private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

        @Value("${brevo.api.key}")
        private String apiKey;

        @Value("${brevo.sender.email}")
        private String senderEmail;

        private final RestTemplate restTemplate = new RestTemplate();

        /**
         * Envía un correo HTML, con adjunto PDF opcional, vía la API de Brevo.
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
                headers.set("api-key", apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("accept", "application/json");

                Map<String, Object> body = new java.util.HashMap<>();
                body.put("sender", Map.of(
                                "name", "Clínica Stella Maris",
                                "email", senderEmail));
                body.put("to", List.of(Map.of("email", destino)));
                body.put("subject", asunto);
                body.put("htmlContent", htmlBody);

                if (adjuntoPdf != null && nombreAdjunto != null) {
                        String base64 = Base64.getEncoder().encodeToString(adjuntoPdf);
                        body.put("attachment", List.of(Map.of(
                                        "content", base64,
                                        "name", nombreAdjunto)));
                }

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                try {
                        restTemplate.postForEntity(BREVO_API_URL, request, String.class);
                        log.debug("Correo enviado vía Brevo API a {} — asunto: {}", destino, asunto);
                } catch (Exception e) {
                        log.error("Error enviando correo vía Brevo API a {}: {}", destino, e.getMessage());
                        throw e;
                }
        }
}