package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.model.HorarioMedico;
import pe.edu.utp.clinica.model.Medico;
import pe.edu.utp.clinica.repository.CitaMedicaRepository;
import pe.edu.utp.clinica.repository.HorarioMedicoRepository;
import pe.edu.utp.clinica.repository.MedicoRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para calcular disponibilidad de médicos.
 *
 * RF-12: Muestra horarios disponibles al registrar cita.
 * Calcula slots de 45 minutos dentro del horario del médico,
 * excluyendo los que ya tienen cita registrada.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private static final int DURACION_CITA_MINUTOS = 45;

    private final MedicoRepository       medicoRepository;
    private final HorarioMedicoRepository horarioRepository;
    private final CitaMedicaRepository   citaRepository;

    /**
     * Devuelve los días de la semana en que el médico trabaja.
     * RF-12: Permite al frontend deshabilitar fechas no laborables.
     *
     * @param medicoId ID del médico
     * @return lista de nombres de días (MONDAY, TUESDAY, etc.)
     */
    @Transactional(readOnly = true)
    public List<String> obtenerDiasLaborables(Long medicoId) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Médico no encontrado: " + medicoId));

        return horarioRepository.findByMedico(medico)
                .stream()
                .map(h -> h.getDia().name())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Calcula los slots disponibles para un médico en una fecha específica.
     *
     * Algoritmo:
     * 1. Busca el horario del médico para el día de la semana de la fecha
     * 2. Genera slots cada 45 minutos dentro del rango horaInicio–horaFin
     * 3. Busca las citas ya registradas para ese médico en esa fecha
     * 4. Marca como OCUPADO los slots que ya tienen cita (±44 min)
     * 5. Retorna solo los slots DISPONIBLES
     *
     * @param medicoId ID del médico
     * @param fechaStr fecha en formato yyyy-MM-dd
     * @return lista de mapas con {hora, estado, fechaHora}
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> obtenerSlotsDisponibles(Long medicoId, String fechaStr) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Médico no encontrado: " + medicoId));

        LocalDate fecha  = LocalDate.parse(fechaStr);
        DayOfWeek diaSemana = fecha.getDayOfWeek();

        // Buscar horario del médico para ese día
        List<HorarioMedico> horariosDia = horarioRepository
                .findByMedicoAndDia(medico, diaSemana);

        if (horariosDia.isEmpty()) {
            log.debug("Médico ID: {} no trabaja el {}", medicoId, diaSemana);
            return Collections.emptyList();
        }

        // Buscar citas ya registradas ese día para ese médico
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia    = fecha.atTime(23, 59, 59);
        List<LocalDateTime> citasExistentes = citaRepository
                .findCitasParaRecordatorio(inicioDia, finDia)
                .stream()
                .filter(c -> c.getMedico().getId().equals(medicoId))
                .map(c -> c.getFechaHora())
                .collect(Collectors.toList());

        // Generar slots para cada rango horario del médico ese día
        List<Map<String, String>> slots = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

        for (HorarioMedico horario : horariosDia) {
            LocalTime cursor = horario.getHoraInicio();
            LocalTime limite = horario.getHoraFin()
                    .minusMinutes(DURACION_CITA_MINUTOS - 1);

            while (!cursor.isAfter(limite)) {
                LocalDateTime slotDateTime = fecha.atTime(cursor);

                // Verificar si ya hay una cita cercana (±44 min)
                boolean ocupado = citasExistentes.stream().anyMatch(citaHora ->
                    Math.abs(java.time.Duration.between(citaHora, slotDateTime)
                            .toMinutes()) < DURACION_CITA_MINUTOS
                );

                // Solo devolver slots futuros y disponibles
                if (!ocupado && slotDateTime.isAfter(LocalDateTime.now())) {
                    Map<String, String> slot = new LinkedHashMap<>();
                    slot.put("hora",      cursor.format(fmt));
                    slot.put("fechaHora", slotDateTime.format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
                    slot.put("estado",    "DISPONIBLE");
                    slots.add(slot);
                }

                cursor = cursor.plusMinutes(DURACION_CITA_MINUTOS);
            }
        }

        log.debug("Slots disponibles para médico ID: {} en {}: {}",
                medicoId, fechaStr, slots.size());
        return slots;
    }
}