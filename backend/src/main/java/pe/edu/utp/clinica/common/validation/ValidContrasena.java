package pe.edu.utp.clinica.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Valida que la contraseña cumpla los requisitos de seguridad.
 *
 * Reglas:
 *  - Mínimo 8 caracteres
 *  - Al menos una letra mayúscula
 *  - Al menos una letra minúscula
 *  - Al menos un número
 *  - Al menos un carácter especial (@$!%*?&)
 *
 * Uso: @ValidContrasena en campos de contraseña de los DTOs del portal.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Documented
@Constraint(validatedBy = ContrasenaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidContrasena {

    String message() default
        "La contraseña debe tener mínimo 8 caracteres, " +
        "una mayúscula, una minúscula, un número y un carácter especial (@$!%*?&)";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}