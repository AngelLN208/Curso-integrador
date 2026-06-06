package pe.edu.utp.clinica.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import pe.edu.utp.clinica.common.ApiResponse;
import pe.edu.utp.clinica.dto.atencion.ConsultaRequest;
import pe.edu.utp.clinica.dto.atencion.ConsultaResponse;
import pe.edu.utp.clinica.dto.atencion.TriajeRequest;
import pe.edu.utp.clinica.model.Triaje;
import pe.edu.utp.clinica.service.AtencionService;

import java.util.List;

/**
 * Controlador para portal del médico.
 *
 * RF-22: Registrar triaje.
 * RF-23: Registrar consulta médica.
 * RF-26: Consultar historial médico del paciente.
 * Actor principal: MEDICO.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/atencion")
@RequiredArgsConstructor
@Tag(name = "Atención Médica", description = "Portal del médico - triaje y consultas")
@PreAuthorize("hasAnyRole('MEDICO', 'ADMINISTRADOR')")
public class AtencionController {

    private final AtencionService atencionService;

    @PostMapping("/triaje")
    @Operation(summary = "Registrar triaje",
               description = "RF-22: Presión, temperatura y peso. "
                           + "RF-21: Solo citas CONFIRMADAS.")
    public ResponseEntity<ApiResponse<Triaje>> registrarTriaje(
            @Valid @RequestBody TriajeRequest request) {

        Triaje response = atencionService.registrarTriaje(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Triaje registrado correctamente", response));
    }

    @PostMapping("/consulta")
    @Operation(summary = "Registrar consulta médica",
               description = "RF-23: Diagnóstico y tratamiento. "
                           + "RF-25: Solo una consulta por cita.")
    public ResponseEntity<ApiResponse<ConsultaResponse>> registrarConsulta(
            @Valid @RequestBody ConsultaRequest request) {

        ConsultaResponse response = atencionService.registrarConsulta(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Consulta registrada correctamente", response));
    }

    @GetMapping("/historial/{pacienteId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMINISTRADOR', 'RECEPCIONISTA')")
    @Operation(summary = "Historial médico del paciente",
               description = "RF-26 y RF-27: Lista consultas ordenadas "
                           + "por fecha descendente.")
    public ResponseEntity<ApiResponse<List<ConsultaResponse>>> historial(
            @PathVariable Long pacienteId) {

        List<ConsultaResponse> response = atencionService.obtenerHistorial(pacienteId);
        return ResponseEntity.ok(
                ApiResponse.success("Historial obtenido correctamente", response));
    }
}