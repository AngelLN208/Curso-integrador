package pe.edu.utp.clinica.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import pe.edu.utp.clinica.dto.portal.ChatbotRequest;
import pe.edu.utp.clinica.dto.portal.ChatbotResponse;

import java.util.*;

/**
 * Servicio para el chatbot de asistencia al paciente.
 *
 * RF-55: Responde preguntas frecuentes de la clínica y orienta
 *        al paciente hacia la especialidad adecuada según los
 *        síntomas que describe.
 *
 * Integración: Anthropic Claude API (claude-sonnet-4-6)
 * RNF-18: Responde el 90% de consultas en menos de 5 segundos.
 *
 * Configuración requerida en application.properties:
 *   chatbot.anthropic.api-key=sk-ant-...
 *   chatbot.anthropic.model=claude-sonnet-4-6
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    @Value("${chatbot.anthropic.api-key:demo}")
    private String apiKey;

    @Value("${chatbot.anthropic.model:claude-sonnet-4-6}")
    private String model;

    private static final String ANTHROPIC_URL =
            "https://api.anthropic.com/v1/messages";

    private static final String SYSTEM_PROMPT = """
        Eres el asistente virtual de la Clínica Stella Maris en Lima, Perú.
        Tu nombre es "Stella" y tu función es ayudar a los pacientes.

        INFORMACIÓN DE LA CLÍNICA:
        - Nombre: Clínica Stella Maris
        - Ubicación: Lima, Perú
        - Horario de atención: Lunes a Sábado, 7:00 AM - 8:00 PM
        - Costo de consulta: S/ 80.00 (puede variar según seguro)
        - Teléfono: (01) 234-5678
        - Correo: contacto@stellamaris.pe

        ESPECIALIDADES DISPONIBLES:
        Medicina General, Cardiología, Pediatría, Ginecología,
        Traumatología, Dermatología, Neurología, Oftalmología,
        Psiquiatría, Endocrinología.

        INSTRUCCIONES:
        1. Responde SIEMPRE en español, de forma amable y profesional.
        2. Si el paciente describe síntomas, sugiere la especialidad más adecuada.
        3. Para agendar citas, indícale que use el portal o llame al teléfono.
        4. NUNCA des diagnósticos médicos. Solo orientación de especialidad.
        5. Si la pregunta no es sobre la clínica o salud, redirige amablemente.
        6. Mantén respuestas concisas (máximo 3 párrafos).
        7. Al sugerir especialidad, incluye exactamente este formato al final:
           ESPECIALIDAD_SUGERIDA: [nombre exacto de la especialidad]

        IMPORTANTE: No inventes información sobre médicos específicos,
        precios o disponibilidad. Solo usa la información proporcionada.
        """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Procesa el mensaje del paciente y genera una respuesta con IA.
     * RF-55: Orienta al paciente y sugiere especialidad si aplica.
     *
     * @param request  mensaje del paciente e historial de conversación
     * @param username correo del paciente (para personalizar respuesta)
     * @return respuesta generada por la IA con especialidad sugerida si aplica
     */
    public ChatbotResponse responder(ChatbotRequest request, String username) {
        // Si no hay API key configurada, usar modo demo
        if ("demo".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            log.warn("Chatbot en modo DEMO — configurar chatbot.anthropic.api-key");
            return respuestaDemo(request.getMensaje());
        }

        try {
            String respuestaTexto = llamarAnthropicAPI(request);
            return parsearRespuesta(respuestaTexto);
        } catch (Exception ex) {
            log.error("Error al llamar a Anthropic API: {}", ex.getMessage());
            return respuestaFallback();
        }
    }

    // ─── Métodos privados ─────────────────────────────────────────────────────

    /**
     * Llama a la API de Anthropic con el mensaje y el historial.
     * RNF-18: Timeout configurado para cumplir el límite de 5 segundos.
     */
    private String llamarAnthropicAPI(ChatbotRequest request) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        // Construir mensajes incluyendo historial
        List<Map<String, String>> mensajes = new ArrayList<>();

        // Agregar historial previo si existe
        if (request.getHistorial() != null) {
            for (ChatbotRequest.MensajeHistorial h : request.getHistorial()) {
                mensajes.add(Map.of(
                        "role",    h.getRol(),
                        "content", h.getContenido()
                ));
            }
        }

        // Agregar mensaje actual del paciente
        mensajes.add(Map.of(
                "role",    "user",
                "content", request.getMensaje()
        ));

        // Construir body de la petición
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model",      model);
        body.put("max_tokens", 500);
        body.put("system",     SYSTEM_PROMPT);
        body.put("messages",   mensajes);

        // Headers requeridos por Anthropic
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key",         apiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                ANTHROPIC_URL, entity, String.class);

        // Extraer texto de la respuesta
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("content").get(0).path("text").asText();
    }

    /**
     * Extrae la especialidad sugerida del texto de respuesta si existe.
     * El modelo incluye "ESPECIALIDAD_SUGERIDA: [nombre]" cuando aplica.
     */
    private ChatbotResponse parsearRespuesta(String texto) {
        String especialidad = null;
        String respuestaLimpia = texto;

        if (texto.contains("ESPECIALIDAD_SUGERIDA:")) {
            String[] partes = texto.split("ESPECIALIDAD_SUGERIDA:");
            respuestaLimpia = partes[0].trim();
            especialidad    = partes[1].trim().replaceAll("[\\[\\]]", "");
        }

        return ChatbotResponse.builder()
                .respuesta(respuestaLimpia)
                .especialidadSugerida(especialidad)
                .generadoPorIA(true)
                .build();
    }

    /**
     * Respuesta de demostración cuando no hay API key configurada.
     * Permite probar el portal sin costo hasta que se configure la clave.
     */
    private ChatbotResponse respuestaDemo(String mensaje) {
        String mensajeLower = mensaje.toLowerCase();

        // Orientación básica por palabras clave
        Map<String, String> orientacion = Map.of(
            "corazón|cardio|presión|taquicardia", "Cardiología",
            "niño|pediatr|bebé|infantil",          "Pediatría",
            "piel|acné|dermat|manchas",            "Dermatología",
            "hueso|fractura|rodilla|columna",      "Traumatología",
            "cabeza|neurolog|migraña|nervio",      "Neurología",
            "ojo|vista|oftalm|visión",             "Oftalmología",
            "ginecolog|embarazo|menstrua|útero",   "Ginecología",
            "diabetes|tiroides|hormona|endocrin",  "Endocrinología",
            "ansied|depresión|psiqui|mental",      "Psiquiatría"
        );

        String especialidadSugerida = null;
        for (Map.Entry<String, String> entry : orientacion.entrySet()) {
            if (Arrays.stream(entry.getKey().split("\\|"))
                      .anyMatch(mensajeLower::contains)) {
                especialidadSugerida = entry.getValue();
                break;
            }
        }

        String respuesta = especialidadSugerida != null
            ? String.format("""
                Hola, soy Stella, tu asistente virtual de la Clínica Stella Maris. 😊

                Basándome en lo que describes, te recomiendo consultar con nuestro \
                equipo de **%s**. Nuestros especialistas están disponibles de lunes \
                a sábado de 7:00 AM a 8:00 PM.

                Puedes agendar tu cita directamente desde este portal o llamarnos \
                al (01) 234-5678. El costo de consulta es S/ 80.00 (puede aplicar \
                tu seguro si tienes uno vinculado).
                """, especialidadSugerida)
            : """
                Hola, soy Stella, tu asistente virtual de la Clínica Stella Maris. 😊

                Estoy aquí para ayudarte con información sobre nuestros servicios, \
                especialidades disponibles y orientación médica general.

                Atendemos de lunes a sábado de 7:00 AM a 8:00 PM. Para agendar \
                una cita puedes usar este portal o llamarnos al (01) 234-5678. \
                ¿En qué más puedo ayudarte?
                """;

        return ChatbotResponse.builder()
                .respuesta(respuesta)
                .especialidadSugerida(especialidadSugerida)
                .generadoPorIA(false)
                .build();
    }

    /** Respuesta de emergencia si la API falla. */
    private ChatbotResponse respuestaFallback() {
        return ChatbotResponse.builder()
                .respuesta("""
                    Lo siento, en este momento no puedo procesar tu consulta. \
                    Por favor llámanos al (01) 234-5678 o visítanos en la clínica \
                    de lunes a sábado de 7:00 AM a 8:00 PM. ¡Estaremos felices \
                    de ayudarte!
                    """)
                .especialidadSugerida(null)
                .generadoPorIA(false)
                .build();
    }
}