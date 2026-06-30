package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.EstadoPago;
import pe.edu.utp.clinica.dto.portal.*;
import pe.edu.utp.clinica.model.*;
import pe.edu.utp.clinica.repository.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para el portal del paciente.
 *
 * RF-51: Dashboard personal con próxima cita, último diagnóstico y pagos.
 * RF-52: Directorio público de especialidades y médicos con horarios.
 * RF-54: Calificación post-consulta de médicos (1-5 estrellas).
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalService {

    private final EspecialidadRepository      especialidadRepository;
    private final MedicoRepository            medicoRepository;
    private final HorarioMedicoRepository     horarioRepository;
    private final PacienteRepository          pacienteRepository;
    private final CitaMedicaRepository        citaRepository;
    private final ConsultaMedicaRepository    consultaRepository;
    private final PagoRepository              pagoRepository;
    private final ValoracionRepository        valoracionRepository;

    // ─── RF-52: Directorio médico público ────────────────────────────────────

    /**
     * Lista todas las especialidades activas con sus médicos y horarios.
     * RF-52: Endpoint público — no requiere autenticación.
     *
     * @return lista de especialidades con médicos activos anidados
     */
    @Transactional(readOnly = true)
    public List<EspecialidadDirectorioResponse> obtenerDirectorio() {
        return especialidadRepository.findByActivoTrue()
                .stream()
                .map(this::toEspecialidadDirectorio)
                .collect(Collectors.toList());
    }

    /**
     * Lista médicos activos de una especialidad específica.
     * RF-52: Permite filtrar por especialidad en el directorio.
     *
     * @param especialidadId ID de la especialidad
     * @return lista de médicos de esa especialidad
     */
    @Transactional(readOnly = true)
    public List<MedicoDirectorioResponse> obtenerMedicosPorEspecialidad(Long especialidadId) {
        return medicoRepository.findByEspecialidadIdAndActivoTrue(especialidadId)
                .stream()
                .map(this::toMedicoDirectorio)
                .collect(Collectors.toList());
    }

    // ─── RF-51: Dashboard del paciente ───────────────────────────────────────

    /**
     * Construye el dashboard personal del paciente autenticado.
     * RF-51: Próxima cita, último diagnóstico, pago pendiente y totales.
     *
     * @param username correo del paciente autenticado (extraído del JWT)
     * @return DTO con todos los datos del dashboard
     */
    @Transactional(readOnly = true)
    public DashboardPacienteResponse obtenerDashboard(String username) {
        Paciente paciente = pacienteRepository.findByCorreo(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Paciente no encontrado: " + username));

        // Próxima cita confirmada o pendiente más cercana
        DashboardPacienteResponse.ProximaCitaDTO proximaCita =
                citaRepository.findProximaCitaPaciente(paciente.getId())
                        .map(c -> {
                            Pago pago = pagoRepository.findByCita(c).orElse(null);
                            return DashboardPacienteResponse.ProximaCitaDTO.builder()
                                    .citaId(c.getId())
                                    .fechaHora(c.getFechaHora())
                                    .medicoNombre(c.getMedico().getNombres()
                                            + " " + c.getMedico().getApellidos())
                                    .especialidad(c.getMedico().getEspecialidad().getNombre())
                                    .estado(c.getEstado())
                                    .pagoId(pago != null ? pago.getId() : null)
                                    .estadoPago(pago != null ? pago.getEstado() : null)
                                    .build();
                        })
                        .orElse(null);

        // Último diagnóstico registrado
        DashboardPacienteResponse.UltimoDiagnosticoDTO ultimoDiagnostico =
                consultaRepository
                        .findTopByCitaPacienteOrderByCitaFechaHoraDesc(paciente)
                        .map(c -> DashboardPacienteResponse.UltimoDiagnosticoDTO.builder()
                                .consultaId(c.getId())
                                .fechaCita(c.getCita().getFechaHora())
                                .medicoNombre(c.getCita().getMedico().getNombres()
                                        + " " + c.getCita().getMedico().getApellidos())
                                .especialidad(c.getCita().getMedico().getEspecialidad().getNombre())
                                .diagnostico(c.getDiagnostico())
                                .tratamiento(c.getTratamiento())
                                .build())
                        .orElse(null);

        // Pago pendiente más reciente
        DashboardPacienteResponse.PagoPendienteDTO pagoPendiente =
                pagoRepository.findFirstPendientePorPaciente(paciente.getId())
                        .map(p -> DashboardPacienteResponse.PagoPendienteDTO.builder()
                                .pagoId(p.getId())
                                .citaId(p.getCita().getId())
                                .monto(p.getMonto())
                                .montoFinal(p.getMontoFinal())
                                .metodoPago(p.getMetodoPago())
                                .build())
                        .orElse(null);

        // Contadores de citas por estado
        List<CitaMedica> todasLasCitas = citaRepository
                .findByCitasPacienteId(paciente.getId());

        log.debug("Dashboard cargado para paciente ID: {}", paciente.getId());

        return DashboardPacienteResponse.builder()
                .nombrePaciente(paciente.getNombres() + " " + paciente.getApellidos())
                .dni(paciente.getDni())
                .correo(paciente.getCorreo())
                .proximaCita(proximaCita)
                .ultimoDiagnostico(ultimoDiagnostico)
                .pagoPendiente(pagoPendiente)
                .totalCitas(todasLasCitas.size())
                .citasPendientes((int) todasLasCitas.stream()
                        .filter(c -> c.getEstado() == EstadoCita.PENDIENTE).count())
                .citasConfirmadas((int) todasLasCitas.stream()
                        .filter(c -> c.getEstado() == EstadoCita.CONFIRMADA).count())
                .citasAtendidas((int) todasLasCitas.stream()
                        .filter(c -> c.getEstado() == EstadoCita.ATENDIDA).count())
                .build();
    }

    // ─── RF-54: Valoraciones ──────────────────────────────────────────────────

    /**
     * Registra la calificación de un médico tras una consulta.
     * RF-54: Solo se puede valorar si la cita está ATENDIDA
     *        y el paciente aún no ha calificado esa cita.
     *
     * @param request datos de la valoración (citaId, puntuación, comentario)
     * @param username correo del paciente autenticado
     * @return valoración registrada
     */
    @Transactional
    public ValoracionResponse registrarValoracion(ValoracionRequest request,
                                                   String username) {
        Paciente paciente = pacienteRepository.findByCorreo(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Paciente no encontrado: " + username));

        CitaMedica cita = citaRepository.findById(request.getCitaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cita no encontrada: " + request.getCitaId()));

        // Verificar que la cita pertenece al paciente
        if (!cita.getPaciente().getId().equals(paciente.getId())) {
            throw new IllegalStateException(
                    "No puedes valorar una cita que no te pertenece.");
        }

        // RF-54: Solo citas ATENDIDAS pueden ser valoradas
        if (cita.getEstado() != EstadoCita.ATENDIDA) {
            throw new IllegalStateException(
                    "Solo puedes valorar citas con estado ATENDIDA. "
                    + "Estado actual: " + cita.getEstado());
        }

        // Evitar valoraciones duplicadas
        if (valoracionRepository.existsByCita(cita)) {
            throw new IllegalStateException(
                    "Ya has calificado esta consulta.");
        }

        Valoracion valoracion = Valoracion.builder()
                .cita(cita)
                .paciente(paciente)
                .medico(cita.getMedico())
                .puntuacion(request.getPuntuacion())
                .comentario(request.getComentario())
                .build();

        valoracion = valoracionRepository.save(valoracion);
        log.info("Valoración registrada — cita ID: {} | puntuación: {}",
                cita.getId(), request.getPuntuacion());

        return ValoracionResponse.builder()
                .id(valoracion.getId())
                .citaId(cita.getId())
                .medicoNombre(cita.getMedico().getNombres()
                        + " " + cita.getMedico().getApellidos())
                .especialidad(cita.getMedico().getEspecialidad().getNombre())
                .puntuacion(valoracion.getPuntuacion())
                .comentario(valoracion.getComentario())
                .registradoEn(valoracion.getRegistradoEn())
                .build();
    }

    // ─── Métodos privados ─────────────────────────────────────────────────────

    private EspecialidadDirectorioResponse toEspecialidadDirectorio(Especialidad e) {
        List<MedicoDirectorioResponse> medicos = medicoRepository
                .findByEspecialidadIdAndActivoTrue(e.getId())
                .stream()
                .map(this::toMedicoDirectorio)
                .collect(Collectors.toList());

        return EspecialidadDirectorioResponse.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .totalMedicos(medicos.size())
                .medicos(medicos)
                .build();
    }

    private MedicoDirectorioResponse toMedicoDirectorio(Medico m) {
        List<MedicoDirectorioResponse.HorarioDisponibleResponse> horarios =
                horarioRepository.findByMedico(m)
                        .stream()
                        .map(h -> MedicoDirectorioResponse.HorarioDisponibleResponse.builder()
                                .dia(h.getDia().name())
                                .horaInicio(h.getHoraInicio().toString())
                                .horaFin(h.getHoraFin().toString())
                                .build())
                        .collect(Collectors.toList());

        // Promedio de valoraciones del médico
        Double promedio = valoracionRepository.promedioByMedico(m.getId());
        Integer total   = valoracionRepository.countByMedicoId(m.getId());

        return MedicoDirectorioResponse.builder()
                .id(m.getId())
                .nombreCompleto(m.getNombres() + " " + m.getApellidos())
                .especialidad(m.getEspecialidad().getNombre())
                .especialidadId(m.getEspecialidad().getId())
                .activo(m.isActivo())
                .promedioValoracion(promedio != null
                        ? Math.round(promedio * 10.0) / 10.0 : null)
                .totalValoraciones(total != null ? total : 0)
                .horarios(horarios)
                .build();
    }
    
}