package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.dto.especialidad.EspecialidadRequest;
import pe.edu.utp.clinica.dto.especialidad.EspecialidadResponse;
import pe.edu.utp.clinica.service.EspecialidadService;

import java.util.List;

/**
 * Controlador para gestión de especialidades médicas.
 *
 * RF-39: Registrar y modificar especialidades.
 *        Solo el ADMINISTRADOR puede crear, modificar, activar y desactivar.
 *        Todos los roles pueden listar las activas.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
@Tag(name = "Especialidades", description = "Gestión de especialidades médicas")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Registrar especialidad", description = "Solo ADMINISTRADOR. RF-39")
    public ResponseEntity<ApiResponse<EspecialidadResponse>> registrar(
            @Valid @RequestBody EspecialidadRequest request) {

        EspecialidadResponse response = especialidadService.registrar(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Especialidad registrada correctamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar especialidad", description = "Solo ADMINISTRADOR. RF-39")
    public ResponseEntity<ApiResponse<EspecialidadResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EspecialidadRequest request) {

        EspecialidadResponse response = especialidadService.actualizar(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Especialidad actualizada correctamente", response));
    }

    @GetMapping
    @Operation(summary = "Listar especialidades activas", description = "Todos los roles. RF-39")
    public ResponseEntity<ApiResponse<List<EspecialidadResponse>>> listar() {

        List<EspecialidadResponse> response = especialidadService.listarActivas();
        return ResponseEntity.ok(
                ApiResponse.success("Especialidades obtenidas correctamente", response));
    }

    @GetMapping("/todas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar todas las especialidades", description = "Solo ADMINISTRADOR. Incluye activas e inactivas, para el panel de gestión.")
    public ResponseEntity<ApiResponse<List<EspecialidadResponse>>> listarTodas() {

        List<EspecialidadResponse> response = especialidadService.listarTodas();
        return ResponseEntity.ok(
                ApiResponse.success("Especialidades obtenidas correctamente", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener especialidad por ID")
    public ResponseEntity<ApiResponse<EspecialidadResponse>> obtenerPorId(
            @PathVariable Long id) {

        EspecialidadResponse response = especialidadService.obtenerPorId(id);
        return ResponseEntity.ok(
                ApiResponse.success("Especialidad obtenida correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Desactivar especialidad", description = "Solo ADMINISTRADOR. RF-39")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Long id) {

        especialidadService.desactivar(id);
        return ResponseEntity.ok(
                ApiResponse.success("Especialidad desactivada correctamente"));
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Reactivar especialidad", description = "Solo ADMINISTRADOR.")
    public ResponseEntity<ApiResponse<Void>> activar(@PathVariable Long id) {

        especialidadService.activar(id);
        return ResponseEntity.ok(
                ApiResponse.success("Especialidad activada correctamente"));
    }
}