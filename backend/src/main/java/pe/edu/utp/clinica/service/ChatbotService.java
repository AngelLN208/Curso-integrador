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
 * RF-55: Responde preguntas frecuentes de la clínica, orienta al
 * paciente hacia la especialidad adecuada, y puede consultar
 * disponibilidad real y agendar citas mediante function calling.
 *
 * Integración: Google Gemini API (gemini-2.5-flash) — free tier.
 * RNF-18: Responde el 90% de consultas en menos de 5 segundos.
 *
 * Configuración requerida en application.properties:
 * chatbot.gemini.api-key=${GEMINI_API_KEY:demo}
 * chatbot.gemini.model=gemini-2.5-flash
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotToolService toolService;

    @Value("${chatbot.gemini.api-key:demo}")
    private String apiKey;

    @Value("${chatbot.gemini.model:gemini-2.5-flash}")
    private String model;

    private static final String GEMINI_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    /**
     * Máximo de vueltas del loop de function calling, para evitar bucles infinitos
     */
    private static final int MAX_TOOL_ITERATIONS = 5;

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
             - Fecha y hora actual: %s

             ESPECIALIDADES DISPONIBLES:
             Medicina General, Cardiología, Pediatría, Ginecología,
             Traumatología, Dermatología, Neurología, Oftalmología,
             Psiquiatría, Endocrinología.

             HERRAMIENTAS DISPONIBLES:
             Tienes acceso a herramientas para consultar médicos, ver su
             disponibilidad real, y agendar citas. Síguelas en este orden:

             1. Si el paciente pregunta por médicos o especialidades, usa
                buscar_medicos para obtener datos reales (nunca inventes
                nombres de médicos).
             2. Si el paciente pregunta por horarios o disponibilidad,
                primero usa buscar_medicos si no sabes el ID del médico,
                luego usa consultar_disponibilidad con ese ID y la fecha
                exacta (formato yyyy-MM-dd). Nunca inventes horarios —
                solo informa los que la herramienta realmente devuelve.
            3. Si el paciente quiere agendar una cita, reúne primero:
                médico (o especialidad), fecha y hora exacta. Usa
                consultar_disponibilidad para confirmar que el horario
                está libre. Luego usa proponer_cita, y en tu respuesta de
                texto incluye un resumen claro para el paciente (médico,
                especialidad, fecha, hora, costo) seguido EXACTAMENTE de
                este marcador en una línea aparte al final:
                CITA_PROPUESTA: {"medicoId": <id>, "medicoNombre": "<nombre>", "especialidad": "<especialidad>", "fechaHora": "<yyyy-MM-ddTHH:mm>", "motivo": "<motivo o null>"}
                Pídele que confirme explícitamente en su siguiente mensaje.
             4. SOLO si el paciente ya confirmó explícitamente una
                propuesta de cita que TÚ le mostraste en el mensaje
                anterior, usa confirmar_agendamiento para crearla de
                verdad. NUNCA llames a confirmar_agendamiento sin que el
                paciente haya confirmado primero en texto.
             5. Si confirmar_agendamiento devuelve exito=false, explica el
                error al paciente de forma amable (por ejemplo, el horario
                ya no está disponible) y ofrécele buscar otro horario.

             INSTRUCCIONES GENERALES:
             1. Responde SIEMPRE en español, de forma amable y profesional.
             2. Si el paciente describe síntomas, sugiere la especialidad más adecuada.
             3. NUNCA des diagnósticos médicos. Solo orientación de especialidad.
             4. Si la pregunta no es sobre la clínica o salud, redirige amablemente.
             5. Mantén respuestas concisas (máximo 3 párrafos).
             6. Al sugerir especialidad (sin agendar), incluye exactamente este
                formato al final: ESPECIALIDAD_SUGERIDA: [nombre exacto]
             7. No inventes información sobre médicos, precios o disponibilidad
                que no provenga directamente de las herramientas.
             """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Procesa el mensaje del paciente y genera una respuesta con IA,
     * resolviendo cualquier llamada a herramientas que el modelo solicite.
     *
     * @param request  mensaje del paciente e historial de conversación
     * @param username correo del paciente (para personalizar y para agendar)
     * @return respuesta final generada por la IA
     */
    public ChatbotResponse responder(ChatbotRequest request, String username) {
        if ("demo".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            log.warn("Chatbot en modo DEMO — configurar chatbot.gemini.api-key");
            return respuestaDemo(request.getMensaje());
        }

        try {
            return ejecutarConversacion(request, username);
        } catch (Exception ex) {
            log.error("Error al llamar a Gemini API: {}", ex.getMessage());
            return respuestaFallback();
        }
    }

    // ─── Loop principal de function calling ────────────────────────────────────

    private ChatbotResponse ejecutarConversacion(ChatbotRequest request, String username) throws Exception {
        List<Map<String, Object>> contents = construirHistorialInicial(request);

        for (int iteracion = 0; iteracion < MAX_TOOL_ITERATIONS; iteracion++) {
            JsonNode respuestaGemini = llamarGeminiAPI(contents);
            JsonNode parteRespuesta = respuestaGemini.path("candidates").get(0)
                    .path("content").path("parts").get(0);

            // Si la respuesta trae texto, terminamos el loop con la respuesta final
            if (parteRespuesta.has("text")) {
                return parsearRespuesta(parteRespuesta.path("text").asText());
            }

            // Si la respuesta trae una llamada a función, la ejecutamos
            if (parteRespuesta.has("functionCall")) {
                JsonNode functionCall = parteRespuesta.path("functionCall");
                String nombreFuncion = functionCall.path("name").asText();
                JsonNode args = functionCall.path("args");

                log.debug("Chatbot invocó tool: {} con args: {}", nombreFuncion, args);

                JsonNode resultadoTool = ejecutarTool(nombreFuncion, args, username);

                // Agregar el turno del modelo (la llamada a función) al historial
                // Agregar el turno del modelo (la llamada a función) al historial.
                // Usamos Object.class (no Map.class) porque "args" o "response"
                // pueden contener arrays anidados, no solo objetos planos.
                Map<String, Object> turnoModelo = new LinkedHashMap<>();
                turnoModelo.put("role", "model");
                turnoModelo.put("parts", List.of(Map.of(
                        "functionCall", objectMapper.convertValue(functionCall, Object.class)
                )));
                contents.add(turnoModelo);

                // Agregar el resultado de la función como siguiente turno
                Map<String, Object> turnoResultado = new LinkedHashMap<>();
                turnoResultado.put("role", "user");
                turnoResultado.put("parts", List.of(Map.of(
                        "functionResponse", Map.of(
                                "name", nombreFuncion,
                                "response", objectMapper.convertValue(resultadoTool, Object.class)
                        )
                )));
                contents.add(turnoResultado);

                continue; // volvemos a llamar a Gemini con el resultado incluido
            }

            // Si no hay texto ni functionCall, no hay nada más que hacer
            break;
        }

        // Si se agotaron las iteraciones sin una respuesta de texto final
        log.warn("Chatbot alcanzó el máximo de iteraciones de tools sin respuesta final");
        return respuestaFallback();
    }

    private JsonNode ejecutarTool(String nombreFuncion, JsonNode args, String username) {
        return switch (nombreFuncion) {
            case "buscar_medicos" -> toolService.buscarMedicos(
                    textoOpcional(args, "especialidad"),
                    textoOpcional(args, "nombre"));

            case "consultar_disponibilidad" -> toolService.consultarDisponibilidad(
                    args.path("medicoId").asLong(),
                    args.path("fecha").asText());

            case "proponer_cita" -> args; // no ejecuta nada, solo confirma estructura al modelo

            case "confirmar_agendamiento" -> toolService.confirmarAgendamiento(
                    args.path("medicoId").asLong(),
                    args.path("fechaHora").asText(),
                    textoOpcional(args, "motivo"),
                    username);

            default -> {
                log.warn("Tool desconocida solicitada por el modelo: {}", nombreFuncion);
                yield objectMapper.createObjectNode().put("error", "Herramienta no reconocida");
            }
        };
    }

    private String textoOpcional(JsonNode args, String campo) {
        return args.has(campo) && !args.path(campo).isNull() ? args.path(campo).asText() : null;
    }

    // ─── Construcción de la petición a Gemini ──────────────────────────────────

    private List<Map<String, Object>> construirHistorialInicial(ChatbotRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (request.getHistorial() != null) {
            for (ChatbotRequest.MensajeHistorial h : request.getHistorial()) {
                String rolGemini = "assistant".equals(h.getRol()) ? "model" : "user";
                contents.add(Map.of(
                        "role", rolGemini,
                        "parts", List.of(Map.of("text", h.getContenido()))));
            }
        }

        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", request.getMensaje()))));

        return contents;
    }

    private JsonNode llamarGeminiAPI(List<Map<String, Object>> contents) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        String systemPromptConFecha = SYSTEM_PROMPT.formatted(
                java.time.LocalDateTime.now().toString());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", contents);
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", systemPromptConFecha))));
        body.put("tools", List.of(Map.of("functionDeclarations", definirTools())));
        body.put("generationConfig", Map.of(
                "maxOutputTokens", 500,
                "temperature", 0.4));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = String.format(GEMINI_URL_TEMPLATE, model, apiKey);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return objectMapper.readTree(response.getBody());
    }

    /** Define las herramientas que el modelo puede invocar. */
    private List<Map<String, Object>> definirTools() {
        return List.of(
                Map.of(
                        "name", "buscar_medicos",
                        "description",
                        "Busca médicos reales por especialidad y/o nombre. Usar siempre antes de mencionar un médico específico.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "especialidad",
                                        Map.of("type", "string", "description",
                                                "Nombre de la especialidad, ej. Cardiología (opcional)"),
                                        "nombre",
                                        Map.of("type", "string", "description",
                                                "Nombre o apellido del médico (opcional)")))),
                Map.of(
                        "name", "consultar_disponibilidad",
                        "description",
                        "Consulta los horarios realmente disponibles de un médico en una fecha específica.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "medicoId",
                                        Map.of("type", "integer", "description",
                                                "ID del médico, obtenido de buscar_medicos"),
                                        "fecha",
                                        Map.of("type", "string", "description", "Fecha en formato yyyy-MM-dd")),
                                "required", List.of("medicoId", "fecha"))),
                Map.of(
                        "name", "proponer_cita",
                        "description",
                        "Estructura los datos de una cita propuesta para mostrarla al paciente antes de confirmar. No agenda nada todavía.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "medicoId", Map.of("type", "integer"),
                                        "medicoNombre", Map.of("type", "string"),
                                        "especialidad", Map.of("type", "string"),
                                        "fechaHora",
                                        Map.of("type", "string", "description", "Formato yyyy-MM-ddTHH:mm"),
                                        "motivo",
                                        Map.of("type", "string", "description", "Motivo de consulta, opcional")),
                                "required", List.of("medicoId", "medicoNombre", "especialidad", "fechaHora"))),
                Map.of(
                        "name", "confirmar_agendamiento",
                        "description",
                        "Crea la cita real en el sistema. SOLO usar después de que el paciente confirmó explícitamente una propuesta de proponer_cita en su mensaje anterior.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "medicoId", Map.of("type", "integer"),
                                        "fechaHora",
                                        Map.of("type", "string", "description",
                                                "Formato yyyy-MM-ddTHH:mm, exactamente igual al propuesto"),
                                        "motivo",
                                        Map.of("type", "string", "description", "Motivo de consulta, opcional")),
                                "required", List.of("medicoId", "fechaHora"))));
    }

    /**
     * Extrae la especialidad sugerida del texto de respuesta si existe.
     * El modelo incluye "ESPECIALIDAD_SUGERIDA: [nombre]" cuando aplica.
     */
    private ChatbotResponse parsearRespuesta(String texto) {
        String especialidad = null;
        String citaPropuestaJson = null;
        String respuestaLimpia = texto;

        if (respuestaLimpia.contains("CITA_PROPUESTA:")) {
            String[] partes = respuestaLimpia.split("CITA_PROPUESTA:");
            respuestaLimpia = partes[0].trim();
            citaPropuestaJson = partes[1].trim();
        }

        if (respuestaLimpia.contains("ESPECIALIDAD_SUGERIDA:")) {
            String[] partes = respuestaLimpia.split("ESPECIALIDAD_SUGERIDA:");
            respuestaLimpia = partes[0].trim();
            especialidad = partes[1].trim().replaceAll("[\\[\\]]", "");
        }

        return ChatbotResponse.builder()
                .respuesta(respuestaLimpia)
                .especialidadSugerida(especialidad)
                .citaPropuesta(citaPropuestaJson)
                .generadoPorIA(true)
                .build();
    }

    /**
     * Respuesta de demostración cuando no hay API key configurada.
     */
    private ChatbotResponse respuestaDemo(String mensaje) {
        String mensajeLower = mensaje.toLowerCase();

        Map<String, String> orientacion = Map.of(
                "corazón|cardio|presión|taquicardia", "Cardiología",
                "niño|pediatr|bebé|infantil", "Pediatría",
                "piel|acné|dermat|manchas", "Dermatología",
                "hueso|fractura|rodilla|columna", "Traumatología",
                "cabeza|neurolog|migraña|nervio", "Neurología",
                "ojo|vista|oftalm|visión", "Oftalmología",
                "ginecolog|embarazo|menstrua|útero", "Ginecología",
                "diabetes|tiroides|hormona|endocrin", "Endocrinología",
                "ansied|depresión|psiqui|mental", "Psiquiatría");

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

                        Para agendar tu cita necesito que la IA esté configurada con una \
                        clave real (actualmente estoy en modo demostración). Mientras tanto, \
                        puedes agendar directamente desde el directorio de médicos del portal.
                        """, especialidadSugerida)
                : """
                        Hola, soy Stella, tu asistente virtual de la Clínica Stella Maris. 😊

                        Estoy en modo demostración — para ayudarte a consultar disponibilidad \
                        real y agendar citas necesito que se configure una clave de IA. \
                        Mientras tanto, puedes explorar el directorio de médicos y agendar \
                        tu cita directamente desde ahí.
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