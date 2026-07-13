package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.common.enums.RolUsuario;
import pe.edu.utp.clinica.dto.auth.RegistroPacienteRequest;
import pe.edu.utp.clinica.dto.auth.RegistroPacienteResponse;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.repository.PacienteRepository;
import pe.edu.utp.clinica.repository.UsuarioRepository;
import pe.edu.utp.clinica.security.JwtUtil;

import pe.edu.utp.clinica.dto.portal.CambiarPasswordRequest;

import pe.edu.utp.clinica.model.PasswordResetToken;
import pe.edu.utp.clinica.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio para el registro e inicio de sesión de pacientes en el portal.
 *
 * RF-28: Registro del paciente con DNI, nombre, correo, celular y contraseña.
 * RF-29: Login del paciente con correo y contraseña vía JWT.
 *
 * Al registrarse se crean dos registros en simultáneo:
 * 1. Entidad Paciente con datos clínicos
 * 2. Entidad Usuario con credenciales y rol ROLE_PACIENTE
 *
 * Validaciones de negocio:
 * - DNI no duplicado en pacientes
 * - Correo no duplicado en usuarios (evita múltiples cuentas)
 * - Contraseña == confirmarContraseña
 *
 * NOTA DE DESPLIEGUE: el envío de correo se realiza vía la API HTTPS de
 * SendGrid (EmailApiService), no vía SMTP directo — ver Plan de Despliegue.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthPacienteService {

        private final PacienteRepository pacienteRepository;
        private final UsuarioRepository usuarioRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;

        private final PasswordResetTokenRepository resetTokenRepository;
        private final EmailApiService emailApiService;
        private final EmailTemplateHelper emailTemplateHelper;

        @Value("${app.portal.paciente.url:http://127.0.0.1:5501}")
        private String portalUrl;

        /**
         * Registra un nuevo paciente en el portal.
         * RF-28: Crea entidad Paciente + Usuario con ROLE_PACIENTE.
         *
         * @param request datos del registro
         * @return DTO con datos del paciente + token JWT para acceso inmediato
         * @throws IllegalArgumentException si DNI o correo ya están registrados
         * @throws IllegalStateException    si las contraseñas no coinciden
         */
        @Transactional
        public RegistroPacienteResponse registrar(RegistroPacienteRequest request) {

                // Validar que las contraseñas coincidan
                if (!request.getContrasena().equals(request.getConfirmarContrasena())) {
                        throw new IllegalStateException(
                                        "Las contraseñas no coinciden. Por favor verifica e intenta de nuevo.");
                }

                // Validar correo único en usuarios (evita duplicar cuentas)
                if (usuarioRepository.existsByUsername(request.getCorreo())) {
                        throw new IllegalArgumentException(
                                        "Ya existe una cuenta registrada con el correo: " + request.getCorreo()
                                                        + ". Si ya tienes cuenta, inicia sesión.");
                }

                Paciente paciente;

                // Caso 1: el paciente YA existe (fue registrado antes por recepción
                // al sacarle una cita presencial). En ese caso solo le creamos las
                // credenciales de acceso al portal, sin duplicar su registro clínico.
                var pacienteExistente = pacienteRepository.findByDni(request.getDni());

                if (pacienteExistente.isPresent()) {
                        paciente = pacienteExistente.get();

                        if (paciente.getCorreo() != null && usuarioRepository.existsByUsername(paciente.getCorreo())) {
                                throw new IllegalArgumentException(
                                                "Este DNI ya tiene una cuenta de portal asociada. Inicia sesión con tu correo registrado.");
                        }

                        // Actualiza datos de contacto por si cambiaron (correo/celular)
                        paciente.setCorreo(request.getCorreo().toLowerCase().trim());
                        paciente.setCelular(request.getCelular());
                        paciente = pacienteRepository.save(paciente);

                        log.info("Paciente existente (DNI: {}) vinculado a nueva cuenta de portal", request.getDni());

                } else {
                        // Caso 2: paciente completamente nuevo, se crea desde cero.
                        paciente = Paciente.builder()
                                        .dni(request.getDni())
                                        .nombres(request.getNombres().trim())
                                        .apellidos(request.getApellidos().trim())
                                        .fechaNacimiento(request.getFechaNacimiento())
                                        .celular(request.getCelular())
                                        .correo(request.getCorreo().toLowerCase().trim())
                                        .sexo(request.getSexo())
                                        .build();
                        paciente = pacienteRepository.save(paciente);

                        log.info("Paciente nuevo registrado — DNI: {}", request.getDni());
                }

                // Crear entidad Usuario vinculada al paciente
                Usuario usuario = Usuario.builder()
                                .username(request.getCorreo().toLowerCase().trim())
                                .password(passwordEncoder.encode(request.getContrasena()))
                                .nombreCompleto(paciente.getNombres() + " " + paciente.getApellidos())
                                .rol(RolUsuario.ROLE_PACIENTE)
                                .activo(true)
                                .build();
                usuarioRepository.save(usuario);

                org.springframework.security.core.userdetails.User userDetails = new org.springframework.security.core.userdetails.User(
                                usuario.getUsername(),
                                usuario.getPassword(),
                                java.util.List.of(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                usuario.getRol().name())));

                String token = jwtUtil.generateToken(userDetails);

                log.info("Cuenta de portal creada exitosamente — DNI: {} | correo: {}",
                                paciente.getDni(), request.getCorreo());

                return RegistroPacienteResponse.builder()
                                .pacienteId(paciente.getId())
                                .dni(paciente.getDni())
                                .nombreCompleto(paciente.getNombres() + " " + paciente.getApellidos())
                                .correo(paciente.getCorreo())
                                .fechaNacimiento(paciente.getFechaNacimiento())
                                .token(token)
                                .mensaje("Registro exitoso. ¡Bienvenido a la Clínica Stella Maris!")
                                .build();
        }

        /**
         * Cambia la contraseña del paciente autenticado desde el portal.
         * Requiere la contraseña actual para verificar identidad antes
         * de permitir el cambio — evita que sesiones abiertas sin
         * supervisión puedan cambiar la contraseña sin saberla.
         *
         * @param username correo del paciente autenticado (username del token)
         * @param request  contraseña actual + nueva + confirmación
         */
        @Transactional
        public void cambiarPassword(String username, CambiarPasswordRequest request) {
                Usuario usuario = usuarioRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Usuario no encontrado: " + username));

                // Verificar que la contraseña actual es correcta
                if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
                        throw new IllegalStateException(
                                        "La contraseña actual no es correcta.");
                }

                // Verificar que la nueva contraseña y su confirmación coinciden
                if (!request.getPasswordNueva().equals(request.getPasswordNuevaConfirmacion())) {
                        throw new IllegalStateException(
                                        "La nueva contraseña y su confirmación no coinciden.");
                }

                // Verificar que la nueva contraseña es distinta a la actual
                if (passwordEncoder.matches(request.getPasswordNueva(), usuario.getPassword())) {
                        throw new IllegalStateException(
                                        "La nueva contraseña debe ser diferente a la actual.");
                }

                usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
                usuarioRepository.save(usuario);

                log.info("Contraseña actualizada para usuario: {}", username);
        }

        /**
         * Genera un token de recuperación y envía el correo al paciente.
         * Si el correo no existe en el sistema, no lanza error (seguridad:
         * no revelar si un correo está registrado o no).
         *
         * @param correo correo del paciente que olvidó su contraseña
         */
        @Transactional
        public void solicitarRecuperacion(String correo) {
                // Buscar el usuario silenciosamente — si no existe, no hacemos nada
                // (no revelamos si el correo está registrado o no)
                usuarioRepository.findByUsername(correo.toLowerCase().trim()).ifPresent(usuario -> {

                        // Eliminar tokens anteriores del mismo usuario
                        resetTokenRepository.deleteByUsuarioUsername(usuario.getUsername());

                        // Generar token UUID con 30 minutos de expiración
                        String token = UUID.randomUUID().toString();
                        PasswordResetToken resetToken = PasswordResetToken.builder()
                                        .token(token)
                                        .usuario(usuario)
                                        .expiraEn(LocalDateTime.now().plusMinutes(30))
                                        .build();
                        resetTokenRepository.save(resetToken);

                        // Enviar correo con el link de reset
                        String linkReset = portalUrl + "/views/reset-password.html?token=" + token;

                        String cuerpoHtml = emailTemplateHelper.plantillaCorreo(
                                        "Recuperación de contraseña",
                                        usuario.getNombreCompleto(),
                                        "Recibimos una solicitud para restablecer la contraseña de tu cuenta en el portal de pacientes.",
                                        "",
                                        emailTemplateHelper.cajaNota(
                                                        "Este enlace es válido por 30 minutos. Si no solicitaste este cambio, puedes ignorar este correo — tu contraseña no cambiará.",
                                                        EmailTemplateHelper.COLOR_GUIA,
                                                        EmailTemplateHelper.COLOR_GUIA_BG,
                                                        EmailTemplateHelper.COLOR_GUIA_TEXTO),
                                        "Restablecer contraseña", linkReset);

                        emailApiService.enviarCorreo(correo,
                                        "Recuperación de contraseña — Clínica Stella Maris",
                                        cuerpoHtml, null, null);

                        log.info("Correo de recuperación enviado a: {}", correo);
                });
        }

        /**
         * Valida el token y cambia la contraseña del usuario.
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

                // Cambiar la contraseña
                Usuario usuario = resetToken.getUsuario();
                usuario.setPassword(passwordEncoder.encode(nuevaPassword));
                usuarioRepository.save(usuario);

                // Marcar el token como usado (no se puede reutilizar)
                resetToken.setUsado(true);
                resetTokenRepository.save(resetToken);

                log.info("Contraseña restablecida para usuario: {}", usuario.getUsername());
        }
}