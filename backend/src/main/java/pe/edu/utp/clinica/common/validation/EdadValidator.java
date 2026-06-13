package pe.edu.utp.clinica.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

/**
 * Validador de fecha de nacimiento para pacientes.
 *
 * Reglas:
 * - El paciente debe tener al menos 0 años (recién nacido)
 * - No puede tener más de 120 años
 * - La fecha no puede ser futura
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public class EdadValidator implements
        ConstraintValidator<ValidEdad, LocalDate> {

    @Override
    public boolean isValid(LocalDate fechaNacimiento,
            ConstraintValidatorContext context) {

        if (fechaNacimiento == null) return false;

        LocalDate hoy = LocalDate.now();

        // No puede ser fecha futura
        if (fechaNacimiento.isAfter(hoy)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "La fecha de nacimiento no puede ser futura")
                    .addConstraintViolation();
            return false;
        }

        int edad = Period.between(fechaNacimiento, hoy).getYears();

        // Máximo 120 años
        if (edad > 120) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "La fecha de nacimiento no es válida — supera los 120 años")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}