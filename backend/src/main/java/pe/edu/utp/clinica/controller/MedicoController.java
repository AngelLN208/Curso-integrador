package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.dto.medico.MedicoRequest;
import pe.edu.utp.clinica.dto.medico.MedicoResponse;
import pe.edu.utp.clinica.service.MedicoService;

import java.util.List;

/**
 * Controlador para gestión de médicos.
 *
 * RF-37: Registrar médico. Solo ADMINISTRADOR.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
@Tag(name = "Médicos", description = "Gestión de médicos de la clínica")
public class MedicoController {

    private final MedicoService medicoService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Registrar médico",
               description = "RF-37: Solo ADMINISTRADOR. DNI único.")
    public ResponseEntity<ApiResponse<MedicoResponse>> registrar(
            @Valid @RequestBody MedicoRequest request) {

        MedicoResponse response = medicoService.registrar(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Médico registrado correctamente", response));
    }

    @GetMapping
    @Operation(summary = "Listar médicos activos",
               description = "Todos los roles pueden consultar.")
    public ResponseEntity<ApiResponse<List<MedicoResponse>>> listar() {

        List<MedicoResponse> response = medicoService.listarActivos();
        return ResponseEntity.ok(
                ApiResponse.success("Médicos obtenidos correctamente", response));
    }

    @GetMapping("/especialidad/{especialidadId}")
    @Operation(summary = "Listar médicos por especialidad",
               description = "Filtra médicos activos por especialidad.")
    public ResponseEntity<ApiResponse<List<MedicoResponse>>> listarPorEspecialidad(
            @PathVariable Long especialidadId) {

        List<MedicoResponse> response = medicoService.listarPorEspecialidad(especialidadId);
        return ResponseEntity.ok(
                ApiResponse.success("Médicos obtenidos correctamente", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener médico por ID")
    public ResponseEntity<ApiResponse<MedicoResponse>> obtenerPorId(
            @PathVariable Long id) {

        MedicoResponse response = medicoService.obtenerPorId(id);
        return ResponseEntity.ok(
                ApiResponse.success("Médico obtenido correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Desactivar médico",
               description = "Solo ADMINISTRADOR.")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Long id) {

        medicoService.desactivar(id);
        return ResponseEntity.ok(
                ApiResponse.success("Médico desactivado correctamente"));
    }
}