package pe.edu.utp.clinica.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import pe.edu.utp.clinica.common.enums.RolUsuario;
import pe.edu.utp.clinica.dto.auth.LoginRequest;
import pe.edu.utp.clinica.dto.auth.LoginResponse;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.repository.UsuarioRepository;
import pe.edu.utp.clinica.security.JwtUtil;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuthService.
 *
 * RF-40: Autenticación con credenciales válidas.
 * RNF-02: Generación de token JWT.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests - AuthService")
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private Usuario usuario;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsername("admin@clinica.pe");
        loginRequest.setPassword("Admin123*");

        usuario = Usuario.builder()
                .id(1L)
                .username("admin@clinica.pe")
                .password("hashedPassword")
                .nombreCompleto("Administrador del Sistema")
                .rol(RolUsuario.ROLE_ADMINISTRADOR)
                .build();

        userDetails = new User(
                "admin@clinica.pe",
                "hashedPassword",
                List.of()
        );
    }

    @Test
    @DisplayName("RF-40: Debe autenticar usuario y retornar token JWT")
    void debeAutenticarUsuarioYRetornarToken() {
        // Arrange
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userDetailsService.loadUserByUsername("admin@clinica.pe"))
                .thenReturn(userDetails);
        when(usuarioRepository.findByUsername("admin@clinica.pe"))
                .thenReturn(Optional.of(usuario));
        when(jwtUtil.generateToken(any())).thenReturn("token.jwt.generado");

        // Act
        LoginResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("token.jwt.generado", response.getToken());
        assertEquals("admin@clinica.pe", response.getUsername());
        assertEquals("ROLE_ADMINISTRADOR", response.getRol());
        verify(jwtUtil, times(1)).generateToken(any());
    }

    @Test
    @DisplayName("RF-40: Debe lanzar excepción con credenciales inválidas")
    void debeLanzarExcepcionConCredencialesInvalidas() {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        // Act & Assert
        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );
        verify(jwtUtil, never()).generateToken(any());
    }
}