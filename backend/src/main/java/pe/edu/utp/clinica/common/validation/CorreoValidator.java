package pe.edu.utp.clinica.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementación del validador de correo electrónico.
 * Más estricto que @Email de Jakarta — verifica dominio y extensión.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public class CorreoValidator
        implements ConstraintValidator<ValidCorreo, String> {

    private static final String PATRON =
        "^[a-zA-Z0-9._%+\\-]{1,64}@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,10}$";

    @Override
    public boolean isValid(String value,
                           ConstraintValidatorContext context) {
        if (value == null) return true;
        return value.matches(PATRON) && value.length() <= 100;
    }
}