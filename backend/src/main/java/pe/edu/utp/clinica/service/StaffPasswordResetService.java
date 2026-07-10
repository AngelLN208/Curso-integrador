package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.model.PasswordResetToken;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.repository.PasswordResetTokenRepository;
import pe.edu.utp.clinica.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio de recuperación de contraseña para cuentas de staff
 * (Administrador, Recepcionista, Médico).
 *
 * Reutiliza la misma tabla password_reset_tokens que el portal de
 * pacientes, pero con copy de correo y link apuntando al frontend
 * de staff en vez del portal.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffPasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String correoRemitente;

    @Value("${frontend.url:http://127.0.0.1:5500}")
    private String frontendUrl;

    /**
     * Genera un token de recuperación y envía el correo a la cuenta de staff.
     * Si el correo no existe, no lanza error (seguridad: no revelar
     * si una cuenta está registrada o no).
     *
     * @param correo correo (username) de la cuenta de staff
     */
    @Transactional
    public void solicitarRecuperacion(String correo) {
        usuarioRepository.findByUsername(correo.toLowerCase().trim()).ifPresent(usuario -> {

            resetTokenRepository.deleteByUsuarioUsername(usuario.getUsername());

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .usuario(usuario)
                    .expiraEn(LocalDateTime.now().plusMinutes(30))
                    .build();
            resetTokenRepository.save(resetToken);

            String linkReset = frontendUrl + "/views/auth/reset-password.html?token=" + token;

            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom("Clínica Stella Maris <" + correoRemitente + ">");
            mensaje.setTo(correo);
            mensaje.setSubject("🔐 Recuperación de contraseña — Clínica Stella Maris");
            mensaje.setText("""
                    Hola,

                    Recibimos una solicitud para restablecer la contraseña de tu cuenta
                    en el sistema de gestión de la Clínica Stella Maris.

                    Haz clic en el siguiente enlace para crear una nueva contraseña:

                    %s

                    Este enlace es válido por 30 minutos. Si no solicitaste este cambio,
                    puedes ignorar este correo — tu contraseña no cambiará.

                    ─────────────────────────────
                    Clínica Stella Maris
                    Tel: (01) 234-5678
                    """.formatted(linkReset));

            mailSender.send(mensaje);
            log.info("Correo de recuperación (staff) enviado a: {}", correo);
        });
    }

    /**
     * Valida el token y cambia la contraseña de la cuenta de staff.
     *
     * @param token         token UUID recibido del link del correo
     * @param nuevaPassword nueva contraseña a establecer
     */
    @Transactional
    public void resetearPassword(String token, String nuevaPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El enlace de recuperación no es válido."));

        if (!resetToken.isValido()) {
            throw new IllegalStateException(
                    "El enlace de recuperación ha expirado o ya fue utilizado. "
                            + "Solicita uno nuevo.");
        }

        if (nuevaPassword == null || nuevaPassword.length() < 6) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe tener al menos 6 caracteres.");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        resetTokenRepository.save(resetToken);

        log.info("Contraseña restablecida (staff) para usuario: {}", usuario.getUsername());
    }
}