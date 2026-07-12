package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import pe.edu.utp.clinica.dto.auth.LoginRequest;
import pe.edu.utp.clinica.dto.auth.LoginResponse;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.repository.UsuarioRepository;
import pe.edu.utp.clinica.security.JwtUtil;

import pe.edu.utp.clinica.repository.MedicoRepository;

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

        private final MedicoRepository medicoRepository;

        /**
         * Autentica al usuario y genera el token JWT.
         *
         * @param request credenciales de login
         * @return token JWT y datos básicos del usuario
         */
        public LoginResponse login(LoginRequest request) {
                // Spring Security valida las credenciales
                // Lanza BadCredentialsException si son inválidas
                try {
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        request.getUsername(),
                                                        request.getPassword()));
                } catch (BadCredentialsException ex) {
                        // RNF-04: no se loguea el username completo ni la contraseña,
                        // solo el hecho de que hubo un intento fallido (útil para
                        // detectar patrones de fuerza bruta en el plan de monitoreo).
                        log.warn("Intento de login fallido: credenciales inválidas");
                        throw ex;
                }

                log.info("Autenticación exitosa para usuario");

                UserDetails userDetails = userDetailsService
                                .loadUserByUsername(request.getUsername());

                String token = jwtUtil.generateToken(userDetails);

                Usuario usuario = usuarioRepository
                                .findByUsername(request.getUsername())
                                .orElseThrow();

                // RF-40 extendido: si el usuario es médico, incluimos su medicoId
                // para que el frontend pueda filtrar "mis citas" sin búsquedas extra.
                Long medicoId = null;
                if ("ROLE_MEDICO".equals(usuario.getRol().name())) {
                        medicoId = medicoRepository.findByUsuario(usuario)
                                        .map(m -> m.getId())
                                        .orElse(null);
                }

                log.debug("Token JWT generado correctamente para rol: {}", usuario.getRol().name());

                return LoginResponse.builder()
                                .token(token)
                                .username(usuario.getUsername())
                                .nombreCompleto(usuario.getNombreCompleto())
                                .rol(usuario.getRol().name())
                                .medicoId(medicoId)
                                .build();
        }
}