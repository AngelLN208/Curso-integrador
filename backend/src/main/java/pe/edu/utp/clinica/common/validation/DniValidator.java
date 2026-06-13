package pe.edu.utp.clinica.common.validation;

import com.google.common.base.Strings;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador personalizado para DNI peruano.
 * Usa Google Guava — requerido por rúbrica.
 *
 * Reglas:
 * - Exactamente 8 dígitos numéricos
 * - No puede ser todo ceros (00000000)
 * - No puede ser nulo ni vacío
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public class DniValidator implements
        ConstraintValidator<ValidDni, String> {

    @Override
    public boolean isValid(String dni,
            ConstraintValidatorContext context) {

        // Guava: verifica nulo o vacío
        if (Strings.isNullOrEmpty(dni)) return false;

        // Exactamente 8 dígitos
        if (!dni.matches("\\d{8}")) return false;

        // No puede ser todo ceros
        if (dni.equals("00000000")) return false;

        return true;
    }
}