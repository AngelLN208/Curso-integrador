package pe.edu.utp.clinica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal del Sistema de Gestión de Citas Médicas.
 * Clínica Stella Maris - Lima, Perú.
 *
 * <p>Arquitectura: MVC + DAO + SOLID
 * <p>Tecnologías: Spring Boot 3.3, PostgreSQL, JWT, Swagger
 *
 * @author Equipo Curso Integrador UTP 2026
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling  // Habilita el scheduler para notificaciones (RF-47)
public class ClinicaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicaApplication.class, args);
    }
}