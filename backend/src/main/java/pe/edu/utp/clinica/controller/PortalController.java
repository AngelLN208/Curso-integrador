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
import pe.edu.utp.clinica.service.PacienteService;

import java.util.List;

import pe.edu.utp.clinica.dto.portal.CitaPortalRequest;
import pe.edu.utp.clinica.dto.cita.CitaResponse;
import pe.edu.utp.clinica.service.CitaService;

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
        private final CitaService citaService;
        private final PacienteService pacienteService;

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

        /**
         * Previsualiza el descuento de seguro antes de pagar.
         * RF-16 (extendido): el paciente ve el cálculo (monto, descuento,
         * monto final) antes de confirmar el cobro, igual que recepcionista.
         *
         * @param citaId      ID de la cita a previsualizar
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @GetMapping("/pagos/cita/{citaId}/previsualizar")
        @Operation(summary = "Previsualizar pago desde el portal", description = "Requiere autenticación. Muestra el descuento de seguro antes de confirmar el cobro.")
        public ResponseEntity<ApiResponse<pe.edu.utp.clinica.dto.pago.PrevisualizarPagoResponse>> previsualizarPagoPortal(
                        @PathVariable Long citaId,
                        @AuthenticationPrincipal UserDetails userDetails) {

                var calculo = portalPagoService.previsualizarDesdePortal(citaId, userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse.success("Cálculo obtenido correctamente", calculo));
        }

        /**
         * Obtiene el comprobante de pago de una cita para mostrarlo en el portal.
         * RF-18 (extendido): el paciente ve su boleta, validando ownership.
         *
         * @param citaId      ID de la cita
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @GetMapping("/pagos/cita/{citaId}/comprobante")
        @Operation(summary = "Ver comprobante de pago", description = "Requiere autenticación. Muestra los datos de la boleta de una cita pagada.")
        public ResponseEntity<ApiResponse<pe.edu.utp.clinica.dto.pago.PagoResponse>> obtenerComprobantePortal(
                        @PathVariable Long citaId,
                        @AuthenticationPrincipal UserDetails userDetails) {

                var pago = portalPagoService.obtenerComprobanteDesdePortal(citaId, userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse.success("Comprobante obtenido correctamente", pago));
        }

        // ─── Agendar cita desde el portal ─────────────────────────────────────────

        /**
         * El paciente agenda su propia cita desde el portal.
         * El pacienteId se deriva del usuario autenticado, nunca del request,
         * para evitar que un paciente agende a nombre de otro.
         *
         * @param request     médico, fecha/hora y motivo
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @PostMapping("/citas")
        @Operation(summary = "Agendar cita desde el portal", description = "Requiere autenticación. El paciente agenda su propia cita; "
                        + "se valida disponibilidad del médico y separación mínima de 45 min.")
        public ResponseEntity<ApiResponse<CitaResponse>> agendarCita(
                        @Valid @RequestBody CitaPortalRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                CitaResponse cita = citaService.registrarDesdePortal(
                                request.getMedicoId(), request.getFechaHora(),
                                request.getMotivo(), userDetails.getUsername());

                return ResponseEntity.status(201)
                                .body(ApiResponse.created("Cita registrada correctamente", cita));
        }

        /**
         * El paciente cancela su propia cita desde el portal.
         * RF-09 (extendido): valida ownership en el service.
         *
         * @param citaId      ID de la cita a cancelar
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @PutMapping("/citas/{citaId}/cancelar")
        @Operation(summary = "Cancelar mi cita", description = "Requiere autenticación. El paciente cancela su propia cita.")
        public ResponseEntity<ApiResponse<CitaResponse>> cancelarMiCita(
                        @PathVariable Long citaId,
                        @AuthenticationPrincipal UserDetails userDetails) {

                CitaResponse cita = citaService.cancelarDesdePortal(citaId, userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse.success("Cita cancelada correctamente", cita));
        }

        /**
         * El paciente reprograma su propia cita desde el portal.
         * RF-06 (extendido): valida ownership y disponibilidad en el service.
         *
         * @param citaId      ID de la cita a reprogramar
         * @param request     nueva fecha y hora
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @PutMapping("/citas/{citaId}/reprogramar")
        @Operation(summary = "Reprogramar mi cita", description = "Requiere autenticación. El paciente reprograma su propia cita validando disponibilidad.")
        public ResponseEntity<ApiResponse<CitaResponse>> reprogramarMiCita(
                        @PathVariable Long citaId,
                        @Valid @RequestBody pe.edu.utp.clinica.dto.cita.CitaReprogramarRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                CitaResponse cita = citaService.reprogramarDesdePortal(
                                citaId, request.getNuevaFechaHora(), userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse.success("Cita reprogramada correctamente", cita));
        }

        /**
         * Lista todas las citas del paciente autenticado.
         * RF-51 (extendido): sección "Mis citas" — historial completo,
         * sin importar el estado (pendiente, confirmada, atendida, cancelada).
         *
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @GetMapping("/citas")
        @Operation(summary = "Listar mis citas", description = "Requiere autenticación. Devuelve el historial completo de citas del paciente autenticado.")
        public ResponseEntity<ApiResponse<List<CitaResponse>>> misCitas(
                        @AuthenticationPrincipal UserDetails userDetails) {

                List<CitaResponse> citas = citaService.listarPorPaciente(userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse.success("Citas obtenidas correctamente", citas));
        }

        /**
         * El paciente actualiza su propio perfil desde el portal.
         * RF-02 (extendido): el DNI nunca se puede modificar. Si cambia
         * el correo, se sincroniza automáticamente con su cuenta de login.
         *
         * @param request     nuevos datos del perfil
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @PutMapping("/perfil")
        @Operation(summary = "Actualizar mi perfil", description = "Requiere autenticación. El paciente edita su propia información, excepto el DNI.")
        public ResponseEntity<ApiResponse<pe.edu.utp.clinica.dto.paciente.PacienteResponse>> actualizarMiPerfil(
                        @Valid @RequestBody PerfilPacienteRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                var paciente = pacienteService.actualizarPerfilPropio(userDetails.getUsername(), request);
                return ResponseEntity.ok(ApiResponse.success("Perfil actualizado correctamente", paciente));
        }

        /**
         * Obtiene el perfil completo del paciente autenticado.
         * RF-02 (extendido): usado para precargar el formulario de edición.
         *
         * @param userDetails usuario autenticado extraído del token JWT
         */
        @GetMapping("/perfil")
        @Operation(summary = "Ver mi perfil", description = "Requiere autenticación. Devuelve los datos completos del paciente autenticado.")
        public ResponseEntity<ApiResponse<pe.edu.utp.clinica.dto.paciente.PacienteResponse>> obtenerMiPerfil(
                        @AuthenticationPrincipal UserDetails userDetails) {

                var paciente = pacienteService.obtenerPorCorreo(userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse.success("Perfil obtenido correctamente", paciente));
        }
}