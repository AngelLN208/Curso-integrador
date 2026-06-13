package pe.edu.utp.clinica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.clinica.model.Usuario;

import java.util.Optional;

/**
 * Repositorio para la entidad Usuario.
 * RF-40: Permite cargar usuarios por username para autenticación.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca un usuario por su username (correo) */
    Optional<Usuario> findByUsername(String username);

    /** Verifica si ya existe un usuario con ese username */
    boolean existsByUsername(String username);
}