package pe.edu.utp.clinica.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de blacklist de tokens JWT invalidados (logout).
 *
 * RNF-02: JWT es stateless por diseño — el servidor no "recuerda" qué
 * tokens ya cerraron sesión, por lo que un token robado o filtrado
 * sigue siendo válido hasta que expira solo (24h). Esta blacklist
 * corrige eso: al hacer logout, el token se guarda aquí y el filtro
 * de seguridad (JwtAuthFilter) lo rechaza en cualquier request futuro,
 * aunque técnicamente no haya expirado todavía.
 *
 * Implementación en memoria (ConcurrentHashMap) — suficiente para el
 * alcance de este proyecto académico con una sola instancia de backend.
 * En un entorno con múltiples instancias/balanceador de carga, esto
 * debería migrar a un almacén compartido como Redis.
 *
 * Se limpia automáticamente cada hora para no acumular memoria con
 * tokens que de todas formas ya expiraron por su cuenta.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
public class TokenBlacklistService {

    // token -> fecha de expiración original (para poder limpiar cuando ya no hace
    // falta)
    private final Map<String, Date> tokensInvalidados = new ConcurrentHashMap<>();

    /**
     * Invalida un token (logout). A partir de este momento, JwtAuthFilter
     * lo rechazará aunque su firma y expiración sigan siendo válidas.
     *
     * @param token           el JWT completo (sin el prefijo "Bearer ")
     * @param fechaExpiracion la expiración original del token, usada
     *                        solo para la limpieza automática
     */
    public void invalidar(String token, Date fechaExpiracion) {
        tokensInvalidados.put(token, fechaExpiracion);
        log.debug("Token invalidado por logout. Total en blacklist: {}", tokensInvalidados.size());
    }

    /**
     * Verifica si un token fue invalidado (logout previo).
     *
     * @param token el JWT completo (sin el prefijo "Bearer ")
     * @return true si el token está en la blacklist
     */
    public boolean estaInvalidado(String token) {
        return tokensInvalidados.containsKey(token);
    }

    /**
     * Limpia tokens que ya expiraron por su cuenta — ya no hace falta
     * mantenerlos en la blacklist porque JwtUtil los rechazaría de
     * todas formas por expiración natural.
     * Corre cada hora.
     */
    @Scheduled(fixedRate = 3_600_000)
    public void limpiarTokensExpirados() {
        Date ahora = new Date();
        int antesDelTamano = tokensInvalidados.size();

        tokensInvalidados.entrySet().removeIf(entry -> entry.getValue().before(ahora));

        int eliminados = antesDelTamano - tokensInvalidados.size();
        if (eliminados > 0) {
            log.debug("Limpieza de blacklist: {} tokens expirados eliminados", eliminados);
        }
    }
}