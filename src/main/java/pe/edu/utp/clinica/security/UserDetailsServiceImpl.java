package pe.edu.utp.clinica.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import pe.edu.utp.clinica.repository.UsuarioRepository;

import java.util.List;

/**
 * Implementación de UserDetailsService para Spring Security.
 *
 * Carga el usuario desde la base de datos por su username (correo).
 * RF-40: Autenticación de todos los usuarios del sistema.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Carga el usuario por su username (correo electrónico).
     *
     * RNF-04: El log NO registra la contraseña ni datos sensibles.
     *
     * @param username correo del usuario
     * @return UserDetails con credenciales y roles
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Cargando usuario para autenticación");

        return usuarioRepository.findByUsername(username)
                .map(usuario -> new User(
                        usuario.getUsername(),
                        usuario.getPassword(),
                        List.of(new SimpleGrantedAuthority(usuario.getRol().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado"));
    }
}