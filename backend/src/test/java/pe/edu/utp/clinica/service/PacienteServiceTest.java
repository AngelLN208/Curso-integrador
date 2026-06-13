package pe.edu.utp.clinica.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.utp.clinica.dto.paciente.PacienteRequest;
import pe.edu.utp.clinica.dto.paciente.PacienteResponse;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.repository.PacienteRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PacienteService.
 *
 * RF-01: Registrar paciente — DNI único.
 * RF-02: Actualizar paciente — sin modificar DNI.
 * RF-03: Buscar paciente por criterio.
 * RF-04: Listar todos los pacientes.
 *
 * Patrón TDD: Arrange → Act → Assert
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests - PacienteService")
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteService pacienteService;

    private Paciente paciente;
    private PacienteRequest request;

    @BeforeEach
    void setUp() {
        paciente = Paciente.builder()
                .id(1L)
                .dni("12345678")
                .nombres("Juan")
                .apellidos("Pérez")
                .fechaNacimiento(LocalDate.of(1990, 1, 15))
                .celular("987654321")
                .correo("juan@email.com")
                .sexo("M")
                .build();

        request = new PacienteRequest();
        request.setDni("12345678");
        request.setNombres("Juan");
        request.setApellidos("Pérez");
        request.setFechaNacimiento(LocalDate.of(1990, 1, 15));
        request.setCelular("987654321");
        request.setCorreo("juan@email.com");
        request.setSexo("M");
    }

    // ─── RF-01: Registrar paciente ────────────────────────────────────

    @Test
    @DisplayName("RF-01: Debe registrar paciente correctamente")
    void debeRegistrarPacienteCorrectamente() {
        // Arrange
        when(pacienteRepository.existsByDni("12345678")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);

        // Act
        PacienteResponse response = pacienteService.registrar(request);

        // Assert
        assertNotNull(response);
        assertEquals("12345678", response.getDni());
        assertEquals("Juan", response.getNombres());
        verify(pacienteRepository, times(1)).save(any(Paciente.class));
    }

    @Test
    @DisplayName("RF-01: Debe lanzar excepción si DNI ya existe")
    void debeLanzarExcepcionSiDniDuplicado() {
        // Arrange
        when(pacienteRepository.existsByDni("12345678")).thenReturn(true);

        // Act & Assert
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> pacienteService.registrar(request)
        );

        assertTrue(ex.getMessage().contains("12345678"));
        verify(pacienteRepository, never()).save(any());
    }

    // ─── RF-02: Actualizar paciente ───────────────────────────────────

    @Test
    @DisplayName("RF-02: Debe actualizar paciente sin modificar DNI")
    void debeActualizarPacienteSinModificarDni() {
        // Arrange
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);

        request.setNombres("Juan Carlos");

        // Act
        PacienteResponse response = pacienteService.actualizar(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals("12345678", response.getDni()); // DNI no cambia
        verify(pacienteRepository, times(1)).save(any(Paciente.class));
    }

    @Test
    @DisplayName("RF-02: Debe lanzar excepción si paciente no existe")
    void debeLanzarExcepcionSiPacienteNoExiste() {
        // Arrange
        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> pacienteService.actualizar(99L, request)
        );
    }

    // ─── RF-03: Buscar paciente ───────────────────────────────────────

    @Test
    @DisplayName("RF-03: Debe buscar pacientes por criterio")
    void debeBuscarPacientesPorCriterio() {
        // Arrange
        when(pacienteRepository.buscarPorCriterio("Juan"))
                .thenReturn(List.of(paciente));

        // Act
        List<PacienteResponse> resultados = pacienteService.buscar("Juan");

        // Assert
        assertFalse(resultados.isEmpty());
        assertEquals(1, resultados.size());
        assertEquals("Juan", resultados.get(0).getNombres());
    }

    @Test
    @DisplayName("RF-03: Debe retornar lista vacía si no hay coincidencias")
    void debeRetornarListaVaciaSiNoHayCoincidencias() {
        // Arrange
        when(pacienteRepository.buscarPorCriterio("XYZ"))
                .thenReturn(List.of());

        // Act
        List<PacienteResponse> resultados = pacienteService.buscar("XYZ");

        // Assert
        assertTrue(resultados.isEmpty());
    }

    // ─── RF-04: Listar pacientes ──────────────────────────────────────

    @Test
    @DisplayName("RF-04: Debe listar todos los pacientes")
    void debeListarTodosLosPacientes() {
        // Arrange
        when(pacienteRepository.findAll()).thenReturn(List.of(paciente));

        // Act
        List<PacienteResponse> lista = pacienteService.listarTodos();

        // Assert
        assertNotNull(lista);
        assertEquals(1, lista.size());
    }
}