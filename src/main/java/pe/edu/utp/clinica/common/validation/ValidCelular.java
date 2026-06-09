package pe.edu.utp.clinica.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotación para validar número de celular peruano.
 * 9 dígitos, empieza con 9.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Documented
@Constraint(validatedBy = CelularValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCelular {

    String message() default "El número de celular no es válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}