package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.PasswordResetToken;

import java.util.Optional;

/**
 * Repositorio para tokens de recuperación de contraseña.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Elimina tokens viejos del mismo usuario para que solo haya uno activo a la
     * vez
     */
    void deleteByUsuarioUsername(String username);
}