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

    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @GetMapping("/pacientes/excel")
    public ResponseEntity<byte[]> descargarPacientesExcel() {
        byte[] excel = reporteService.generarReportePacientesExcel();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pacientes.xlsx")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(excel);
    }

    @GetMapping("/citas/excel")
    public ResponseEntity<byte[]> descargarCitasExcel() {
        byte[] excel = reporteService.generarReporteCitasExcel();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=citas.xlsx")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(excel);
    }

    @GetMapping("/pagos/excel")
    public ResponseEntity<byte[]> descargarPagosExcel() {
        byte[] excel = reporteService.generarReportePagosExcel();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pagos.xlsx")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(excel);
    }
}