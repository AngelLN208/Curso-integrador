package pe.edu.utp.clinica.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * Validador de monto de pago.
 *
 * Reglas:
 * - Monto mínimo: S/ 1.00
 * - Monto máximo: S/ 9,999.99
 * - Máximo 2 decimales
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public class MontoValidator implements
        ConstraintValidator<ValidMonto, BigDecimal> {

    private static final BigDecimal MINIMO = new BigDecimal("1.00");
    private static final BigDecimal MAXIMO = new BigDecimal("9999.99");

    @Override
    public boolean isValid(BigDecimal monto,
            ConstraintValidatorContext context) {

        if (monto == null) return false;

        // Mínimo S/ 1.00
        if (monto.compareTo(MINIMO) < 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "El monto mínimo es S/ 1.00")
                    .addConstraintViolation();
            return false;
        }

        // Máximo S/ 9,999.99
        if (monto.compareTo(MAXIMO) > 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "El monto máximo es S/ 9,999.99")
                    .addConstraintViolation();
            return false;
        }

        // Máximo 2 decimales
        if (monto.scale() > 2) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "El monto no puede tener más de 2 decimales")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}