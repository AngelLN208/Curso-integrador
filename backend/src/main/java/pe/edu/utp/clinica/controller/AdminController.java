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

import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.PacienteSeguro;
import pe.edu.utp.clinica.repository.PacienteSeguroRepository;

import pe.edu.utp.clinica.repository.UsuarioRepository;
import pe.edu.utp.clinica.dto.usuario.UsuarioResponse;
import pe.edu.utp.clinica.model.Usuario;

import pe.edu.utp.clinica.dto.usuario.UsuarioRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.clinica.common.enums.RolUsuario;

import pe.edu.utp.clinica.dto.auditoria.AuditoriaCitaResponse;

import pe.edu.utp.clinica.service.AuditoriaReportService;

import pe.edu.utp.clinica.dto.usuario.EditarUsuarioRequest;

/**
 * Controlador para portal del administrador.
 *
 * RF-41: Auditoría de citas.
 * RF-42: Consultar auditoría por cita.
 * RF-43: Filtrar reporte de auditoría.
 * RF-49: Gestionar seguros médicos.
 * RF-50: Vincular seguro existente a un paciente.
 *
 * Reglas de acceso (RNF-03):
 * - Auditoría: solo ADMINISTRADOR.
 * - Crear/desactivar seguros (catálogo): solo ADMINISTRADOR.
 * - Listar seguros y vincular a paciente: ADMINISTRADOR y RECEPCIONISTA,
 * ya que en la operación real es la recepción quien registra el seguro
 * del paciente al momento de atenderlo, usando el catálogo que el
 * administrador ya configuró previamente.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Portal del administrador")
public class AdminController {

        private final AuditoriaCitaRepository auditoriaRepository;
        private final CitaMedicaRepository citaRepository;
        private final SeguroMedicoRepository seguroRepository;

        private final pe.edu.utp.clinica.repository.PacienteRepository pacienteRepository;
        private final pe.edu.utp.clinica.repository.PacienteSeguroRepository pacienteSeguroRepository;

        private final UsuarioRepository usuarioRepository;

        private final PasswordEncoder passwordEncoder;

        // ─── Auditoría — solo ADMINISTRADOR ──────────────────────────────

        @GetMapping("/auditoria/cita/{citaId}")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        @Operation(summary = "Historial de auditoría por cita", description = "RF-42: Estado anterior, estado nuevo, usuario y fecha.")
        public ResponseEntity<ApiResponse<List<AuditoriaCitaResponse>>> auditoriaPorCita(
                        @PathVariable Long citaId) {

                var cita = citaRepository.findById(citaId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Cita no encontrada con ID: " + citaId));

                List<AuditoriaCitaResponse> auditoria = auditoriaRepository
                                .findByCitaOrderByFechaAccionAsc(cita)
                                .stream()
                                .map(this::toAuditoriaResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(
                                ApiResponse.success("Auditoría obtenida correctamente", auditoria));
        }

        @GetMapping("/auditoria/filtrar")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        @Operation(summary = "Filtrar reporte de auditoría", description = "RF-43: Filtra por usuarioId y/o tipoAccion.")
        public ResponseEntity<ApiResponse<List<AuditoriaCitaResponse>>> filtrarAuditoria(
                        @RequestParam(required = false) Long usuarioId,
                        @RequestParam(required = false) TipoAccion tipoAccion) {

                List<AuditoriaCitaResponse> auditoria = auditoriaRepository
                                .filtrar(usuarioId, tipoAccion)
                                .stream()
                                .map(this::toAuditoriaResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(
                                ApiResponse.success("Reporte de auditoría generado", auditoria));
        }

        // ─── Seguros Médicos — catálogo, solo ADMINISTRADOR ──────────────

        @PostMapping("/seguros")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        @Operation(summary = "Registrar seguro médico", description = "RF-49: Nombre, tipo y porcentaje de cobertura. Solo ADMINISTRADOR.")
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

        private AuditoriaCitaResponse toAuditoriaResponse(AuditoriaCita a) {
                return AuditoriaCitaResponse.builder()
                                .id(a.getId())
                                .citaId(a.getCita() != null ? a.getCita().getId() : null)
                                .pacienteId(a.getCita() != null && a.getCita().getPaciente() != null
                                                ? a.getCita().getPaciente().getId()
                                                : null)
                                .pacienteNombre(a.getCita() != null && a.getCita().getPaciente() != null
                                                ? a.getCita().getPaciente().getNombres() + " "
                                                                + a.getCita().getPaciente().getApellidos()
                                                : null)
                                .usuarioId(a.getUsuario() != null ? a.getUsuario().getId() : null)
                                .usuarioNombre(a.getUsuario() != null ? a.getUsuario().getNombreCompleto() : "Sistema")
                                .tipoAccion(a.getTipoAccion())
                                .estadoAnterior(a.getEstadoAnterior())
                                .estadoNuevo(a.getEstadoNuevo())
                                .fechaAccion(a.getFechaAccion())
                                .build();
        }

        // ─── Listar seguros — ADMINISTRADOR y RECEPCIONISTA ──────────────
        // La recepción necesita ver el catálogo para vincular seguros
        // a los pacientes durante la atención diaria.

        @GetMapping("/seguros")
        @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
        @Operation(summary = "Listar seguros", description = "RF-49: Devuelve TODOS los seguros (activos e inactivos). "
                        + "El frontend de recepcionista filtra solo los activos al vincular.")
        public ResponseEntity<ApiResponse<List<SeguroResponse>>> listarSeguros() {

                List<SeguroResponse> seguros = seguroRepository
                                .findAll()
                                .stream()
                                .map(this::toSeguroResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(
                                ApiResponse.success("Seguros obtenidos correctamente", seguros));
        }

        @DeleteMapping("/seguros/{id}")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        @Operation(summary = "Desactivar seguro médico", description = "RF-49: Desactiva el convenio del seguro. Solo ADMINISTRADOR.")
        public ResponseEntity<ApiResponse<Void>> desactivarSeguro(@PathVariable Long id) {

                SeguroMedico seguro = seguroRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Seguro no encontrado con ID: " + id));

                seguro.setConvenioActivo(false);
                seguroRepository.save(seguro);

                return ResponseEntity.ok(
                                ApiResponse.success("Seguro desactivado correctamente"));
        }

        @PutMapping("/seguros/{id}/activar")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        @Operation(summary = "Reactivar seguro médico", description = "RF-49: Reactiva el convenio del seguro.")
        public ResponseEntity<ApiResponse<Void>> activarSeguro(@PathVariable Long id) {
                SeguroMedico seguro = seguroRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Seguro no encontrado con ID: " + id));
                seguro.setConvenioActivo(true);
                seguroRepository.save(seguro);
                return ResponseEntity.ok(
                                ApiResponse.success("Seguro reactivado correctamente"));
        }

        // ─── Usuarios del sistema — solo ADMINISTRADOR ───────────────────

        @GetMapping("/usuarios")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        @Operation(summary = "Listar usuarios del sistema", description = "Lista todas las cuentas: administradores, recepcionistas y médicos.")
        public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listarUsuarios() {

                List<UsuarioResponse> usuarios = usuarioRepository.findAll()
                                .stream()
                                .map(this::toUsuarioResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(
                                ApiResponse.success("Usuarios obtenidos correctamente", usuarios));
        }

        @PostMapping("/usuarios")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        @Operation(summary = "Crear cuenta de usuario", description = "Crea una cuenta de ADMINISTRADOR o RECEPCIONISTA. "
                        + "Para médicos, usar POST /api/medicos.")
        public ResponseEntity<ApiResponse<UsuarioResponse>> crearUsuario(
                        @Valid @RequestBody UsuarioRequest request) {

                if (request.getRol() == RolUsuario.ROLE_MEDICO) {
                        throw new IllegalStateException(
                                        "Para registrar médicos use el endpoint /api/medicos, "
                                                        + "ya que requiere especialidad y crea el perfil profesional.");
                }

                if (usuarioRepository.existsByUsername(request.getUsername())) {
                        throw new IllegalStateException(
                                        "Ya existe un usuario con el correo: " + request.getUsername());
                }

                Usuario usuario = Usuario.builder()
                                .username(request.getUsername())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .nombreCompleto(request.getNombreCompleto())
                                .rol(request.getRol())
                                .activo(true)
                                .build();

                usuario = usuarioRepository.save(usuario);

                return ResponseEntity.status(201)
                                .body(ApiResponse.created("Usuario creado correctamente", toUsuarioResponse(usuario)));
        }

        @PutMapping("/usuarios/{id}/estado")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        @Operation(summary = "Activar o desactivar usuario", description = "Cambia el estado activo/inactivo de una cuenta.")
        public ResponseEntity<ApiResponse<Void>> cambiarEstadoUsuario(
                        @PathVariable Long id,
                        @RequestParam boolean activo) {

                Usuario usuario = usuarioRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Usuario no encontrado con ID: " + id));

                usuario.setActivo(activo);
                usuarioRepository.save(usuario);

                String mensaje = activo ? "Usuario activado correctamente" : "Usuario desactivado correctamente";
                return ResponseEntity.ok(ApiResponse.success(mensaje));
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

        private UsuarioResponse toUsuarioResponse(Usuario u) {
                return UsuarioResponse.builder()
                                .id(u.getId())
                                .username(u.getUsername())
                                .nombreCompleto(u.getNombreCompleto())
                                .rol(u.getRol())
                                .activo(u.isActivo())
                                .creadoEn(u.getCreadoEn())
                                .build();
        }

        // ─── Vincular seguro a paciente — ADMINISTRADOR y RECEPCIONISTA ──

        @PostMapping("/pacientes/{pacienteId}/seguros/{seguroId}")
        @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
        @Operation(summary = "Vincular seguro a paciente", description = "RF-50: Asocia un seguro médico existente a un paciente.")
        public ResponseEntity<ApiResponse<Void>> vincularSeguro(
                        @PathVariable Long pacienteId,
                        @PathVariable Long seguroId,
                        @RequestParam(required = false) String numeroPoliza) {

                Paciente paciente = pacienteRepository.findById(pacienteId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Paciente no encontrado con ID: " + pacienteId));
                SeguroMedico seguro = seguroRepository.findById(seguroId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Seguro no encontrado con ID: " + seguroId));

                if (!seguro.isConvenioActivo()) {
                        throw new IllegalStateException(
                                        "El seguro no tiene convenio activo: " + seguro.getNombre());
                }

                // Busca si ya existe un vínculo (activo o inactivo) entre paciente y seguro.
                // La BD tiene una restricción única (paciente_id, seguro_id), por lo que
                // no se puede insertar un segundo registro aunque el anterior esté inactivo.
                // Si existe inactivo, se reactiva en vez de crear uno nuevo.
                var vinculoExistente = pacienteSeguroRepository.findByPacienteAndSeguro(paciente, seguro);

                if (vinculoExistente.isPresent()) {
                        PacienteSeguro vinculo = vinculoExistente.get();
                        if (vinculo.isActivo()) {
                                throw new IllegalStateException(
                                                "El paciente ya tiene ese seguro vinculado");
                        }
                        vinculo.setActivo(true);
                        vinculo.setNumeroPoliza(numeroPoliza);
                        pacienteSeguroRepository.save(vinculo);
                        return ResponseEntity.status(201)
                                        .body(ApiResponse.created("Seguro vinculado correctamente", null));
                }

                PacienteSeguro vinculo = PacienteSeguro.builder()
                                .paciente(paciente)
                                .seguro(seguro)
                                .numeroPoliza(numeroPoliza)
                                .activo(true)
                                .build();
                pacienteSeguroRepository.save(vinculo);

                return ResponseEntity.status(201)
                                .body(ApiResponse.created("Seguro vinculado correctamente", null));
        }

        @DeleteMapping("/pacientes/{pacienteId}/seguros/{vinculoId}")
        @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
        @Operation(summary = "Desvincular seguro de paciente", description = "RF-50: Desactiva el vínculo entre un paciente y su seguro.")
        public ResponseEntity<ApiResponse<Void>> desvincularSeguro(
                        @PathVariable Long pacienteId,
                        @PathVariable Long vinculoId) {

                PacienteSeguro vinculo = pacienteSeguroRepository
                                .findByIdAndPacienteId(vinculoId, pacienteId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Vínculo no encontrado para el paciente ID: " + pacienteId));

                vinculo.setActivo(false);
                pacienteSeguroRepository.save(vinculo);

                return ResponseEntity.ok(
                                ApiResponse.success("Seguro desvinculado correctamente"));
        }

        // 1. Agregar el campo junto a los demás repositorios/servicios:
        private final AuditoriaReportService auditoriaReportService;

        // 2. Agregar este nuevo endpoint, junto a "filtrarAuditoria":
        @GetMapping("/auditoria/reporte-pdf")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        @Operation(summary = "Descargar reporte de auditoría en PDF", description = "RF-43: Genera un PDF respetando los mismos filtros de usuarioId y tipoAccion.")
        public ResponseEntity<byte[]> descargarReportePdf(
                        @RequestParam(required = false) Long usuarioId,
                        @RequestParam(required = false) TipoAccion tipoAccion) {

                List<AuditoriaCitaResponse> auditoria = auditoriaRepository
                                .filtrar(usuarioId, tipoAccion)
                                .stream()
                                .map(this::toAuditoriaResponse)
                                .collect(Collectors.toList());

                byte[] pdf = auditoriaReportService.generarPdf(auditoria);

                String nombreArchivo = "reporte-auditoria-" +
                                java.time.LocalDate.now().format(
                                                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                                + ".pdf";

                return ResponseEntity.ok()
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + nombreArchivo + "\"")
                                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                                .body(pdf);
        }

        @PutMapping("/usuarios/{id}")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        @Operation(summary = "Editar cuenta de usuario", description = "Actualiza nombre, correo, rol y opcionalmente la contraseña. "
                        + "Si el campo password viene vacío, la contraseña actual no se modifica.")
        public ResponseEntity<ApiResponse<UsuarioResponse>> editarUsuario(
                        @PathVariable Long id,
                        @Valid @RequestBody EditarUsuarioRequest request) {

                Usuario usuario = usuarioRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Usuario no encontrado con ID: " + id));

                // Verifica que el nuevo correo no esté en uso por OTRA cuenta
                if (!usuario.getUsername().equalsIgnoreCase(request.getUsername())) {
                        boolean correoEnUso = usuarioRepository.findByUsername(request.getUsername())
                                        .filter(u -> !u.getId().equals(id))
                                        .isPresent();
                        if (correoEnUso) {
                                throw new IllegalStateException(
                                                "Ya existe otra cuenta con el correo: " + request.getUsername());
                        }
                }

                usuario.setNombreCompleto(request.getNombreCompleto());
                usuario.setUsername(request.getUsername());
                usuario.setRol(RolUsuario.valueOf(request.getRol()));

                // Solo actualiza la contraseña si se proporcionó una nueva
                if (request.getPassword() != null && !request.getPassword().isBlank()) {
                        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
                }

                usuario = usuarioRepository.save(usuario);

                return ResponseEntity.ok(
                                ApiResponse.success("Cuenta actualizada correctamente", toUsuarioResponse(usuario)));
        }
}