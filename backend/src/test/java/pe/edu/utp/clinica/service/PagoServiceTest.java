package pe.edu.utp.clinica.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.EstadoPago;
import pe.edu.utp.clinica.dto.pago.PagoRequest;
import pe.edu.utp.clinica.dto.pago.PagoResponse;
import pe.edu.utp.clinica.model.*;
import pe.edu.utp.clinica.repository.ComprobanteRepository;
import pe.edu.utp.clinica.repository.NotificacionRepository;
import pe.edu.utp.clinica.repository.PacienteSeguroRepository;
import pe.edu.utp.clinica.repository.PagoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PagoService.
 *
 * RF-14: Registrar pago con monto, fecha y método.
 * RF-15: Confirmar cita al validar pago.
 * RF-16: Aplicar cobertura del seguro del paciente al monto final.
 * RF-17: Actualizar estado del pago a PAGADO.
 * RF-19: Restringir pago en cita cancelada.
 *
 * Patrón TDD: Arrange → Act → Assert
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests - PagoService")
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private ComprobanteRepository comprobanteRepository;

    @Mock
    private PacienteSeguroRepository pacienteSeguroRepository;

    @Mock
    private CitaService citaService;

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private PagoService pagoService;

    private CitaMedica cita;
    private Paciente paciente;
    private Medico medico;
    private Pago pago;
    private PagoRequest request;

    @BeforeEach
    void setUp() {
        paciente = Paciente.builder()
                .id(1L)
                .dni("12345678")
                .nombres("Juan")
                .apellidos("Pérez")
                .build();

        Especialidad especialidad = Especialidad.builder()
                .id(1L)
                .nombre("Medicina General")
                .build();

        medico = Medico.builder()
                .id(1L)
                .nombres("Carlos")
                .apellidos("López")
                .especialidad(especialidad)
                .build();

        cita = CitaMedica.builder()
                .id(1L)
                .paciente(paciente)
                .medico(medico)
                .fechaHora(LocalDateTime.now().plusDays(1))
                .estado(EstadoCita.PENDIENTE)
                .build();

        pago = Pago.builder()
                .id(1L)
                .cita(cita)
                .monto(new BigDecimal("80.00"))
                .montoFinal(new BigDecimal("80.00"))
                .estado(EstadoPago.PENDIENTE)
                .build();

        request = new PagoRequest();
        request.setCitaId(1L);
        request.setMonto(new BigDecimal("80.00"));
    }

    // ─── RF-16: Cálculo con seguro ─────────────────────────────────────
    @Test
    @DisplayName("RF-16: Debe aplicar descuento de seguro correctamente")
    void debeAplicarDescuentoDeSeguroCorrectamente() {
        // Arrange
        SeguroMedico seguro = SeguroMedico.builder()
                .id(1L)
                .nombre("Rimac Salud")
                .porcentajeCobertura(new BigDecimal("30"))
                .build();

        PacienteSeguro pacienteSeguro = PacienteSeguro.builder()
                .paciente(paciente)
                .seguro(seguro)
                .activo(true)
                .build();

        when(citaService.buscarEntidadPorId(1L)).thenReturn(cita);
        when(pagoRepository.findByCita(cita)).thenReturn(Optional.of(pago));
        when(pacienteSeguroRepository.findFirstByPacienteAndActivoTrue(paciente))
                .thenReturn(Optional.of(pacienteSeguro));
        when(pagoRepository.save(any(Pago.class))).thenReturn(pago);
        when(citaService.confirmar(eq(1L), any())).thenReturn(null);

        // Act
        PagoResponse response = pagoService.registrarPago(request, "recepcion@clinica.pe");

        // Assert — 80.00 con 30% de cobertura = 24.00 de descuento → 56.00 final
        assertNotNull(response);
        assertEquals(new BigDecimal("56.00"), pago.getMontoFinal());
        verify(comprobanteRepository, times(1)).save(any(Comprobante.class));
        verify(notificacionRepository, times(1)).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("RF-16: Debe cobrar el monto completo si el paciente no tiene seguro activo")
    void debeCobrarMontoCompletoSinSeguro() {
        // Arrange
        when(citaService.buscarEntidadPorId(1L)).thenReturn(cita);
        when(pagoRepository.findByCita(cita)).thenReturn(Optional.of(pago));
        when(pacienteSeguroRepository.findFirstByPacienteAndActivoTrue(paciente))
                .thenReturn(Optional.empty());
        when(pagoRepository.save(any(Pago.class))).thenReturn(pago);
        when(citaService.confirmar(eq(1L), any())).thenReturn(null);

        // Act
        pagoService.registrarPago(request, "recepcion@clinica.pe");

        // Assert — sin seguro, el monto final debe ser igual al bruto
        assertEquals(new BigDecimal("80.00"), pago.getMontoFinal());
    }

    // ─── RF-19: Restricciones de negocio ───────────────────────────────

    @Test
    @DisplayName("RF-19: Debe rechazar el pago de una cita cancelada")
    void debeRechazarPagoDeCitaCancelada() {
        // Arrange
        cita.setEstado(EstadoCita.CANCELADA);
        when(citaService.buscarEntidadPorId(1L)).thenReturn(cita);

        // Act & Assert
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> pagoService.registrarPago(request, "recepcion@clinica.pe"));

        assertTrue(ex.getMessage().contains("cancelada"));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar un pago duplicado sobre una cita ya pagada")
    void debeRechazarPagoDuplicado() {
        // Arrange
        pago.setEstado(EstadoPago.PAGADO);
        when(citaService.buscarEntidadPorId(1L)).thenReturn(cita);
        when(pagoRepository.findByCita(cita)).thenReturn(Optional.of(pago));

        // Act & Assert
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> pagoService.registrarPago(request, "recepcion@clinica.pe"));

        assertTrue(ex.getMessage().contains("pago registrado"));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si no existe pago asociado a la cita")
    void debeLanzarExcepcionSiNoExistePagoAsociado() {
        // Arrange
        when(citaService.buscarEntidadPorId(1L)).thenReturn(cita);
        when(pagoRepository.findByCita(cita)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.registrarPago(request, "recepcion@clinica.pe"));
    }
}