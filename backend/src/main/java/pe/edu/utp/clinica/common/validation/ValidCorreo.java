package pe.edu.utp.clinica.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Valida formato de correo electrónico con reglas más estrictas que @Email.
 *
 * Reglas adicionales:
 *  - Dominio debe tener extensión (ej: .com, .pe, .org)
 *  - No permite caracteres especiales peligrosos
 *  - Longitud máxima 100 caracteres
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Documented
@Constraint(validatedBy = CorreoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCorreo {

    String message() default
        "El correo electrónico no tiene un formato válido";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}