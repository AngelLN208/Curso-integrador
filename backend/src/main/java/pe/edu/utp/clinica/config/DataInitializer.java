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
        private final SeguroMedicoRepository seguroRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) {
                crearUsuarioAdministrador();
                crearUsuarioRecepcionista();
                crearEspecialidades();
                crearMedicos();
                crearPacientes();
                crearSeguros();
                crearCitasDeHoy();
        }

        // ─── Usuarios ─────────────────────────────────────────────────────

        private void crearUsuarioAdministrador() {
                String username = "admin@clinica.pe";
                if (usuarioRepository.existsByUsername(username))
                        return;

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
                if (usuarioRepository.existsByUsername(username))
                        return;

                usuarioRepository.save(Usuario.builder()
                                .username(username)
                                .password(passwordEncoder.encode("Recep123*"))
                                .nombreCompleto("Recepcionista Principal")
                                .rol(RolUsuario.ROLE_RECEPCIONISTA)
                                .activo(true).build());
                log.info("✅ Recepcionista creado: {}", username);
        }

        // ─── Especialidades ───────────────────────────────────────────────
        // Precios de referencia para clínicas privadas de nivel medio en
        // Lima (estimación de mercado 2026, no un dato verificado exacto).

        private void crearEspecialidades() {
                Object[][] especialidades = {
                                { "Medicina General", "Atención primaria y consultas generales",
                                                new BigDecimal("70.00") },
                                { "Cardiología", "Diagnóstico y tratamiento de enfermedades cardíacas",
                                                new BigDecimal("150.00") },
                                { "Pediatría", "Atención médica para niños y adolescentes", new BigDecimal("90.00") },
                                { "Ginecología", "Salud reproductiva y atención femenina", new BigDecimal("120.00") },
                                { "Traumatología", "Lesiones del sistema musculoesquelético",
                                                new BigDecimal("130.00") },
                                { "Dermatología", "Enfermedades de la piel", new BigDecimal("140.00") },
                                { "Neurología", "Trastornos del sistema nervioso", new BigDecimal("160.00") },
                                { "Oftalmología", "Enfermedades de los ojos", new BigDecimal("110.00") },
                                { "Psiquiatría", "Salud mental y trastornos psiquiátricos", new BigDecimal("180.00") },
                                { "Endocrinología", "Trastornos hormonales y metabólicos", new BigDecimal("140.00") }
                };

                for (Object[] esp : especialidades) {
                        String nombre = (String) esp[0];
                        if (!especialidadRepository.existsByNombreIgnoreCase(nombre)) {
                                especialidadRepository.save(Especialidad.builder()
                                                .nombre(nombre)
                                                .descripcion((String) esp[1])
                                                .costo((BigDecimal) esp[2])
                                                .activo(true).build());
                                log.info("✅ Especialidad creada: {}", nombre);
                        }
                }
        }

        // ─── Médicos ──────────────────────────────────────────────────────
        // Un médico por cada una de las 10 especialidades registradas.

        private void crearMedicos() {
                if (medicoRepository.existsByDni("45123678"))
                        return;

                Especialidad medGeneral = especialidadRepository.findByNombreIgnoreCase("Medicina General")
                                .orElseThrow();
                Especialidad cardiologia = especialidadRepository.findByNombreIgnoreCase("Cardiología")
                                .orElseThrow();
                Especialidad pediatria = especialidadRepository.findByNombreIgnoreCase("Pediatría").orElseThrow();
                Especialidad ginecologia = especialidadRepository.findByNombreIgnoreCase("Ginecología").orElseThrow();
                Especialidad traumatologia = especialidadRepository.findByNombreIgnoreCase("Traumatología")
                                .orElseThrow();
                Especialidad dermatologia = especialidadRepository.findByNombreIgnoreCase("Dermatología")
                                .orElseThrow();
                Especialidad neurologia = especialidadRepository.findByNombreIgnoreCase("Neurología").orElseThrow();
                Especialidad oftalmologia = especialidadRepository.findByNombreIgnoreCase("Oftalmología")
                                .orElseThrow();
                Especialidad psiquiatria = especialidadRepository.findByNombreIgnoreCase("Psiquiatría").orElseThrow();
                Especialidad endocrinologia = especialidadRepository.findByNombreIgnoreCase("Endocrinología")
                                .orElseThrow();

                // Médico 1 — Medicina General
                Usuario u1 = usuarioRepository.save(Usuario.builder()
                                .username("carlos.lopez@clinica.pe")
                                .password(passwordEncoder.encode("Medico123*"))
                                .nombreCompleto("Carlos López Ramírez")
                                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());
                Medico m1 = medicoRepository.save(Medico.builder()
                                .dni("45123678").nombres("Carlos").apellidos("López Ramírez")
                                .especialidad(medGeneral).celular("987111222")
                                .correo("carlos.lopez@clinica.pe").usuario(u1).activo(true).build());
                for (DayOfWeek dia : new DayOfWeek[] {
                                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY }) {
                        horarioRepository.save(HorarioMedico.builder()
                                        .medico(m1).dia(dia)
                                        .horaInicio(LocalTime.of(8, 0))
                                        .horaFin(LocalTime.of(14, 0)).build());
                }

                // Médico 2 — Cardiología
                Usuario u2 = usuarioRepository.save(Usuario.builder()
                                .username("ana.torres@clinica.pe")
                                .password(passwordEncoder.encode("Medico123*"))
                                .nombreCompleto("Ana Torres Vega")
                                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());
                Medico m2 = medicoRepository.save(Medico.builder()
                                .dni("52398741").nombres("Ana").apellidos("Torres Vega")
                                .especialidad(cardiologia).celular("987333444")
                                .correo("ana.torres@clinica.pe").usuario(u2).activo(true).build());
                for (DayOfWeek dia : new DayOfWeek[] {
                                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY }) {
                        horarioRepository.save(HorarioMedico.builder()
                                        .medico(m2).dia(dia)
                                        .horaInicio(LocalTime.of(9, 0))
                                        .horaFin(LocalTime.of(13, 0)).build());
                }

                // Médico 3 — Pediatría
                Usuario u3 = usuarioRepository.save(Usuario.builder()
                                .username("luis.mendoza@clinica.pe")
                                .password(passwordEncoder.encode("Medico123*"))
                                .nombreCompleto("Luis Mendoza Castillo")
                                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());
                Medico m3 = medicoRepository.save(Medico.builder()
                                .dni("63741852").nombres("Luis").apellidos("Mendoza Castillo")
                                .especialidad(pediatria).celular("987555666")
                                .correo("luis.mendoza@clinica.pe").usuario(u3).activo(true).build());
                for (DayOfWeek dia : new DayOfWeek[] {
                                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY }) {
                        horarioRepository.save(HorarioMedico.builder()
                                        .medico(m3).dia(dia)
                                        .horaInicio(LocalTime.of(8, 0))
                                        .horaFin(LocalTime.of(12, 0)).build());
                }

                // Médico 4 — Ginecología
                Usuario u4 = usuarioRepository.save(Usuario.builder()
                                .username("patricia.salazar@clinica.pe")
                                .password(passwordEncoder.encode("Medico123*"))
                                .nombreCompleto("Patricia Salazar Ruiz")
                                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());
                Medico m4 = medicoRepository.save(Medico.builder()
                                .dni("48234567").nombres("Patricia").apellidos("Salazar Ruiz")
                                .especialidad(ginecologia).celular("987222111")
                                .correo("patricia.salazar@clinica.pe").usuario(u4).activo(true).build());
                for (DayOfWeek dia : new DayOfWeek[] {
                                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY }) {
                        horarioRepository.save(HorarioMedico.builder()
                                        .medico(m4).dia(dia)
                                        .horaInicio(LocalTime.of(9, 0))
                                        .horaFin(LocalTime.of(13, 0)).build());
                }

                // Médico 5 — Traumatología
                Usuario u5 = usuarioRepository.save(Usuario.builder()
                                .username("jorge.ramirez@clinica.pe")
                                .password(passwordEncoder.encode("Medico123*"))
                                .nombreCompleto("Jorge Ramírez Ochoa")
                                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());
                Medico m5 = medicoRepository.save(Medico.builder()
                                .dni("47345678").nombres("Jorge").apellidos("Ramírez Ochoa")
                                .especialidad(traumatologia).celular("987444555")
                                .correo("jorge.ramirez@clinica.pe").usuario(u5).activo(true).build());
                for (DayOfWeek dia : new DayOfWeek[] {
                                DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY }) {
                        horarioRepository.save(HorarioMedico.builder()
                                        .medico(m5).dia(dia)
                                        .horaInicio(LocalTime.of(8, 0))
                                        .horaFin(LocalTime.of(12, 0)).build());
                }

                // Médico 6 — Dermatología
                Usuario u6 = usuarioRepository.save(Usuario.builder()
                                .username("fiorella.castro@clinica.pe")
                                .password(passwordEncoder.encode("Medico123*"))
                                .nombreCompleto("Fiorella Castro Núñez")
                                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());
                Medico m6 = medicoRepository.save(Medico.builder()
                                .dni("46456789").nombres("Fiorella").apellidos("Castro Núñez")
                                .especialidad(dermatologia).celular("987666777")
                                .correo("fiorella.castro@clinica.pe").usuario(u6).activo(true).build());
                for (DayOfWeek dia : new DayOfWeek[] { DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY }) {
                        horarioRepository.save(HorarioMedico.builder()
                                        .medico(m6).dia(dia)
                                        .horaInicio(LocalTime.of(10, 0))
                                        .horaFin(LocalTime.of(14, 0)).build());
                }

                // Médico 7 — Neurología
                Usuario u7 = usuarioRepository.save(Usuario.builder()
                                .username("ricardo.paredes@clinica.pe")
                                .password(passwordEncoder.encode("Medico123*"))
                                .nombreCompleto("Ricardo Paredes Guevara")
                                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());
                Medico m7 = medicoRepository.save(Medico.builder()
                                .dni("49567890").nombres("Ricardo").apellidos("Paredes Guevara")
                                .especialidad(neurologia).celular("987888999")
                                .correo("ricardo.paredes@clinica.pe").usuario(u7).activo(true).build());
                for (DayOfWeek dia : new DayOfWeek[] { DayOfWeek.TUESDAY, DayOfWeek.THURSDAY }) {
                        horarioRepository.save(HorarioMedico.builder()
                                        .medico(m7).dia(dia)
                                        .horaInicio(LocalTime.of(9, 0))
                                        .horaFin(LocalTime.of(13, 0)).build());
                }

                // Médico 8 — Oftalmología
                Usuario u8 = usuarioRepository.save(Usuario.builder()
                                .username("silvia.cabrera@clinica.pe")
                                .password(passwordEncoder.encode("Medico123*"))
                                .nombreCompleto("Silvia Cabrera León")
                                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());
                Medico m8 = medicoRepository.save(Medico.builder()
                                .dni("44678901").nombres("Silvia").apellidos("Cabrera León")
                                .especialidad(oftalmologia).celular("987101112")
                                .correo("silvia.cabrera@clinica.pe").usuario(u8).activo(true).build());
                for (DayOfWeek dia : new DayOfWeek[] {
                                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY }) {
                        horarioRepository.save(HorarioMedico.builder()
                                        .medico(m8).dia(dia)
                                        .horaInicio(LocalTime.of(8, 0))
                                        .horaFin(LocalTime.of(12, 0)).build());
                }

                // Médico 9 — Psiquiatría
                Usuario u9 = usuarioRepository.save(Usuario.builder()
                                .username("manuel.ortega@clinica.pe")
                                .password(passwordEncoder.encode("Medico123*"))
                                .nombreCompleto("Manuel Ortega Salinas")
                                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());
                Medico m9 = medicoRepository.save(Medico.builder()
                                .dni("43789012").nombres("Manuel").apellidos("Ortega Salinas")
                                .especialidad(psiquiatria).celular("987131415")
                                .correo("manuel.ortega@clinica.pe").usuario(u9).activo(true).build());
                for (DayOfWeek dia : new DayOfWeek[] { DayOfWeek.TUESDAY, DayOfWeek.THURSDAY }) {
                        horarioRepository.save(HorarioMedico.builder()
                                        .medico(m9).dia(dia)
                                        .horaInicio(LocalTime.of(14, 0))
                                        .horaFin(LocalTime.of(18, 0)).build());
                }

                // Médico 10 — Endocrinología
                Usuario u10 = usuarioRepository.save(Usuario.builder()
                                .username("lucia.herrera@clinica.pe")
                                .password(passwordEncoder.encode("Medico123*"))
                                .nombreCompleto("Lucía Herrera Campos")
                                .rol(RolUsuario.ROLE_MEDICO).activo(true).build());
                Medico m10 = medicoRepository.save(Medico.builder()
                                .dni("40890123").nombres("Lucía").apellidos("Herrera Campos")
                                .especialidad(endocrinologia).celular("987161718")
                                .correo("lucia.herrera@clinica.pe").usuario(u10).activo(true).build());
                for (DayOfWeek dia : new DayOfWeek[] { DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY }) {
                        horarioRepository.save(HorarioMedico.builder()
                                        .medico(m10).dia(dia)
                                        .horaInicio(LocalTime.of(9, 0))
                                        .horaFin(LocalTime.of(13, 0)).build());
                }

                log.info("✅ 10 médicos creados (uno por especialidad): López, Torres, Mendoza, Salazar, "
                                + "Ramírez, Castro, Paredes, Cabrera, Ortega, Herrera");
        }

        // ─── Pacientes ────────────────────────────────────────────────────

        private void crearPacientes() {
                if (pacienteRepository.existsByDni("71234567"))
                        return;

                Object[][] pacientes = {
                                { "71234567", "María", "García Pérez", LocalDate.of(1985, 3, 12), "987001001",
                                                "maria.garcia@gmail.com", "F" },
                                { "72345678", "José", "Rodríguez Soto", LocalDate.of(1990, 7, 25), "987002002",
                                                "jose.rodriguez@gmail.com", "M" },
                                { "73456789", "Carmen", "Flores Lima", LocalDate.of(1978, 11, 5), "987003003",
                                                "carmen.flores@gmail.com", "F" },
                                { "74567890", "Pedro", "Díaz Quispe", LocalDate.of(1995, 1, 18), "987004004",
                                                "pedro.diaz@gmail.com", "M" },
                                { "75678901", "Rosa", "Mamani Torres", LocalDate.of(2000, 6, 30), "987005005",
                                                "rosa.mamani@gmail.com", "F" },
                                { "76789012", "Luis", "Cárdenas Vega", LocalDate.of(1972, 9, 14), "987006006",
                                                "luis.cardenas@gmail.com", "M" },
                                { "77890123", "Ana", "Huanca Ramos", LocalDate.of(1988, 4, 22), "987007007",
                                                "ana.huanca@gmail.com", "F" },
                                { "78901234", "Carlos", "Vargas Suárez", LocalDate.of(2003, 12, 8), "987008008",
                                                "carlos.vargas@gmail.com", "M" },
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

        // ─── Seguros médicos ──────────────────────────────────────────────

        private void crearSeguros() {
                if (seguroRepository.existsByNombreIgnoreCase("Rímac Salud"))
                        return;

                Object[][] seguros = {
                                // nombre, tipo, porcentajeCobertura, deducible
                                { "Rímac Salud", "PRIVADO", new BigDecimal("30.00"), new BigDecimal("20.00") },
                                { "Pacífico Seguros", "PRIVADO", new BigDecimal("25.00"), new BigDecimal("15.00") },
                                { "EsSalud", "PUBLICO", new BigDecimal("50.00"), BigDecimal.ZERO },
                                { "SIS", "PUBLICO", new BigDecimal("100.00"), BigDecimal.ZERO },
                                { "Mapfre Salud", "PRIVADO", new BigDecimal("20.00"), new BigDecimal("25.00") },
                                { "La Positiva Salud", "PRIVADO", new BigDecimal("20.00"), new BigDecimal("20.00") },
                };

                for (Object[] s : seguros) {
                        seguroRepository.save(SeguroMedico.builder()
                                        .nombre((String) s[0])
                                        .tipo((String) s[1])
                                        .porcentajeCobertura((BigDecimal) s[2])
                                        .deducible((BigDecimal) s[3])
                                        .convenioActivo(true)
                                        .build());
                }
                log.info("✅ 6 seguros médicos creados (catálogo inicial)");
        }

        // ─── Citas de hoy ─────────────────────────────────────────────────
        // Los montos ahora corresponden al costo real de la especialidad
        // de cada médico (antes estaban fijos en 80.00 para todos).

        private void crearCitasDeHoy() {
                Paciente p1 = pacienteRepository.findByDni("71234567").orElseThrow();
                Paciente p2 = pacienteRepository.findByDni("72345678").orElseThrow();
                Paciente p3 = pacienteRepository.findByDni("73456789").orElseThrow();
                Paciente p4 = pacienteRepository.findByDni("74567890").orElseThrow();
                Paciente p5 = pacienteRepository.findByDni("75678901").orElseThrow();

                Medico m1 = medicoRepository.findByDni("45123678").orElseThrow(); // Medicina General — 70.00
                Medico m2 = medicoRepository.findByDni("52398741").orElseThrow(); // Cardiología — 150.00

                Usuario admin = usuarioRepository.findByUsername("admin@clinica.pe").orElseThrow();

                LocalDate hoy = LocalDate.now();
                BigDecimal costoM1 = m1.getEspecialidad().getCosto();
                BigDecimal costoM2 = m2.getEspecialidad().getCosto();

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
                                        .cita(c1).monto(costoM1)
                                        .montoFinal(costoM1)
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
                                        .cita(c2).monto(costoM1)
                                        .montoFinal(costoM1)
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
                                        .cita(c3).monto(costoM1)
                                        .montoFinal(costoM1)
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
                                        .cita(c4).monto(costoM2)
                                        .montoFinal(costoM2)
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
                                        .cita(c5).monto(costoM1)
                                        .montoFinal(costoM1)
                                        .estado(EstadoPago.ANULADO).build());
                        log.info("✅ Cita 5 CANCELADA — Rosa Mamani 11:00");
                }

                log.info("✅ Citas de hoy creadas correctamente");
        }
}