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

                // Validar DNI único en pacientes
                if (pacienteRepository.existsByDni(request.getDni())) {
                        throw new IllegalArgumentException(
                                        "Ya existe un paciente registrado con el DNI: " + request.getDni()
                                                        + ". Si ya tienes cuenta, inicia sesión.");
                }

                // Validar correo único en usuarios (evita duplicar cuentas)
                if (usuarioRepository.existsByUsername(request.getCorreo())) {
                        throw new IllegalArgumentException(
                                        "Ya existe una cuenta registrada con el correo: " + request.getCorreo()
                                                        + ". Si ya tienes cuenta, inicia sesión.");
                }

                // Crear entidad Paciente
                Paciente paciente = Paciente.builder()
                                .dni(request.getDni())
                                .nombres(request.getNombres().trim())
                                .apellidos(request.getApellidos().trim())
                                .fechaNacimiento(request.getFechaNacimiento())
                                .celular(request.getCelular())
                                .correo(request.getCorreo().toLowerCase().trim())
                                .sexo(request.getSexo())
                                .build();
                paciente = pacienteRepository.save(paciente);

                // Crear entidad Usuario vinculada al paciente
                // El username es el correo (en minúsculas para evitar duplicados por case)
                Usuario usuario = Usuario.builder()
                                .username(request.getCorreo().toLowerCase().trim())
                                .password(passwordEncoder.encode(request.getContrasena()))
                                .nombreCompleto(request.getNombres().trim()
                                                + " " + request.getApellidos().trim())
                                .rol(RolUsuario.ROLE_PACIENTE)
                                .activo(true)
                                .build();
                usuarioRepository.save(usuario);

                // Generar token JWT para acceso inmediato al portal
                // Crear UserDetails temporal para generar el token
                org.springframework.security.core.userdetails.User userDetails = new org.springframework.security.core.userdetails.User(
                                usuario.getUsername(),
                                usuario.getPassword(),
                                java.util.List.of(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                usuario.getRol().name())));

                // Generar token JWT para acceso inmediato al portal
                String token = jwtUtil.generateToken(userDetails);

                log.info("Paciente registrado exitosamente — DNI: {} | correo: {}",
                                request.getDni(), request.getCorreo());

                return RegistroPacienteResponse.builder()
                                .pacienteId(paciente.getId())
                                .dni(paciente.getDni())
                                .nombreCompleto(paciente.getNombres()
                                                + " " + paciente.getApellidos())
                                .correo(paciente.getCorreo())
                                .fechaNacimiento(paciente.getFechaNacimiento())
                                .token(token)
                                .mensaje("Registro exitoso. ¡Bienvenido a la Clínica Stella Maris!")
                                .build();
        }
}