package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.dto.cita.CitaRequest;
import pe.edu.utp.clinica.dto.cita.CitaReprogramarRequest;
import pe.edu.utp.clinica.dto.cita.CitaResponse;
import pe.edu.utp.clinica.service.CitaService;

import java.util.List;

/**
 * Controlador para gestión de citas médicas.
 *
 * RF-05: Registrar cita.
 * RF-06: Reprogramar cita.
 * RF-07: Buscar citas por filtros.
 * RF-08: Listar citas.
 * RF-09: Cancelar cita.
 * Actor principal: RECEPCIONISTA.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
@Tag(name = "Citas Médicas", description = "Gestión de citas médicas")
@PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMINISTRADOR')")
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    @Operation(summary = "Registrar cita médica",
               description = "RF-05: Estado inicial PENDIENTE. "
                           + "RF-10: Valida disponibilidad. "
                           + "RF-11: Genera pago automático.")
    public ResponseEntity<ApiResponse<CitaResponse>> registrar(
            @Valid @RequestBody CitaRequest request,
            Authentication auth) {

        CitaResponse response = citaService.registrar(request, auth.getName());
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Cita registrada correctamente", response));
    }

    @PutMapping("/{id}/reprogramar")
    @Operation(summary = "Reprogramar cita médica",
               description = "RF-06: Cambia estado a REPROGRAMADA. "
                           + "Registra en auditoría.")
    public ResponseEntity<ApiResponse<CitaResponse>> reprogramar(
            @PathVariable Long id,
            @Valid @RequestBody CitaReprogramarRequest request,
            Authentication auth) {

        CitaResponse response = citaService.reprogramar(id, request, auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Cita reprogramada correctamente", response));
    }

    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar cita médica",
               description = "RF-09: Cambia estado a CANCELADA. "
                           + "No se puede cancelar dos veces.")
    public ResponseEntity<ApiResponse<CitaResponse>> cancelar(
            @PathVariable Long id,
            Authentication auth) {

        CitaResponse response = citaService.cancelar(id, auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success("Cita cancelada correctamente", response));
    }

    @GetMapping
    @Operation(summary = "Listar todas las citas",
               description = "RF-08: Lista con ID, fecha, hora y estado.")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> listar() {

        List<CitaResponse> response = citaService.listarTodas();
        return ResponseEntity.ok(
                ApiResponse.success("Citas obtenidas correctamente", response));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar citas por filtros",
               description = "RF-07: Filtra por pacienteId, medicoId, estado.")
    public ResponseEntity<ApiResponse<List<CitaResponse>>> buscar(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) String fecha) {

        List<CitaResponse> response = citaService
                .buscarPorFiltros(pacienteId, medicoId, estado, fecha);
        return ResponseEntity.ok(
                ApiResponse.success("Búsqueda completada", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cita por ID")
    public ResponseEntity<ApiResponse<CitaResponse>> obtenerPorId(
            @PathVariable Long id) {

        CitaResponse response = citaService.obtenerPorId(id);
        return ResponseEntity.ok(
                ApiResponse.success("Cita obtenida correctamente", response));
    }
}