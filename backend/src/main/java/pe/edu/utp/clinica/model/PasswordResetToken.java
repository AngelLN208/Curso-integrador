package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Token temporal para recuperación de contraseña.
 * Expira en 30 minutos y solo puede usarse una vez.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Token UUID único — se envía en el link del correo */
    @Column(nullable = false, unique = true)
    private String token;

    /** Usuario al que pertenece el token */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Fecha/hora de expiración (30 minutos desde la creación) */
    @Column(nullable = false)
    private LocalDateTime expiraEn;

    /** Si ya fue usado para cambiar la contraseña */
    @Column(nullable = false)
    private boolean usado;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
        usado = false;
    }

    public boolean isValido() {
        return !usado && LocalDateTime.now().isBefore(expiraEn);
    }
}