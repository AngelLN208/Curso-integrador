package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.common.enums.TipoAccion;
import pe.edu.utp.clinica.dto.seguro.SeguroRequest;
import pe.edu.utp.clinica.dto.seguro.SeguroResponse;
import pe.edu.utp.clinica.model.AuditoriaCita;
import pe.edu.utp.clinica.model.SeguroMedico;
import pe.edu.utp.clinica.repository.AuditoriaCitaRepository;
import pe.edu.utp.clinica.repository.CitaMedicaRepository;
import pe.edu.utp.clinica.repository.SeguroMedicoRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador para portal del administrador.
 *
 * RF-41: Auditoría de citas.
 * RF-42: Consultar auditoría por cita.
 * RF-43: Filtrar reporte de auditoría.
 * RF-49: Gestionar seguros médicos.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Administración", description = "Portal del administrador")
public class AdminController {

    private final AuditoriaCitaRepository auditoriaRepository;
    private final CitaMedicaRepository citaRepository;
    private final SeguroMedicoRepository seguroRepository;

    // ─── Auditoría ────────────────────────────────────────────────────

    @GetMapping("/auditoria/cita/{citaId}")
    @Operation(summary = "Historial de auditoría por cita",
               description = "RF-42: Estado anterior, estado nuevo, usuario y fecha.")
    public ResponseEntity<ApiResponse<List<AuditoriaCita>>> auditoriaPorCita(
            @PathVariable Long citaId) {

        var cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cita no encontrada con ID: " + citaId));

        List<AuditoriaCita> auditoria = auditoriaRepository
                .findByCitaOrderByFechaAccionAsc(cita);

        return ResponseEntity.ok(
                ApiResponse.success("Auditoría obtenida correctamente", auditoria));
    }

    @GetMapping("/auditoria/filtrar")
    @Operation(summary = "Filtrar reporte de auditoría",
               description = "RF-43: Filtra por usuarioId y/o tipoAccion.")
    public ResponseEntity<ApiResponse<List<AuditoriaCita>>> filtrarAuditoria(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) TipoAccion tipoAccion) {

        List<AuditoriaCita> auditoria = auditoriaRepository
                .filtrar(usuarioId, tipoAccion, null, null);

        return ResponseEntity.ok(
                ApiResponse.success("Reporte de auditoría generado", auditoria));
    }

    // ─── Seguros Médicos ──────────────────────────────────────────────

    @PostMapping("/seguros")
    @Operation(summary = "Registrar seguro médico",
               description = "RF-49: Nombre, tipo y porcentaje de cobertura.")
    public ResponseEntity<ApiResponse<SeguroResponse>> registrarSeguro(
            @Valid @RequestBody SeguroRequest request) {

        if (seguroRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new IllegalStateException(
                    "Ya existe un seguro con el nombre: " + request.getNombre());
        }

        SeguroMedico seguro = SeguroMedico.builder()
                .nombre(request.getNombre())
                .tipo(request.getTipo())
                .porcentajeCobertura(request.getPorcentajeCobertura())
                .deducible(request.getDeducible() != null
                        ? request.getDeducible()
                        : java.math.BigDecimal.ZERO)
                .convenioActivo(true)
                .build();

        seguro = seguroRepository.save(seguro);

        return ResponseEntity.status(201)
                .body(ApiResponse.created("Seguro registrado correctamente",
                        toSeguroResponse(seguro)));
    }

    @GetMapping("/seguros")
    @Operation(summary = "Listar seguros activos",
               description = "RF-49: Solo seguros con convenio activo.")
    public ResponseEntity<ApiResponse<List<SeguroResponse>>> listarSeguros() {

        List<SeguroResponse> seguros = seguroRepository
                .findByConvenioActivoTrue()
                .stream()
                .map(this::toSeguroResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("Seguros obtenidos correctamente", seguros));
    }

    @DeleteMapping("/seguros/{id}")
    @Operation(summary = "Desactivar seguro médico",
               description = "RF-49: Desactiva el convenio del seguro.")
    public ResponseEntity<ApiResponse<Void>> desactivarSeguro(@PathVariable Long id) {

        SeguroMedico seguro = seguroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seguro no encontrado con ID: " + id));

        seguro.setConvenioActivo(false);
        seguroRepository.save(seguro);

        return ResponseEntity.ok(
                ApiResponse.success("Seguro desactivado correctamente"));
    }

    // ─── Métodos internos ─────────────────────────────────────────────

    private SeguroResponse toSeguroResponse(SeguroMedico s) {
        return SeguroResponse.builder()
                .id(s.getId())
                .nombre(s.getNombre())
                .tipo(s.getTipo())
                .porcentajeCobertura(s.getPorcentajeCobertura())
                .deducible(s.getDeducible())
                .convenioActivo(s.isConvenioActivo())
                .build();
    }
}