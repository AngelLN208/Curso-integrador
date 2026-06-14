package pe.edu.utp.clinica.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

/**
 * Implementación del validador de fecha de nacimiento.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public class FechaNacimientoValidator
        implements ConstraintValidator<ValidFechaNacimiento, LocalDate> {

    private static final int EDAD_MAXIMA = 120;

    @Override
    public boolean isValid(LocalDate fecha,
                           ConstraintValidatorContext context) {
        if (fecha == null) return true;

        LocalDate hoy       = LocalDate.now();
        LocalDate limiteMin = hoy.minusYears(EDAD_MAXIMA);

        // No puede ser futura ni mayor a 120 años
        return !fecha.isAfter(hoy) && !fecha.isBefore(limiteMin);
    }
}