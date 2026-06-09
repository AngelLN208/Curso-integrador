package pe.edu.utp.clinica.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.EstadoPago;
import pe.edu.utp.clinica.common.enums.RolUsuario;
import pe.edu.utp.clinica.model.*;
import pe.edu.utp.clinica.repository.*;

import java.math.BigDecimal;
import java.time.*;

/**
 * Inicializador de datos del sistema.
 * Carga usuarios, especialidades, médicos, pacientes y citas de prueba.
 *
 * RNF-01: Contraseñas cifradas con BCrypt.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final CitaMedicaRepository citaRepository;
    private final PagoRepository pagoRepository;
    private final HorarioMedicoRepository horarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        crearUsuarioAdministrador();
        crearUsuarioRecepcionista();
        crearEspecialidades();
        crearMedicos();
        crearPacientes();
        crearCitasDeHoy();
    }

    // ─── Usuarios ─────────────────────────────────────────────────────

    private void crearUsuarioAdministrador() {
        String username = "admin@clinica.pe";
        if (usuarioRepository.existsByUsername(username)) return;

        usuarioRepository.save(Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode("Admin123*"))
                .nombreCompleto("Administrador del Sistema")
                .rol(RolUsuario.ROLE_ADMINISTRADOR)
                .activo(true).build());
        log.info("✅ Admin creado: {}", username);
    }

    private void crearUsuarioRecepcionista() {
        String username = "recepcion@clinica.pe";
        if (usuarioRepository.existsByUsername(username)) return;

        usuarioRepository.save(Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode("Recep123*"))
                .nombreCompleto("Recepcionista Principal")
                .rol(RolUsuario.ROLE_RECEPCIONISTA)
                .activo(true).build());
        log.info("✅ Recepcionista creado: {}", username);
    }

    // ─── Especialidades ───────────────────────────────────────────────

    private void crearEspecialidades() {
        String[][] especialidades = {
            {"Medicina General",  "Atención primaria y consultas generales"},
            {"Cardiología",       "Diagnóstico y tratamiento de enfermedades cardíacas"},
            {"Pediatría",         "Atención médica para niños y adolescentes"},
            {"Ginecología",       "Salud reproductiva y atención femenina"},
            {"Traumatología",     "Lesiones del sistema musculoesquelético"},
            {"Dermatología",      "Enfermedades de la piel"},
            {"Neurología",        "Trastornos del sistema nervioso"},
            {"Oftalmología",      "Enfermedades de los ojos"},
            {"Psiquiatría",       "Salud mental y trastornos psiquiátricos"},
            {"Endocrinología",    "Trastornos hormonales y metabólicos"}
        };

        for (String[] esp : especialidades) {
            if (!especialidadRepository.existsByNombreIgnoreCase(esp[0])) {
                especialidadRepository.save(Especialidad.builder()
                        .nombre(esp[0]).descripcion(esp[1]).activo(true).build());
                log.info("✅ Especialidad creada: {}", esp[0]);
            }
        }
    }

    // ─── Médicos ──────────────────────────────────────────────────────

    private void crearMedicos() {
        if (medicoRepository.existsByDni("45123678")) return;

        Especialidad medGeneral = especialidadRepository
                .findByNombreIgnoreCase("Medicina General").orElseThrow();
        Especialidad cardiologia = especialidadRepository
                .findByNombreIgnoreCase("Cardiología").orElseThrow();
        Especialidad pediatria = especialidadRepository
                .findByNombreIgnoreCase("Pediatría").orElseThrow();

        // Médico 1
        Usuario u1 = usuarioRepository.save(Usuario.builder()
                .username("carlos.lopez@clinica.pe")
                .password(passwordEncoder.encode("Medico123*"))
                .nombreCompleto("Carlos López Ramírez")
                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());

        Medico m1 = medicoRepository.save(Medico.builder()
                .dni("45123678").nombres("Carlos").apellidos("López Ramírez")
                .especialidad(medGeneral).celular("987111222")
                .correo("carlos.lopez@clinica.pe").usuario(u1).activo(true).build());

        // Horario médico 1 (Lunes a Viernes 8am-2pm)
        for (DayOfWeek dia : new DayOfWeek[]{
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            horarioRepository.save(HorarioMedico.builder()
                    .medico(m1).dia(dia)
                    .horaInicio(LocalTime.of(8, 0))
                    .horaFin(LocalTime.of(14, 0)).build());
        }

        // Médico 2
        Usuario u2 = usuarioRepository.save(Usuario.builder()
                .username("ana.torres@clinica.pe")
                .password(passwordEncoder.encode("Medico123*"))
                .nombreCompleto("Ana Torres Vega")
                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());

        Medico m2 = medicoRepository.save(Medico.builder()
                .dni("52398741").nombres("Ana").apellidos("Torres Vega")
                .especialidad(cardiologia).celular("987333444")
                .correo("ana.torres@clinica.pe").usuario(u2).activo(true).build());

        // Horario médico 2 (Lunes, Miércoles, Viernes 9am-1pm)
        for (DayOfWeek dia : new DayOfWeek[]{
                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY}) {
            horarioRepository.save(HorarioMedico.builder()
                    .medico(m2).dia(dia)
                    .horaInicio(LocalTime.of(9, 0))
                    .horaFin(LocalTime.of(13, 0)).build());
        }

        // Médico 3
        Usuario u3 = usuarioRepository.save(Usuario.builder()
                .username("luis.mendoza@clinica.pe")
                .password(passwordEncoder.encode("Medico123*"))
                .nombreCompleto("Luis Mendoza Castillo")
                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());

        medicoRepository.save(Medico.builder()
                .dni("63741852").nombres("Luis").apellidos("Mendoza Castillo")
                .especialidad(pediatria).celular("987555666")
                .correo("luis.mendoza@clinica.pe").usuario(u3).activo(true).build());

        log.info("✅ Médicos creados: Carlos López, Ana Torres, Luis Mendoza");
    }

    // ─── Pacientes ────────────────────────────────────────────────────

    private void crearPacientes() {
        if (pacienteRepository.existsByDni("71234567")) return;

        Object[][] pacientes = {
            {"71234567","María","García Pérez",     LocalDate.of(1985,3,12),"987001001","maria.garcia@gmail.com","F"},
            {"72345678","José","Rodríguez Soto",    LocalDate.of(1990,7,25),"987002002","jose.rodriguez@gmail.com","M"},
            {"73456789","Carmen","Flores Lima",     LocalDate.of(1978,11,5),"987003003","carmen.flores@gmail.com","F"},
            {"74567890","Pedro","Díaz Quispe",      LocalDate.of(1995,1,18),"987004004","pedro.diaz@gmail.com","M"},
            {"75678901","Rosa","Mamani Torres",     LocalDate.of(2000,6,30),"987005005","rosa.mamani@gmail.com","F"},
            {"76789012","Luis","Cárdenas Vega",     LocalDate.of(1972,9,14),"987006006","luis.cardenas@gmail.com","M"},
            {"77890123","Ana","Huanca Ramos",       LocalDate.of(1988,4,22),"987007007","ana.huanca@gmail.com","F"},
            {"78901234","Carlos","Vargas Suárez",   LocalDate.of(2003,12,8),"987008008","carlos.vargas@gmail.com","M"},
        };

        for (Object[] p : pacientes) {
            if (!pacienteRepository.existsByDni((String) p[0])) {
                pacienteRepository.save(Paciente.builder()
                        .dni((String) p[0])
                        .nombres((String) p[1])
                        .apellidos((String) p[2])
                        .fechaNacimiento((LocalDate) p[3])
                        .celular((String) p[4])
                        .correo((String) p[5])
                        .sexo((String) p[6])
                        .build());
            }
        }
        log.info("✅ 8 pacientes creados");
    }

    // ─── Citas de hoy ─────────────────────────────────────────────────

    private void crearCitasDeHoy() {
        Paciente p1 = pacienteRepository.findByDni("71234567").orElseThrow();
        Paciente p2 = pacienteRepository.findByDni("72345678").orElseThrow();
        Paciente p3 = pacienteRepository.findByDni("73456789").orElseThrow();
        Paciente p4 = pacienteRepository.findByDni("74567890").orElseThrow();
        Paciente p5 = pacienteRepository.findByDni("75678901").orElseThrow();

        Medico m1 = medicoRepository.findByDni("45123678").orElseThrow();
        Medico m2 = medicoRepository.findByDni("52398741").orElseThrow();

        Usuario admin = usuarioRepository.findByUsername("admin@clinica.pe").orElseThrow();

        LocalDate hoy = LocalDate.now();

        // Cita 1 — CONFIRMADA con pago
        if (!citaRepository.existeConflictoHorario(m1,
                hoy.atTime(8, 0))) {
            CitaMedica c1 = citaRepository.save(CitaMedica.builder()
                    .paciente(p1).medico(m1)
                    .fechaHora(hoy.atTime(8, 0))
                    .motivo("Control de presión arterial")
                    .estado(EstadoCita.CONFIRMADA)
                    .registradoPor(admin).build());
            pagoRepository.save(Pago.builder()
                    .cita(c1).monto(new BigDecimal("80.00"))
                    .montoFinal(new BigDecimal("80.00"))
                    .metodoPago("EFECTIVO")
                    .fechaPago(hoy.atTime(7, 45))
                    .estado(EstadoPago.PAGADO).build());
            log.info("✅ Cita 1 CONFIRMADA — María García 08:00");
        }

        // Cita 2 — CONFIRMADA con pago
        if (!citaRepository.existeConflictoHorario(m1,
                hoy.atTime(9, 0))) {
            CitaMedica c2 = citaRepository.save(CitaMedica.builder()
                    .paciente(p2).medico(m1)
                    .fechaHora(hoy.atTime(9, 0))
                    .motivo("Dolor de cabeza frecuente")
                    .estado(EstadoCita.CONFIRMADA)
                    .registradoPor(admin).build());
            pagoRepository.save(Pago.builder()
                    .cita(c2).monto(new BigDecimal("80.00"))
                    .montoFinal(new BigDecimal("80.00"))
                    .metodoPago("TARJETA")
                    .fechaPago(hoy.atTime(8, 50))
                    .estado(EstadoPago.PAGADO).build());
            log.info("✅ Cita 2 CONFIRMADA — José Rodríguez 09:00");
        }

        // Cita 3 — PENDIENTE (aún no paga)
        if (!citaRepository.existeConflictoHorario(m1,
                hoy.atTime(10, 0))) {
            CitaMedica c3 = citaRepository.save(CitaMedica.builder()
                    .paciente(p3).medico(m1)
                    .fechaHora(hoy.atTime(10, 0))
                    .motivo("Chequeo general anual")
                    .estado(EstadoCita.PENDIENTE)
                    .registradoPor(admin).build());
            pagoRepository.save(Pago.builder()
                    .cita(c3).monto(new BigDecimal("80.00"))
                    .montoFinal(new BigDecimal("80.00"))
                    .estado(EstadoPago.PENDIENTE).build());
            log.info("✅ Cita 3 PENDIENTE — Carmen Flores 10:00");
        }

        // Cita 4 — CONFIRMADA con cardióloga
        if (!citaRepository.existeConflictoHorario(m2,
                hoy.atTime(9, 0))) {
            CitaMedica c4 = citaRepository.save(CitaMedica.builder()
                    .paciente(p4).medico(m2)
                    .fechaHora(hoy.atTime(9, 0))
                    .motivo("Evaluación cardíaca")
                    .estado(EstadoCita.CONFIRMADA)
                    .registradoPor(admin).build());
            pagoRepository.save(Pago.builder()
                    .cita(c4).monto(new BigDecimal("80.00"))
                    .montoFinal(new BigDecimal("80.00"))
                    .metodoPago("TRANSFERENCIA")
                    .fechaPago(hoy.atTime(8, 55))
                    .estado(EstadoPago.PAGADO).build());
            log.info("✅ Cita 4 CONFIRMADA — Pedro Díaz 09:00 Cardiología");
        }

        // Cita 5 — CANCELADA
        if (!citaRepository.existeConflictoHorario(m1,
                hoy.atTime(11, 0))) {
            CitaMedica c5 = citaRepository.save(CitaMedica.builder()
                    .paciente(p5).medico(m1)
                    .fechaHora(hoy.atTime(11, 0))
                    .motivo("Consulta general")
                    .estado(EstadoCita.CANCELADA)
                    .registradoPor(admin).build());
            pagoRepository.save(Pago.builder()
                    .cita(c5).monto(new BigDecimal("80.00"))
                    .montoFinal(new BigDecimal("80.00"))
                    .estado(EstadoPago.ANULADO).build());
            log.info("✅ Cita 5 CANCELADA — Rosa Mamani 11:00");
        }

        log.info("✅ Citas de hoy creadas correctamente");
    }
}