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
import pe.edu.utp.clinica.dto.pago.PagoRequest;
import pe.edu.utp.clinica.dto.pago.PagoResponse;
import pe.edu.utp.clinica.service.PagoService;

import java.util.List;

/**
 * Controlador para gestión de pagos.
 *
 * RF-14: Registrar pago de cita.
 * RF-15: Confirma la cita al pagar.
 * RF-17: Estado del pago cambia a PAGADO.
 * RF-18: Genera comprobante automáticamente.
 * RF-19: Restringe pago en cita cancelada.
 * Actor principal: RECEPCIONISTA.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Gestión de pagos de citas médicas")
@PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMINISTRADOR')")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping
    @Operation(summary = "Registrar pago de cita",
               description = "RF-14: Registra pago. "
                           + "RF-15: Confirma la cita. "
                           + "RF-17: Estado PAGADO. "
                           + "RF-18: Genera comprobante. "
                           + "RF-19: No aplica en citas canceladas.")
    public ResponseEntity<ApiResponse<PagoResponse>> registrarPago(
            @Valid @RequestBody PagoRequest request,
            Authentication auth) {

        PagoResponse response = pagoService.registrarPago(request, auth.getName());
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Pago registrado correctamente", response));
    }

    @GetMapping("/paciente/{pacienteId}")
    @Operation(summary = "Listar pagos de un paciente",
               description = "RF-35: Muestra cita, monto y estado.")
    public ResponseEntity<ApiResponse<List<PagoResponse>>> listarPorPaciente(
            @PathVariable Long pacienteId) {

        List<PagoResponse> response = pagoService.listarPorPaciente(pacienteId);
        return ResponseEntity.ok(
                ApiResponse.success("Pagos obtenidos correctamente", response));
    }
}