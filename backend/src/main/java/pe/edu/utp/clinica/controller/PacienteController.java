package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.dto.paciente.PacienteRequest;
import pe.edu.utp.clinica.dto.paciente.PacienteResponse;
import pe.edu.utp.clinica.service.PacienteService;

import java.util.List;

/**
 * Controlador para gestión de pacientes.
 *
 * RF-01: Registrar paciente.
 * RF-02: Actualizar paciente.
 * RF-03: Buscar paciente por DNI, nombre o apellido.
 * RF-04: Listar todos los pacientes.
 * Actor principal: RECEPCIONISTA.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Gestión de pacientes de la clínica")
@PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMINISTRADOR')")
public class PacienteController {

    private final PacienteService pacienteService;

    @PostMapping
    @Operation(summary = "Registrar paciente",
               description = "RF-01: DNI único de 8 dígitos, campos obligatorios.")
    public ResponseEntity<ApiResponse<PacienteResponse>> registrar(
            @Valid @RequestBody PacienteRequest request) {

        PacienteResponse response = pacienteService.registrar(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Paciente registrado correctamente", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar paciente",
               description = "RF-02: No se puede modificar el DNI.")
    public ResponseEntity<ApiResponse<PacienteResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PacienteRequest request) {

        PacienteResponse response = pacienteService.actualizar(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Paciente actualizado correctamente", response));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar paciente",
               description = "RF-03: Busca por DNI, nombre o apellido.")
    public ResponseEntity<ApiResponse<List<PacienteResponse>>> buscar(
            @RequestParam String criterio) {

        List<PacienteResponse> response = pacienteService.buscar(criterio);
        return ResponseEntity.ok(
                ApiResponse.success("Búsqueda completada", response));
    }

    @GetMapping
    @Operation(summary = "Listar todos los pacientes",
               description = "RF-04: Lista completa de pacientes.")
    public ResponseEntity<ApiResponse<List<PacienteResponse>>> listar() {

        List<PacienteResponse> response = pacienteService.listarTodos();
        return ResponseEntity.ok(
                ApiResponse.success("Pacientes obtenidos correctamente", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener paciente por ID")
    public ResponseEntity<ApiResponse<PacienteResponse>> obtenerPorId(
            @PathVariable Long id) {

        PacienteResponse response = pacienteService.obtenerPorId(id);
        return ResponseEntity.ok(
                ApiResponse.success("Paciente obtenido correctamente", response));
    }
}