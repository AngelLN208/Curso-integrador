package pe.edu.utp.clinica.common.validation;

import com.google.common.base.Strings;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador de número de celular peruano.
 * Usa Google Guava — requerido por rúbrica.
 *
 * Reglas:
 * - Debe tener exactamente 9 dígitos
 * - Debe empezar con 9 (celulares peruanos)
 * - No puede ser todo iguales (999999999)
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public class CelularValidator implements
        ConstraintValidator<ValidCelular, String> {

    @Override
    public boolean isValid(String celular,
            ConstraintValidatorContext context) {

        // Guava: verifica nulo o vacío
        if (Strings.isNullOrEmpty(celular)) return false;

        // Exactamente 9 dígitos
        if (!celular.matches("\\d{9}")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "El celular debe tener exactamente 9 dígitos")
                    .addConstraintViolation();
            return false;
        }

        // Debe empezar con 9
        if (!celular.startsWith("9")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "El celular peruano debe empezar con 9")
                    .addConstraintViolation();
            return false;
        }

        // No puede ser todos iguales
        if (celular.chars().distinct().count() == 1) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "El número de celular no es válido")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}