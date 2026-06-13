package pe.edu.utp.clinica.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

/**
 * Validador personalizado para fechas de cita.
 * Reglas:
 * - La fecha debe ser futura
 * - No puede ser más de 90 días en el futuro
 * - Hora de atención: 7am a 8pm
 *
 * @author Equipo Curso Integrador UTP 2026
 */
public class FechaFuturaValidator implements
        ConstraintValidator<ValidFechaCita, LocalDateTime> {

    @Override
    public boolean isValid(LocalDateTime fecha,
            ConstraintValidatorContext context) {

        if (fecha == null) return false;

        LocalDateTime ahora = LocalDateTime.now();

        // Debe ser futura
        if (!fecha.isAfter(ahora)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "La fecha de la cita debe ser posterior a la fecha actual")
                    .addConstraintViolation();
            return false;
        }

        // Máximo 90 días en el futuro
        if (fecha.isAfter(ahora.plusDays(90))) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "No se pueden agendar citas con más de 90 días de anticipación")
                    .addConstraintViolation();
            return false;
        }

        // Horario de atención: 7am a 8pm
        int hora = fecha.getHour();
        if (hora < 7 || hora >= 20) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Las citas solo se pueden agendar entre 7:00 AM y 8:00 PM")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}