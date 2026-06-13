package pe.edu.utp.clinica.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger / OpenAPI 3.
 *
 * Habilita el botón "Authorize" en Swagger UI para enviar
 * el token JWT en todas las peticiones de prueba.
 *
 * Acceso: http://localhost:8080/swagger-ui.html
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "API - Sistema de Gestión de Citas Médicas",
        version = "1.0.0",
        description = "Clínica Stella Maris · Lima, Perú · UTP 2026",
        contact = @Contact(
            name = "Equipo Curso Integrador UTP",
            email = "equipo@utp.edu.pe"
        )
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Ingresa el token JWT obtenido en POST /api/auth/login"
)
public class SwaggerConfig {
    // La configuración se hace solo con anotaciones
}