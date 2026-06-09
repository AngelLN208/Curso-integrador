package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.repository.PacienteRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final PacienteRepository pacienteRepository;

    public byte[] generarReportePacientesExcel() {
        List<Paciente> pacientes = pacienteRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Pacientes");

            Row header = sheet.createRow(0);
            String[] columnas = {"ID", "DNI", "Nombres", "Apellidos", "Celular", "Correo"};

            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
            }

            int rowIdx = 1;
            for (Paciente p : pacientes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getDni());
                row.createCell(2).setCellValue(p.getNombres());
                row.createCell(3).setCellValue(p.getApellidos());
                row.createCell(4).setCellValue(p.getCelular());
                row.createCell(5).setCellValue(p.getCorreo());
            }

            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Error al generar reporte Excel de pacientes", e);
        }
    }
}