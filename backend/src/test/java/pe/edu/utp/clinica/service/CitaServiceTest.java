package pe.edu.utp.clinica.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.RolUsuario;
import pe.edu.utp.clinica.dto.cita.CitaRequest;
import pe.edu.utp.clinica.dto.cita.CitaReprogramarRequest;
import pe.edu.utp.clinica.dto.cita.CitaResponse;
import pe.edu.utp.clinica.model.*;
import pe.edu.utp.clinica.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CitaService.
 *
 * RF-05: Registrar cita en estado PENDIENTE.
 * RF-06: Reprogramar cita — estado REPROGRAMADA.
 * RF-09: Cancelar cita — estado CANCELADA.
 * RF-10: Validar disponibilidad del médico.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests - CitaService")
class CitaServiceTest {

    @Mock private CitaMedicaRepository citaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private AuditoriaCitaRepository auditoriaRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PacienteService pacienteService;
    @Mock private MedicoService medicoService;

    @InjectMocks
    private CitaService citaService;

    private Paciente paciente;
    private Medico medico;
    private Especialidad especialidad;
    private Usuario usuario;
    private CitaMedica cita;
    private CitaRequest citaRequest;

    @BeforeEach
    void setUp() {
        especialidad = Especialidad.builder()
                .id(1L).nombre("Medicina General").build();

        paciente = Paciente.builder()
                .id(1L).dni("12345678")
                .nombres("Juan").apellidos("Pérez")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .celular("987654321").sexo("M").build();

        medico = Medico.builder()
                .id(1L).dni("87654321")
                .nombres("Dr. Carlos").apellidos("López")
                .especialidad(especialidad).activo(true).build();

        usuario = Usuario.builder()
                .id(1L).username("admin@clinica.pe")
                .nombreCompleto("Administrador")
                .rol(RolUsuario.ROLE_ADMINISTRADOR).build();

        cita = CitaMedica.builder()
                .id(1L).paciente(paciente).medico(medico)
                .fechaHora(LocalDateTime.now().plusDays(1))
                .estado(EstadoCita.PENDIENTE)
                .registradoPor(usuario).build();

        citaRequest = new CitaRequest();
        citaRequest.setPacienteId(1L);
        citaRequest.setMedicoId(1L);
        citaRequest.setFechaHora(LocalDateTime.now().plusDays(1));
    }

    // ─── RF-05: Registrar cita ────────────────────────────────────────

    @Test
    @DisplayName("RF-05: Debe registrar cita en estado PENDIENTE")
    void debeRegistrarCitaEnEstadoPendiente() {
        // Arrange
        when(pacienteService.buscarEntidadPorId(1L)).thenReturn(paciente);
        when(medicoService.buscarEntidadPorId(1L)).thenReturn(medico);
        when(usuarioRepository.findByUsername("admin@clinica.pe"))
                .thenReturn(Optional.of(usuario));
        when(citaRepository.existeConflictoHorario(any(), any())).thenReturn(false);
        when(citaRepository.save(any(CitaMedica.class))).thenReturn(cita);
        when(pagoRepository.save(any())).thenReturn(null);
        when(auditoriaRepository.save(any())).thenReturn(null);
        when(notificacionRepository.save(any())).thenReturn(null);

        // Act
        CitaResponse response = citaService.registrar(citaRequest, "admin@clinica.pe");

        // Assert
        assertNotNull(response);
        assertEquals(EstadoCita.PENDIENTE, response.getEstado());
        verify(citaRepository, times(1)).save(any(CitaMedica.class));
        verify(pagoRepository, times(1)).save(any()); // RF-11
        verify(auditoriaRepository, times(1)).save(any()); // RF-41
    }

    @Test
    @DisplayName("RF-10: Debe lanzar excepción si médico no tiene disponibilidad")
    void debeLanzarExcepcionSiMedicoNoDisponible() {
        // Arrange
        when(pacienteService.buscarEntidadPorId(1L)).thenReturn(paciente);
        when(medicoService.buscarEntidadPorId(1L)).thenReturn(medico);
        when(usuarioRepository.findByUsername("admin@clinica.pe"))
                .thenReturn(Optional.of(usuario));
        when(citaRepository.existeConflictoHorario(any(), any())).thenReturn(true);

        // Act & Assert
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> citaService.registrar(citaRequest, "admin@clinica.pe")
        );

        assertTrue(ex.getMessage().contains("disponibilidad"));
        verify(citaRepository, never()).save(any());
    }

    // ─── RF-09: Cancelar cita ─────────────────────────────────────────

    @Test
    @DisplayName("RF-09: Debe cancelar cita cambiando estado a CANCELADA")
    void debeCancelarCita() {
        // Arrange
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByUsername("admin@clinica.pe"))
                .thenReturn(Optional.of(usuario));
        when(citaRepository.save(any(CitaMedica.class))).thenReturn(cita);
        when(auditoriaRepository.save(any())).thenReturn(null);
        when(notificacionRepository.save(any())).thenReturn(null);

        // Act
        CitaResponse response = citaService.cancelar(1L, "admin@clinica.pe");

        // Assert
        assertNotNull(response);
        verify(citaRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("RF-09: Debe lanzar excepción si cita ya está cancelada")
    void debeLanzarExcepcionSiCitaYaCancelada() {
        // Arrange
        cita.setEstado(EstadoCita.CANCELADA);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByUsername("admin@clinica.pe"))
                .thenReturn(Optional.of(usuario));

        // Act & Assert
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> citaService.cancelar(1L, "admin@clinica.pe")
        );

        assertTrue(ex.getMessage().contains("cancelada"));
    }

    // ─── RF-06: Reprogramar cita ──────────────────────────────────────

    @Test
    @DisplayName("RF-06: Debe reprogramar cita cambiando estado a REPROGRAMADA")
    void debeReprogramarCita() {
        // Arrange
        CitaReprogramarRequest reprogramarRequest = new CitaReprogramarRequest();
        reprogramarRequest.setNuevaFechaHora(LocalDateTime.now().plusDays(3));

        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByUsername("admin@clinica.pe"))
                .thenReturn(Optional.of(usuario));
        when(citaRepository.existeConflictoHorario(any(), any())).thenReturn(false);
        when(citaRepository.save(any())).thenReturn(cita);
        when(auditoriaRepository.save(any())).thenReturn(null);
        when(notificacionRepository.save(any())).thenReturn(null);

        // Act
        CitaResponse response = citaService.reprogramar(
                1L, reprogramarRequest, "admin@clinica.pe");

        // Assert
        assertNotNull(response);
        verify(citaRepository, times(1)).save(any());
        verify(auditoriaRepository, times(1)).save(any()); // RF-41
    }

    @Test
    @DisplayName("RF-06: Debe lanzar excepción si se reprograma cita cancelada")
    void debeLanzarExcepcionAlReprogramarCitaCancelada() {
        // Arrange
        cita.setEstado(EstadoCita.CANCELADA);
        CitaReprogramarRequest reprogramarRequest = new CitaReprogramarRequest();
        reprogramarRequest.setNuevaFechaHora(LocalDateTime.now().plusDays(3));

        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByUsername("admin@clinica.pe"))
                .thenReturn(Optional.of(usuario));

        // Act & Assert
        assertThrows(
                IllegalStateException.class,
                () -> citaService.reprogramar(1L, reprogramarRequest, "admin@clinica.pe")
        );
    }
}