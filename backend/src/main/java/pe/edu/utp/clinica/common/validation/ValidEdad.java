package pe.edu.utp.clinica.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotación para validar fecha de nacimiento de paciente.
 * Verifica que la edad sea coherente (0 a 120 años).
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Documented
@Constraint(validatedBy = EdadValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEdad {

    String message() default "La fecha de nacimiento no es válida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}