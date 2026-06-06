package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import pe.edu.utp.clinica.dto.auth.LoginRequest;
import pe.edu.utp.clinica.dto.auth.LoginResponse;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.repository.UsuarioRepository;
import pe.edu.utp.clinica.security.JwtUtil;

/**
 * Servicio de autenticación del sistema.
 *
 * RF-40: Autentica a todos los usuarios mediante credenciales válidas.
 * RNF-02: Genera token JWT con expiración de 24 horas.
 * RNF-04: No se registran datos sensibles en los logs.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    /**
     * Autentica al usuario y genera el token JWT.
     *
     * @param request credenciales de login
     * @return token JWT y datos básicos del usuario
     */
    public LoginResponse login(LoginRequest request) {
        // Spring Security valida las credenciales
        // Lanza BadCredentialsException si son inválidas
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        log.debug("Autenticación exitosa para usuario");

        UserDetails userDetails = userDetailsService
                .loadUserByUsername(request.getUsername());

        String token = jwtUtil.generateToken(userDetails);

        Usuario usuario = usuarioRepository
                .findByUsername(request.getUsername())
                .orElseThrow();

        return LoginResponse.builder()
                .token(token)
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .rol(usuario.getRol().name())
                .build();
    }
}