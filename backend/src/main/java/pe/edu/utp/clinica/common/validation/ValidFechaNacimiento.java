package pe.edu.utp.clinica.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Valida que la fecha de nacimiento sea razonable para un paciente.
 *
 * Reglas:
 *  - No puede ser una fecha futura
 *  - El paciente debe tener entre 0 y 120 años
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Documented
@Constraint(validatedBy = FechaNacimientoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFechaNacimiento {

    String message() default
        "La fecha de nacimiento no es válida. " +
        "Debe ser una fecha pasada y el paciente no puede tener más de 120 años";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}