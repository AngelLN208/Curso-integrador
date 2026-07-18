package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.dto.pago.PagoRequest;
import pe.edu.utp.clinica.dto.pago.PagoResponse;
import pe.edu.utp.clinica.dto.portal.PagoPortalRequest;
import pe.edu.utp.clinica.model.CitaMedica;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.repository.CitaMedicaRepository;
import pe.edu.utp.clinica.repository.PacienteRepository;

import java.math.BigDecimal;

/**
 * Servicio para el pago de citas desde el portal del paciente.
 *
 * RF-56: El paciente paga su cita directamente desde el portal.
 * Valida ownership (la cita debe pertenecer al paciente).
 * Aplica algoritmo de Luhn para pagos con tarjeta.
 * Delega el procesamiento real al PagoService existente.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalPagoService {

        private final PacienteRepository pacienteRepository;
        private final CitaMedicaRepository citaRepository;
        private final PagoService pagoService;

        /**
         * Procesa el pago de una cita desde el portal del paciente.
         *
         * RF-56: Validaciones en orden:
         * 1. El paciente autenticado existe
         * 2. La cita existe y pertenece al paciente (ownership)
         * 3. La cita está en estado PENDIENTE o CONFIRMADA (no CANCELADA)
         * 4. Si método = TARJETA: validar algoritmo de Luhn
         * 5. Delegar al PagoService para procesar y confirmar la cita
         *
         * @param request  datos del pago (citaId, método, datos de tarjeta)
         * @param username correo del paciente autenticado
         * @return PagoResponse con comprobante generado
         */
        @Transactional
        public PagoResponse pagarDesdePortal(PagoPortalRequest request,
                        String username) {
                // Buscar paciente autenticado
                Paciente paciente = pacienteRepository.findByCorreo(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Paciente no encontrado: " + username));

                // Buscar la cita
                CitaMedica cita = citaRepository.findById(request.getCitaId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Cita no encontrada con ID: " + request.getCitaId()));

                // RF-56: Validar ownership — la cita debe pertenecer al paciente
                if (!cita.getPaciente().getId().equals(paciente.getId())) {
                        throw new IllegalStateException(
                                        "No tienes permiso para pagar esta cita.");
                }

                // La cita no debe estar cancelada
                if (cita.getEstado() == EstadoCita.CANCELADA) {
                        throw new IllegalStateException(
                                        "No se puede pagar una cita cancelada.");
                }

                // Validar tarjeta con algoritmo de Luhn si el método es TARJETA
                if ("TARJETA".equals(request.getMetodoPago())) {
                        if (request.getNumeroTarjeta() == null
                                        || request.getNumeroTarjeta().isBlank()) {
                                throw new IllegalArgumentException(
                                                "El número de tarjeta es obligatorio para pagos con tarjeta.");
                        }
                        if (!validarLuhn(request.getNumeroTarjeta())) {
                                throw new IllegalArgumentException(
                                                "El número de tarjeta no es válido. "
                                                                + "Por favor verifica los 16 dígitos e intenta de nuevo.");
                        }
                        if (request.getTitularTarjeta() == null
                                        || request.getTitularTarjeta().isBlank()) {
                                throw new IllegalArgumentException(
                                                "El nombre del titular es obligatorio para pagos con tarjeta.");
                        }
                }

                // Construir PagoRequest para delegar al PagoService existente
                // El monto base es el costo real de la especialidad del médico
                // (antes estaba fijo en 80.00 para todas las especialidades).
                PagoRequest pagoRequest = new PagoRequest();
                pagoRequest.setCitaId(request.getCitaId());
                pagoRequest.setMonto(cita.getMedico().getEspecialidad().getCosto());
                pagoRequest.setMetodoPago(request.getMetodoPago());
                log.info("Pago desde portal — paciente ID: {} | cita: {} | método: {}",
                                paciente.getId(), cita.getId(), request.getMetodoPago());

                return pagoService.registrarPago(pagoRequest, username);
        }

        /**
         * Valida un número de tarjeta usando el algoritmo de Luhn.
         * RF-56: Verificación estándar usada por todas las tarjetas de crédito/débito.
         *
         * Algoritmo:
         * 1. Desde el penúltimo dígito hacia la izquierda, duplicar cada segundo dígito
         * 2. Si el resultado > 9, restar 9
         * 3. Sumar todos los dígitos
         * 4. Si la suma es divisible por 10 → tarjeta válida
         *
         * @param numero 16 dígitos numéricos de la tarjeta
         * @return true si pasa la validación de Luhn
         */
        private boolean validarLuhn(String numero) {
                int suma = 0;
                boolean duplicar = false;

                for (int i = numero.length() - 1; i >= 0; i--) {
                        int digito = numero.charAt(i) - '0';

                        if (duplicar) {
                                digito *= 2;
                                if (digito > 9)
                                        digito -= 9;
                        }

                        suma += digito;
                        duplicar = !duplicar;
                }

                return suma % 10 == 0;
        }

        /**
         * Previsualiza el cálculo de pago (con descuento de seguro si aplica)
         * para una cita del paciente autenticado, antes de confirmar el cobro.
         * RF-16 (extendido): mismo cálculo que usa recepcionista, pero
         * validando que la cita pertenezca al paciente.
         *
         * @param citaId   ID de la cita a previsualizar
         * @param username correo del paciente autenticado
         * @return cálculo con monto bruto, descuento, monto final y seguro aplicado
         */
        @Transactional(readOnly = true)
        public pe.edu.utp.clinica.dto.pago.PrevisualizarPagoResponse previsualizarDesdePortal(
                        Long citaId, String username) {

                Paciente paciente = pacienteRepository.findByCorreo(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Paciente no encontrado: " + username));

                CitaMedica cita = citaRepository.findById(citaId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Cita no encontrada con ID: " + citaId));

                if (!cita.getPaciente().getId().equals(paciente.getId())) {
                        throw new IllegalStateException(
                                        "No tienes permiso para ver el pago de esta cita.");
                }

                return pagoService.previsualizarPago(citaId);
        }

        /**
         * Obtiene los datos del pago de una cita para mostrar el comprobante.
         * RF-18 (extendido): el paciente ve su boleta desde el portal,
         * igual que recepcionista, validando ownership.
         *
         * @param citaId   ID de la cita
         * @param username correo del paciente autenticado
         * @return datos completos del pago (monto, descuento, método, fecha)
         */
        @Transactional(readOnly = true)
        public pe.edu.utp.clinica.dto.pago.PagoResponse obtenerComprobanteDesdePortal(
                        Long citaId, String username) {

                Paciente paciente = pacienteRepository.findByCorreo(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Paciente no encontrado: " + username));

                CitaMedica cita = citaRepository.findById(citaId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Cita no encontrada con ID: " + citaId));

                if (!cita.getPaciente().getId().equals(paciente.getId())) {
                        throw new IllegalStateException(
                                        "No tienes permiso para ver el comprobante de esta cita.");
                }

                return pagoService.obtenerPorCita(citaId);
        }

}