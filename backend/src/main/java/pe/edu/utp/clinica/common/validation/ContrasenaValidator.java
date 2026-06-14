package pe.edu.utp.clinica.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementación del validador de contraseña segura.
 *
 * Patrón: mínimo 8 chars, mayúscula, minúscula, número y símbolo.
 * Null es válido — usar @NotBlank en conjunto si el campo es obligatorio.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public class ContrasenaValidator
        implements ConstraintValidator<ValidContrasena, String> {

    // Al menos: 8 chars, 1 mayúscula, 1 minúscula, 1 dígito, 1 especial
    private static final String PATRON =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    @Override
    public boolean isValid(String value,
                           ConstraintValidatorContext context) {
        if (value == null) return true; // @NotBlank se encarga del null
        return value.matches(PATRON);
    }
}