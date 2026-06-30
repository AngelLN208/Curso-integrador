package pe.edu.utp.clinica.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import pe.edu.utp.clinica.dto.cita.CitaResponse;
import pe.edu.utp.clinica.dto.portal.EspecialidadDirectorioResponse;
import pe.edu.utp.clinica.dto.portal.MedicoDirectorioResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Ejecuta las herramientas (tools) que el chatbot puede invocar para
 * consultar médicos, disponibilidad real y agendar citas.
 * RF-55 (extendido): el chatbot puede actuar sobre datos reales,
 * no solo responder con texto genérico.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotToolService {

    private final PortalService portalService;
    private final DisponibilidadService disponibilidadService;
    private final CitaService citaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Busca médicos por especialidad y/o nombre (búsqueda parcial).
     * Reutiliza el directorio público, ya filtrando en memoria.
     */
    public JsonNode buscarMedicos(String especialidad, String nombre) {
        List<EspecialidadDirectorioResponse> directorio = portalService.obtenerDirectorio();

        var listaMedicos = objectMapper.createArrayNode();

        for (EspecialidadDirectorioResponse esp : directorio) {
            if (especialidad != null && !especialidad.isBlank()
                    && !esp.getNombre().toLowerCase().contains(especialidad.toLowerCase())) {
                continue;
            }

            for (MedicoDirectorioResponse m : esp.getMedicos()) {
                if (nombre != null && !nombre.isBlank()
                        && !m.getNombreCompleto().toLowerCase().contains(nombre.toLowerCase())) {
                    continue;
                }

                ObjectNode medicoJson = objectMapper.createObjectNode();
                medicoJson.put("medicoId", m.getId());
                medicoJson.put("nombreCompleto", m.getNombreCompleto());
                medicoJson.put("especialidad", esp.getNombre());
                listaMedicos.add(medicoJson);
            }
        }

        if (listaMedicos.isEmpty()) {
            log.debug("Tool buscar_medicos sin resultados — especialidad: {} | nombre: {}", especialidad, nombre);
        }

        ObjectNode respuesta = objectMapper.createObjectNode();
        respuesta.set("medicos", listaMedicos);
        return respuesta;
    }

    /**
     * Consulta los horarios realmente disponibles de un médico en una fecha.
     * Reutiliza DisponibilidadService — el mismo cálculo que usa el
     * directorio del portal, ya corregido para excluir citas no canceladas.
     */
    public JsonNode consultarDisponibilidad(Long medicoId, String fecha) {
        List<Map<String, String>> slots = disponibilidadService.obtenerSlotsDisponibles(medicoId, fecha);

        var listaSlots = objectMapper.createArrayNode();
        for (Map<String, String> slot : slots) {
            ObjectNode slotJson = objectMapper.createObjectNode();
            slotJson.put("hora", slot.get("hora"));
            slotJson.put("fechaHora", slot.get("fechaHora"));
            listaSlots.add(slotJson);
        }

        ObjectNode respuesta = objectMapper.createObjectNode();
        respuesta.set("slots", listaSlots);
        return respuesta;
    }

    /**
     * Crea la cita real. Solo se invoca cuando el paciente ya confirmó
     * explícitamente en el flujo de conversación — el chatbot nunca debe
     * llamar esto sin una confirmación previa del usuario en texto.
     * Reutiliza la misma validación de conflictos que ya usa el resto
     * del portal (CitaService.registrarDesdePortal).
     */
    public JsonNode confirmarAgendamiento(Long medicoId, String fechaHoraIso, String motivo, String correoPaciente) {
        ObjectNode resultado = objectMapper.createObjectNode();
        try {
            LocalDateTime fechaHora = LocalDateTime.parse(fechaHoraIso);
            CitaResponse cita = citaService.registrarDesdePortal(medicoId, fechaHora, motivo, correoPaciente);

            resultado.put("exito", true);
            resultado.put("citaId", cita.getId());
            resultado.put("medicoNombre", cita.getMedicoNombre());
            resultado.put("fechaHora", cita.getFechaHora().toString());
            resultado.put("estado", cita.getEstado().toString());

        } catch (Exception ex) {
            log.warn("Tool confirmar_agendamiento falló: {}", ex.getMessage());
            resultado.put("exito", false);
            resultado.put("error", ex.getMessage());
        }
        return resultado;
    }
}