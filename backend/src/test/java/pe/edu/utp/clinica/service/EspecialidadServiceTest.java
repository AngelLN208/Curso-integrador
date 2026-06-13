package pe.edu.utp.clinica.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.edu.utp.clinica.dto.especialidad.EspecialidadRequest;
import pe.edu.utp.clinica.dto.especialidad.EspecialidadResponse;
import pe.edu.utp.clinica.model.Especialidad;
import pe.edu.utp.clinica.repository.EspecialidadRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para EspecialidadService.
 *
 * RF-39: Registrar y modificar especialidades.
 *        No se permiten nombres duplicados.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests - EspecialidadService")
class EspecialidadServiceTest {

    @Mock
    private EspecialidadRepository especialidadRepository;

    @InjectMocks
    private EspecialidadService especialidadService;

    private Especialidad especialidad;
    private EspecialidadRequest request;

    @BeforeEach
    void setUp() {
        especialidad = Especialidad.builder()
                .id(1L)
                .nombre("Cardiología")
                .descripcion("Enfermedades del corazón")
                .activo(true)
                .build();

        request = new EspecialidadRequest();
        request.setNombre("Cardiología");
        request.setDescripcion("Enfermedades del corazón");
    }

    @Test
    @DisplayName("RF-39: Debe registrar especialidad correctamente")
    void debeRegistrarEspecialidadCorrectamente() {
        // Arrange
        when(especialidadRepository.existsByNombreIgnoreCase("Cardiología"))
                .thenReturn(false);
        when(especialidadRepository.save(any())).thenReturn(especialidad);

        // Act
        EspecialidadResponse response = especialidadService.registrar(request);

        // Assert
        assertNotNull(response);
        assertEquals("Cardiología", response.getNombre());
        verify(especialidadRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("RF-39: Debe lanzar excepción si nombre duplicado")
    void debeLanzarExcepcionSiNombreDuplicado() {
        // Arrange
        when(especialidadRepository.existsByNombreIgnoreCase("Cardiología"))
                .thenReturn(true);

        // Act & Assert
        assertThrows(
                IllegalStateException.class,
                () -> especialidadService.registrar(request)
        );
        verify(especialidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF-39: Debe listar solo especialidades activas")
    void debeListarSoloEspecialidadesActivas() {
        // Arrange
        when(especialidadRepository.findByActivoTrue())
                .thenReturn(List.of(especialidad));

        // Act
        List<EspecialidadResponse> lista = especialidadService.listarActivas();

        // Assert
        assertFalse(lista.isEmpty());
        assertEquals(1, lista.size());
        assertTrue(lista.get(0).isActivo());
    }

    @Test
    @DisplayName("RF-39: Debe desactivar especialidad")
    void debeDesactivarEspecialidad() {
        // Arrange
        when(especialidadRepository.findById(1L))
                .thenReturn(Optional.of(especialidad));
        when(especialidadRepository.save(any())).thenReturn(especialidad);

        // Act
        especialidadService.desactivar(1L);

        // Assert
        verify(especialidadRepository, times(1)).save(any());
        assertFalse(especialidad.isActivo());
    }
}