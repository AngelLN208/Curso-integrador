package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import pe.edu.utp.clinica.service.AuthPacienteService;
import pe.edu.utp.clinica.service.AuthService;

/**
 * Controlador de autenticación.
 *
 * RF-40: Endpoint público para login de todos los usuarios del sistema.
 * RF-28: Registro de nuevos pacientes desde el portal.
 * RF-29: Login de pacientes con correo y contraseña.
 * RNF-02: Devuelve token JWT válido por 24 horas.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login y registro de usuarios y pacientes")
public class AuthController {

    private final AuthService          authService;
    private final AuthPacienteService  authPacienteService;

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
    @Operation(
        summary = "Iniciar sesión",
        description = "Autentica al usuario y retorna un token JWT. " +
                      "Roles: ADMINISTRADOR, RECEPCIONISTA, MEDICO, PACIENTE"
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success("Login exitoso", response));
    }

    /**
     * Registra un nuevo paciente en el portal de autoatención.
     *
     * RF-28: Crea entidad Paciente + Usuario con ROLE_PACIENTE.
     *        Valida DNI único, correo único y contraseña segura.
     *        Retorna token JWT para acceso inmediato al portal.
     *
     * Validaciones aplicadas automáticamente (Bean Validation):
     *  - DNI: exactamente 8 dígitos (@ValidDni)
     *  - Celular: 9 dígitos empezando en 9 (@ValidCelular)
     *  - Correo: formato estricto con dominio (@ValidCorreo)
     *  - Contraseña: mínimo 8 chars, mayúscula, número y símbolo (@ValidContrasena)
     *  - Fecha de nacimiento: pasada, máximo 120 años (@ValidFechaNacimiento)
     *  - Nombres/apellidos: solo letras y espacios
     *
     * @param request datos del nuevo paciente
     * @return datos del paciente registrado + token JWT
     */
    @PostMapping("/registro-paciente")
    @Operation(
        summary = "Registrar paciente en el portal",
        description = "Registro público. Crea cuenta de paciente con validaciones completas. " +
                      "Retorna token JWT para acceso inmediato."
    )
    public ResponseEntity<ApiResponse<RegistroPacienteResponse>> registrarPaciente(
            @Valid @RequestBody RegistroPacienteRequest request) {
        RegistroPacienteResponse response =
                authPacienteService.registrar(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        "Paciente registrado exitosamente", response));
    }
}