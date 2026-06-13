package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.dto.auth.LoginRequest;
import pe.edu.utp.clinica.dto.auth.LoginResponse;
import pe.edu.utp.clinica.service.AuthService;

/**
 * Controlador de autenticación.
 *
 * RF-40: Endpoint público para login de todos los usuarios.
 * RNF-02: Devuelve token JWT válido por 24 horas.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login y gestión de tokens JWT")
public class AuthController {

    private final AuthService authService;

    /**
     * Autentica un usuario y devuelve el token JWT.
     *
     * @param request credenciales de login
     * @return token JWT + datos del usuario
     */
    @PostMapping("/login")
    @Operation(
        summary = "Iniciar sesión",
        description = "Autentica al usuario y retorna un token JWT. "
                    + "Roles: ADMINISTRADOR, RECEPCIONISTA, MEDICO"
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success("Login exitoso", response));
    }
}