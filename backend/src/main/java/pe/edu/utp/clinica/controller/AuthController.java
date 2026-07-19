package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.dto.auth.LoginRequest;
import pe.edu.utp.clinica.dto.auth.LoginResponse;
import pe.edu.utp.clinica.dto.auth.RegistroPacienteRequest;
import pe.edu.utp.clinica.dto.auth.RegistroPacienteResponse;
import pe.edu.utp.clinica.security.JwtUtil;
import pe.edu.utp.clinica.security.TokenBlacklistService;
import pe.edu.utp.clinica.service.AuthPacienteService;
import pe.edu.utp.clinica.service.AuthService;

import pe.edu.utp.clinica.dto.portal.RecuperarPasswordRequest;
import pe.edu.utp.clinica.dto.portal.ResetPasswordRequest;
import pe.edu.utp.clinica.service.StaffPasswordResetService;

/**
 * Controlador de autenticación.
 *
 * RF-40: Endpoint público para login de todos los usuarios del sistema.
 * RF-28: Registro de nuevos pacientes desde el portal.
 * RF-29: Login de pacientes con correo y contraseña.
 * RNF-02: Devuelve token JWT válido por 24 horas. El logout invalida
 * el token antes de tiempo vía TokenBlacklistService.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login y registro de usuarios y pacientes")
public class AuthController {

        private final AuthService authService;
        private final AuthPacienteService authPacienteService;

        private final StaffPasswordResetService staffPasswordResetService;

        private final JwtUtil jwtUtil;
        private final TokenBlacklistService tokenBlacklistService;

        /**
         * Autentica un usuario del sistema (admin, recepcionista, médico o paciente)
         * y devuelve el token JWT.
         *
         * RF-40: Login de empleados del sistema.
         * RF-29: Login de pacientes del portal.
         *
         * @param request credenciales de login (username + contraseña)
         * @return token JWT + datos del usuario autenticado
         */
        @PostMapping("/login")
        @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y retorna un token JWT. " +
                        "Roles: ADMINISTRADOR, RECEPCIONISTA, MEDICO, PACIENTE")
        public ResponseEntity<ApiResponse<LoginResponse>> login(
                        @Valid @RequestBody LoginRequest request) {
                LoginResponse response = authService.login(request);
                return ResponseEntity.ok(
                                ApiResponse.success("Login exitoso", response));
        }

        /**
         * Cierra la sesión del usuario de staff autenticado (Admin,
         * Recepcionista o Médico), invalidando su token JWT actual.
         *
         * RNF-02: A partir de este momento, el token queda rechazado por
         * JwtAuthFilter aunque técnicamente no haya expirado todavía —
         * corrige el hecho de que JWT es stateless por diseño.
         *
         * @param request usado para leer el header Authorization directamente
         */
        @PostMapping("/logout")
        @Operation(summary = "Cerrar sesión (staff)", description = "RNF-02: Invalida el token JWT actual — deja de ser válido "
                        + "de inmediato, sin esperar a que expire solo.")
        public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
                String authHeader = request.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                                var expiracion = jwtUtil.extractExpiration(token);
                                tokenBlacklistService.invalidar(token, expiracion);
                        } catch (Exception ex) {
                                // Token ya inválido/expirado — no hay nada que invalidar,
                                // pero no es un error real para el usuario que cierra sesión
                        }
                }

                return ResponseEntity.ok(ApiResponse.success("Sesión cerrada correctamente", null));
        }

        /**
         * Registra un nuevo paciente en el portal de autoatención.
         *
         * RF-28: Crea entidad Paciente + Usuario con ROLE_PACIENTE.
         * Valida DNI único, correo único y contraseña segura.
         * Retorna token JWT para acceso inmediato al portal.
         *
         * Validaciones aplicadas automáticamente (Bean Validation):
         * - DNI: exactamente 8 dígitos (@ValidDni)
         * - Celular: 9 dígitos empezando en 9 (@ValidCelular)
         * - Correo: formato estricto con dominio (@ValidCorreo)
         * - Contraseña: mínimo 8 chars, mayúscula, número y símbolo (@ValidContrasena)
         * - Fecha de nacimiento: pasada, máximo 120 años (@ValidFechaNacimiento)
         * - Nombres/apellidos: solo letras y espacios
         *
         * @param request datos del nuevo paciente
         * @return datos del paciente registrado + token JWT
         */
        @PostMapping("/registro-paciente")
        @Operation(summary = "Registrar paciente en el portal", description = "Registro público. Crea cuenta de paciente con validaciones completas. "
                        +
                        "Retorna token JWT para acceso inmediato.")
        public ResponseEntity<ApiResponse<RegistroPacienteResponse>> registrarPaciente(
                        @Valid @RequestBody RegistroPacienteRequest request) {
                RegistroPacienteResponse response = authPacienteService.registrar(request);
                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.created(
                                                "Paciente registrado exitosamente", response));
        }

        /**
         * Solicita el envío de un correo de recuperación de contraseña
         * para cuentas de staff (Admin, Recepcionista, Médico).
         * Público — no requiere autenticación. Siempre responde con éxito
         * sin revelar si el correo existe o no.
         *
         * @param request correo de la cuenta de staff
         */
        @PostMapping("/recuperar-password")
        @Operation(summary = "Solicitar recuperación de contraseña (staff)", description = "Público. Envía un correo con link de reset si la cuenta existe.")
        public ResponseEntity<ApiResponse<Void>> solicitarRecuperacion(
                        @Valid @RequestBody RecuperarPasswordRequest request) {

                staffPasswordResetService.solicitarRecuperacion(request.getCorreo());
                return ResponseEntity.ok(ApiResponse.success(
                                "Si el correo está registrado, recibirás un enlace en los próximos minutos.", null));
        }

        /**
         * Valida el token y restablece la contraseña de una cuenta de staff.
         * Público — no requiere autenticación.
         *
         * @param request token + nueva contraseña + confirmación
         */
        @PostMapping("/reset-password")
        @Operation(summary = "Restablecer contraseña (staff)", description = "Público. Valida el token y establece la nueva contraseña.")
        public ResponseEntity<ApiResponse<Void>> resetearPassword(
                        @Valid @RequestBody ResetPasswordRequest request) {

                if (!request.getNuevaPassword().equals(request.getConfirmarPassword())) {
                        throw new IllegalArgumentException(
                                        "La nueva contraseña y su confirmación no coinciden.");
                }

                staffPasswordResetService.resetearPassword(request.getToken(), request.getNuevaPassword());
                return ResponseEntity.ok(ApiResponse.success(
                                "Contraseña restablecida correctamente. Ya puedes iniciar sesión.", null));
        }
}