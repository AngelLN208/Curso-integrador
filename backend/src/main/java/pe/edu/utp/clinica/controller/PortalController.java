package pe.edu.utp.clinica.controller;

import pe.edu.utp.clinica.service.HistorialPdfService;
import pe.edu.utp.clinica.service.PortalPagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.dto.portal.*;
import pe.edu.utp.clinica.service.ChatbotService;
import pe.edu.utp.clinica.service.PortalService;

import java.util.List;

/**
 * Controller para el portal del paciente.
 *
 * RF-51: Dashboard personal del paciente.
 * RF-52: Directorio público de especialidades y médicos.
 * RF-54: Calificación post-consulta de médicos.
 * RF-55: Chatbot de asistencia con IA.
 *
 * Rutas públicas (sin token): /api/portal/directorio/**
 * Rutas protegidas (con token ROLE_PACIENTE): /api/portal/dashboard,
 * /api/portal/valoraciones, /api/portal/chatbot
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@Tag(name = "Portal Paciente", description = "Autoatención del paciente — directorio, dashboard, valoraciones y chatbot")
public class PortalController {

        private final PortalService portalService;
        private final ChatbotService chatbotService;
        private final HistorialPdfService historialPdfService;
        private final PortalPagoService portalPagoService;

        // ─── RF-52: Directorio público ────────────────────────────────────────────

        /**
         * Lista todas las especialidades activas con sus médicos y horarios.
         * RF-52: Endpoint público — no requiere autenticación.
         * Usado en la página de inicio del portal para explorar médicos.
         */
        @GetMapping("/directorio")
        @Operation(summary = "Directorio de especialidades y médicos", description = "Público. Lista especialidades activas con médicos y horarios disponibles.")
        public ResponseEntity<ApiResponse<List<EspecialidadDirectorioResponse>>> obtenerDirectorio() {
                List<EspecialidadDirectorioResponse> directorio = portalService.obtenerDirectorio();
                return ResponseEntity.ok(ApiResponse.success("Directorio médico obtenido correctamente", directorio));
        }

        /**
         * Lista médicos activos de una especialidad específica.
         * RF-52: Filtrado por especialidad en el directorio.
         *
         * @param especialidadId ID de la especialidad a filtrar
         */
        @GetMapping("/directorio/especialidades/{especialidadId}/medicos")
        @Operation(summary = "Médicos por especialidad", description = "Público. Lista médicos activos de una especialidad con sus horarios y valoraciones.")
        public ResponseEntity<ApiResponse<List<MedicoDirectorioResponse>>> medicosPorEspecialidad(
                        @PathVariable Long especialidadId) {
                List<MedicoDirectorioResponse> medicos = portalService.obtenerMedicosPorEspecialidad(especialidadId);
                return ResponseEntity
                                .ok(ApiResponse.success("Médicos de la especialidad obtenidos correctamente", medicos));
        }

        // ─── RF-51: Dashboard del paciente ───────────────────────────────────────

        /**
         * Obtiene el dashboard personal del paciente autenticado.
         * RF-51: Próxima cita, último diagnóstico, pago pendiente y totales.
         * Requiere token JWT con rol ROLE_PACIENTE.
         *
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @GetMapping("/dashboard")
        @Operation(summary = "Dashboard del paciente", description = "Requiere autenticación. Muestra próxima cita, último diagnóstico y pagos pendientes.")
        public ResponseEntity<ApiResponse<DashboardPacienteResponse>> obtenerDashboard(
                        @AuthenticationPrincipal UserDetails userDetails) {
                DashboardPacienteResponse dashboard = portalService.obtenerDashboard(userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse.success("Dashboard cargado correctamente", dashboard));
        }

        // ─── RF-54: Valoraciones ──────────────────────────────────────────────────

        /**
         * Registra la calificación de un médico tras una consulta.
         * RF-54: Solo citas ATENDIDAS. Una sola valoración por cita.
         * Requiere token JWT con rol ROLE_PACIENTE.
         *
         * @param request     datos de la valoración (citaId, puntuación 1-5,
         *                    comentario)
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @PostMapping("/valoraciones")
        @Operation(summary = "Calificar médico post-consulta", description = "Requiere autenticación. El paciente califica al médico (1-5) después de una cita ATENDIDA.")
        public ResponseEntity<ApiResponse<ValoracionResponse>> registrarValoracion(
                        @Valid @RequestBody ValoracionRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {
                ValoracionResponse valoracion = portalService.registrarValoracion(request, userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse
                                .success("Valoración registrada correctamente. ¡Gracias por tu opinión!", valoracion));
        }

        // ─── RF-55: Chatbot ───────────────────────────────────────────────────────

        /**
         * Envía un mensaje al chatbot de asistencia.
         * RF-55: Responde FAQs y orienta al paciente hacia la especialidad
         * adecuada según los síntomas descritos.
         * Requiere token JWT (para personalizar respuestas con datos del paciente).
         *
         * @param request     mensaje del paciente e historial de conversación
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @PostMapping("/chatbot")
        @Operation(summary = "Chatbot de asistencia", description = "Requiere autenticación. IA que responde FAQs y orienta al paciente hacia la especialidad correcta.")
        public ResponseEntity<ApiResponse<ChatbotResponse>> chat(
                        @Valid @RequestBody ChatbotRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {
                ChatbotResponse respuesta = chatbotService.responder(request, userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse.success("Respuesta generada correctamente", respuesta));
        }
        // ─── RF-53: Historial en PDF ──────────────────────────────────────────────

        /**
         * Descarga el historial médico completo del paciente en PDF.
         * RF-53: Incluye todas las consultas con diagnóstico y tratamiento.
         * RNF-19: Generado en menos de 3 segundos.
         *
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @GetMapping(value = "/historial/pdf", produces = "application/pdf")
        @Operation(summary = "Descargar historial médico en PDF", description = "Requiere autenticación. Genera y descarga el historial clínico completo en PDF.")
        public ResponseEntity<byte[]> descargarHistorialPdf(
                        @AuthenticationPrincipal UserDetails userDetails) {

                byte[] pdf = historialPdfService.generarHistorial(
                                userDetails.getUsername());

                return ResponseEntity.ok()
                                .header("Content-Disposition",
                                                "attachment; filename=\"historial-medico.pdf\"")
                                .header("Content-Length", String.valueOf(pdf.length))
                                .body(pdf);
        }

        // ─── RF-56: Pago desde portal ─────────────────────────────────────────────

        /**
         * El paciente paga su cita directamente desde el portal.
         * RF-56: Solo puede pagar citas propias en estado PENDIENTE.
         * Valida algoritmo de Luhn si el método es TARJETA.
         *
         * @param request     datos del pago
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @PostMapping("/pagos")
        @Operation(summary = "Pagar cita desde el portal", description = "Requiere autenticación. El paciente paga su cita con validación de ownership.")
        public ResponseEntity<ApiResponse<pe.edu.utp.clinica.dto.pago.PagoResponse>> pagarCita(
                        @Valid @RequestBody PagoPortalRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                pe.edu.utp.clinica.dto.pago.PagoResponse pago = portalPagoService.pagarDesdePortal(
                                request, userDetails.getUsername());

                return ResponseEntity.ok(ApiResponse.success(
                                "Pago registrado correctamente", pago));
        }
}