package pe.edu.utp.clinica.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotación personalizada para validar fecha de cita médica.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Documented
@Constraint(validatedBy = FechaFuturaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFechaCita {

    String message() default "Fecha de cita inválida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}