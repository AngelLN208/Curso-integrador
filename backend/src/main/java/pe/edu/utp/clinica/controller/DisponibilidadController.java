package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.service.DisponibilidadService;

import java.util.List;
import java.util.Map;

/**
 * Controller para consultar disponibilidad de médicos.
 *
 * RF-12: Mostrar horarios disponibles al registrar cita.
 * Endpoint público — no requiere autenticación para que
 * el portal paciente también pueda consultarlo.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/disponibilidad")
@RequiredArgsConstructor
@Tag(name = "Disponibilidad", description = "Consulta de horarios disponibles por médico y fecha")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    /**
     * Devuelve los días de la semana en que trabaja el médico.
     * Usado para deshabilitar fechas en el calendario del frontend.
     *
     * @param medicoId ID del médico
     * @return lista de días (MONDAY, TUESDAY, etc.)
     */
    @GetMapping("/medico/{medicoId}/dias")
    @Operation(summary = "Días laborables del médico",
               description = "Devuelve los días de la semana en que el médico tiene horario asignado.")
    public ResponseEntity<ApiResponse<List<String>>> obtenerDias(
            @PathVariable Long medicoId) {
        List<String> dias = disponibilidadService.obtenerDiasLaborables(medicoId);
        return ResponseEntity.ok(ApiResponse.success("Días obtenidos", dias));
    }

    /**
     * Devuelve los slots de tiempo disponibles para un médico en una fecha.
     * Slots cada 45 minutos dentro del horario del médico,
     * excluyendo los que ya tienen cita.
     *
     * @param medicoId ID del médico
     * @param fecha    fecha en formato yyyy-MM-dd
     * @return lista de slots disponibles con hora y estado
     */
    @GetMapping("/medico/{medicoId}/slots")
    @Operation(summary = "Slots disponibles del médico en una fecha",
               description = "RF-12: Slots cada 45 min. Excluye horarios ya ocupados.")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> obtenerSlots(
            @PathVariable Long medicoId,
            @RequestParam  String fecha) {
        List<Map<String, String>> slots =
                disponibilidadService.obtenerSlotsDisponibles(medicoId, fecha);
        return ResponseEntity.ok(ApiResponse.success("Slots obtenidos", slots));
    }
}