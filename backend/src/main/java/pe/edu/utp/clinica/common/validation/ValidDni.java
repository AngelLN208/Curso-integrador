package pe.edu.utp.clinica.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotación personalizada para validar DNI peruano.
 *
 * Uso: @ValidDni en campos String de DTOs y entidades.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Documented
@Constraint(validatedBy = DniValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDni {

    String message() default
            "El DNI debe tener exactamente 8 dígitos numéricos válidos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}