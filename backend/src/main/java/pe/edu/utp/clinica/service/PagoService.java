package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.EstadoPago;
import pe.edu.utp.clinica.dto.pago.PagoRequest;
import pe.edu.utp.clinica.dto.pago.PagoResponse;
import pe.edu.utp.clinica.model.*;
import pe.edu.utp.clinica.repository.*;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de pagos y comprobantes.
 *
 * RF-14: Registrar pago con monto, fecha y método.
 * RF-15: Confirmar cita al validar pago.
 * RF-17: Actualizar estado del pago a PAGADO.
 * RF-18: Generar comprobante automáticamente.
 * RF-19: Restringir pago en cita cancelada.
 * RNF-08: Comprobante disponible antes de respuesta HTTP.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final CitaService citaService;

    /**
     * Registra el pago de una cita y genera el comprobante.
     * RF-14, RF-15, RF-17, RF-18, RF-19.
     *
     * @param request datos del pago
     * @param username usuario que realiza el cobro
     * @return pago registrado
     */
    @Transactional
    public PagoResponse registrarPago(PagoRequest request, String username) {
        CitaMedica cita = citaService.buscarEntidadPorId(request.getCitaId());

        // RF-19: No se puede pagar una cita cancelada
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new IllegalStateException(
                    "No se puede registrar el pago de una cita cancelada.");
        }

        Pago pago = pagoRepository.findByCita(cita)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el pago asociado a la cita ID: "
                        + request.getCitaId()));

        if (pago.getEstado() == EstadoPago.PAGADO) {
            throw new IllegalStateException("Esta cita ya tiene un pago registrado.");
        }

        // RF-17: Actualizar estado del pago a PAGADO
        pago.setMonto(request.getMonto());
        pago.setMontoFinal(request.getMonto());
        pago.setMetodoPago(request.getMetodoPago());
        pago.setFechaPago(LocalDateTime.now());
        pago.setEstado(EstadoPago.PAGADO);
        pago = pagoRepository.save(pago);

        // RF-15: Confirmar cita al validar pago
        citaService.confirmar(cita.getId(), username);

        // RF-18: Generar comprobante automáticamente (RNF-08)
        generarComprobante(pago);

        log.debug("Pago registrado para cita ID: {}", cita.getId());
        return toResponse(pago);
    }

    /**
     * Lista los pagos de un paciente.
     * RF-35: Muestra cita asociada, monto y estado.
     *
     * @param pacienteId ID del paciente
     */
    @Transactional(readOnly = true)
    public List<PagoResponse> listarPorPaciente(Long pacienteId) {
        // Usamos findAll y filtramos por paciente
        return pagoRepository.findAll()
                .stream()
                .filter(p -> p.getCita().getPaciente().getId().equals(pacienteId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Métodos internos ─────────────────────────────────────────────

    /**
     * Genera el comprobante de pago con número único.
     * RF-18: Número formato COMP-{año}-{id con ceros}.
     * RNF-08: Se genera antes de retornar la respuesta HTTP.
     */
    private void generarComprobante(Pago pago) {
        long total = comprobanteRepository.count() + 1;
        String numero = String.format("COMP-%d-%06d", Year.now().getValue(), total);

        Comprobante comprobante = Comprobante.builder()
                .numero(numero)
                .pago(pago)
                .tipo("BOLETA")
                .enviado(false)
                .build();

        comprobanteRepository.save(comprobante);
        log.debug("Comprobante generado: {}", numero);
    }

    private PagoResponse toResponse(Pago p) {
        return PagoResponse.builder()
                .id(p.getId())
                .citaId(p.getCita().getId())
                .pacienteNombre(p.getCita().getPaciente().getNombres()
                        + " " + p.getCita().getPaciente().getApellidos())
                .monto(p.getMonto())
                .montoFinal(p.getMontoFinal())
                .metodoPago(p.getMetodoPago())
                .fechaPago(p.getFechaPago())
                .estado(p.getEstado())
                .creadoEn(p.getCreadoEn())
                .build();
    }
}