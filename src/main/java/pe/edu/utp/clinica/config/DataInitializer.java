package pe.edu.utp.clinica.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pe.edu.utp.clinica.common.enums.RolUsuario;
import pe.edu.utp.clinica.model.Especialidad;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.repository.EspecialidadRepository;
import pe.edu.utp.clinica.repository.UsuarioRepository;

/**
 * Inicializador de datos del sistema.
 *
 * Se ejecuta al arrancar la aplicación y carga:
 * - Usuario administrador por defecto.
 * - Usuario recepcionista por defecto.
 * - Especialidades médicas iniciales.
 *
 * RNF-01: Las contraseñas se cifran con BCrypt.
 *
 * IMPORTANTE: Cambiar las contraseñas por defecto antes de producción.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        crearUsuarioAdministrador();
        crearUsuarioRecepcionista();
        crearEspecialidades();
    }

    /**
     * Crea el usuario administrador por defecto si no existe.
     * Credenciales: admin@clinica.pe / Admin123*
     */
    private void crearUsuarioAdministrador() {
        String username = "admin@clinica.pe";

        if (usuarioRepository.existsByUsername(username)) {
            log.debug("Usuario administrador ya existe, se omite creación");
            return;
        }

        Usuario admin = Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode("Admin123*"))
                .nombreCompleto("Administrador del Sistema")
                .rol(RolUsuario.ROLE_ADMINISTRADOR)
                .activo(true)
                .build();

        usuarioRepository.save(admin);
        log.info("✅ Usuario administrador creado: {}", username);
    }

    /**
     * Crea el usuario recepcionista por defecto si no existe.
     * Credenciales: recepcion@clinica.pe / Recep123*
     */
    private void crearUsuarioRecepcionista() {
        String username = "recepcion@clinica.pe";

        if (usuarioRepository.existsByUsername(username)) {
            log.debug("Usuario recepcionista ya existe, se omite creación");
            return;
        }

        Usuario recepcionista = Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode("Recep123*"))
                .nombreCompleto("Recepcionista Principal")
                .rol(RolUsuario.ROLE_RECEPCIONISTA)
                .activo(true)
                .build();

        usuarioRepository.save(recepcionista);
        log.info("✅ Usuario recepcionista creado: {}", username);
    }

    /**
     * Crea las especialidades médicas iniciales si no existen.
     */
    private void crearEspecialidades() {
        String[][] especialidades = {
            {"Medicina General",     "Atención primaria y consultas generales"},
            {"Cardiología",          "Diagnóstico y tratamiento de enfermedades cardíacas"},
            {"Pediatría",            "Atención médica para niños y adolescentes"},
            {"Ginecología",          "Salud reproductiva y atención femenina"},
            {"Traumatología",        "Lesiones del sistema musculoesquelético"},
            {"Dermatología",         "Enfermedades de la piel"},
            {"Neurología",           "Trastornos del sistema nervioso"},
            {"Oftalmología",         "Enfermedades de los ojos"},
            {"Psiquiatría",          "Salud mental y trastornos psiquiátricos"},
            {"Endocrinología",       "Trastornos hormonales y metabólicos"}
        };

        for (String[] esp : especialidades) {
            if (!especialidadRepository.existsByNombreIgnoreCase(esp[0])) {
                Especialidad especialidad = Especialidad.builder()
                        .nombre(esp[0])
                        .descripcion(esp[1])
                        .activo(true)
                        .build();
                especialidadRepository.save(especialidad);
                log.info("✅ Especialidad creada: {}", esp[0]);
            }
        }
    }
}