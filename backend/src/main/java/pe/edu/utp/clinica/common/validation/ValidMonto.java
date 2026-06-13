package pe.edu.utp.clinica.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotación para validar montos de pago.
 * Rango: S/ 1.00 a S/ 9,999.99 con máximo 2 decimales.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Documented
@Constraint(validatedBy = MontoValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMonto {

    String message() default "El monto no es válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}