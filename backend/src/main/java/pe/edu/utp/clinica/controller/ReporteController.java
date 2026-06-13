package pe.edu.utp.clinica.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.clinica.service.ReporteService;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMINISTRADOR')")
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/pacientes/excel")
    public ResponseEntity<byte[]> descargarPacientesExcel() {
        byte[] excel = reporteService.generarReportePacientesExcel();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pacientes.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}