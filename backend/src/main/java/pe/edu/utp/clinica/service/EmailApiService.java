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
 * Servicio de envío de correos vía API HTTPS de Resend.
 *
 * Se usa en reemplazo de SMTP directo (JavaMailSender) porque el hosting
 * gratuito de Render bloquea las conexiones salientes a los puertos SMTP
 * (587 y 465 verificados y bloqueados — ver Plan de Despliegue). La API
 * de Resend funciona sobre HTTPS estándar, que sí está permitido.
 *
 * Nota: en el plan gratuito, el remitente esta limitado a
 * onboarding@resend.dev hasta verificar un dominio propio.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
public class EmailApiService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.sender.email}")
    private String senderEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Envía un correo HTML, con adjunto PDF opcional, vía la API de Resend.
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
        body.put("from", "Clínica Stella Maris <" + senderEmail + ">");
        body.put("to", List.of(destino));
        body.put("subject", asunto);
        body.put("html", htmlBody);

        if (adjuntoPdf != null && nombreAdjunto != null) {
            String base64 = Base64.getEncoder().encodeToString(adjuntoPdf);
            body.put("attachments", List.of(Map.of(
                    "filename", nombreAdjunto,
                    "content", base64)));
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(RESEND_API_URL, request, String.class);
            log.debug("Correo enviado vía Resend API a {} — asunto: {}", destino, asunto);
        } catch (Exception e) {
            log.error("Error enviando correo vía Resend API a {}: {}", destino, e.getMessage());
            throw e;
        }
    }
}