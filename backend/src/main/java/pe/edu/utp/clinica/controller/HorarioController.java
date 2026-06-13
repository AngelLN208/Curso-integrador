package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.dto.horario.HorarioRequest;
import pe.edu.utp.clinica.dto.horario.HorarioResponse;
import pe.edu.utp.clinica.service.HorarioService;

import java.util.List;

/**
 * Controlador para gestión de horarios de médicos.
 *
 * RF-38: Asignar horarios a médicos. Solo ADMINISTRADOR.
 * RF-12: Listar horarios disponibles.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
@Tag(name = "Horarios", description = "Gestión de horarios de médicos")
public class HorarioController {

    private final HorarioService horarioService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Asignar horario a médico",
               description = "RF-38: Solo ADMINISTRADOR. Sin traslapes.")
    public ResponseEntity<ApiResponse<HorarioResponse>> asignar(
            @Valid @RequestBody HorarioRequest request) {

        HorarioResponse response = horarioService.asignar(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Horario asignado correctamente", response));
    }

    @GetMapping("/medico/{medicoId}")
    @Operation(summary = "Listar horarios de un médico",
               description = "RF-12: Base para disponibilidad al registrar cita.")
    public ResponseEntity<ApiResponse<List<HorarioResponse>>> listarPorMedico(
            @PathVariable Long medicoId) {

        List<HorarioResponse> response = horarioService.listarPorMedico(medicoId);
        return ResponseEntity.ok(
                ApiResponse.success("Horarios obtenidos correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Eliminar horario",
               description = "Solo ADMINISTRADOR.")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {

        horarioService.eliminar(id);
        return ResponseEntity.ok(
                ApiResponse.success("Horario eliminado correctamente"));
    }
}