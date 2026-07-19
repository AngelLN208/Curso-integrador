package pe.edu.utp.clinica.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilidad para generación y validación de tokens JWT.
 *
 * RNF-02: Toda petición sin token válido recibe HTTP 401.
 * El token expira en 24 horas (86400000 ms).
 * Al hacer logout, el token se invalida antes de tiempo vía
 * TokenBlacklistService (ver JwtAuthFilter).
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Genera la clave de firma a partir del secret configurado.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un token JWT para el usuario autenticado.
     *
     * @param userDetails datos del usuario autenticado
     * @return token JWT firmado
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae el username (correo) del token.
     *
     * @param token JWT recibido
     * @return username almacenado en el token
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extrae la fecha de expiración del token.
     * RNF-02: usado por TokenBlacklistService para saber hasta cuándo
     * mantener un token invalidado en la blacklist (después de esa
     * fecha, ya no hace falta — expiraría solo de todas formas).
     *
     * @param token JWT recibido
     * @return fecha de expiración del token
     */
    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    /**
     * Valida que el token pertenezca al usuario y no esté expirado.
     *
     * @param token       JWT recibido
     * @param userDetails datos del usuario a comparar
     * @return true si el token es válido
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception ex) {
            log.warn("Token JWT inválido: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Verifica si el token ya expiró.
     */
    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    /**
     * Extrae todos los claims del token.
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}