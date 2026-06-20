package pe.edu.utp.clinica.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.edu.utp.clinica.model.CitaMedica;
import pe.edu.utp.clinica.model.Pago;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.repository.CitaMedicaRepository;
import pe.edu.utp.clinica.repository.PacienteRepository;
import pe.edu.utp.clinica.repository.PagoRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para generación de reportes en Excel.
 *
 * Exporta pacientes, citas y pagos del sistema en formato .xlsx
 * para que el administrador pueda descargar y analizar la información
 * fuera del sistema (auditorías externas, contabilidad, etc.).
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final PacienteRepository pacienteRepository;
    private final CitaMedicaRepository citaRepository;
    private final PagoRepository pagoRepository;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─── Reporte de pacientes ───────────────────────────────────────

    public byte[] generarReportePacientesExcel() {
        List<Paciente> pacientes = pacienteRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Pacientes");
            String[] columnas = { "ID", "DNI", "Nombres", "Apellidos", "Celular", "Correo" };
            crearEncabezado(workbook, sheet, columnas);

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

            autoajustarColumnas(sheet, columnas.length);
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Error al generar reporte Excel de pacientes", e);
        }
    }

    // ─── Reporte de citas ───────────────────────────────────────────

    public byte[] generarReporteCitasExcel() {
        List<CitaMedica> citas = citaRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Citas");
            String[] columnas = {
                    "ID", "Fecha y hora", "Paciente", "Médico",
                    "Especialidad", "Motivo", "Estado"
            };
            crearEncabezado(workbook, sheet, columnas);

            int rowIdx = 1;
            for (CitaMedica c : citas) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getId());
                row.createCell(1).setCellValue(c.getFechaHora().format(FORMATO_FECHA));
                row.createCell(2).setCellValue(
                        c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos());
                row.createCell(3).setCellValue(
                        c.getMedico().getNombres() + " " + c.getMedico().getApellidos());
                row.createCell(4).setCellValue(c.getMedico().getEspecialidad().getNombre());
                row.createCell(5).setCellValue(c.getMotivo() != null ? c.getMotivo() : "");
                row.createCell(6).setCellValue(c.getEstado().name());
            }

            autoajustarColumnas(sheet, columnas.length);
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Error al generar reporte Excel de citas", e);
        }
    }

    // ─── Reporte de pagos ───────────────────────────────────────────

    public byte[] generarReportePagosExcel() {
        List<Pago> pagos = pagoRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Pagos");
            String[] columnas = {
                    "ID", "Paciente", "Médico", "Monto bruto", "Monto final",
                    "Método de pago", "Fecha de pago", "Estado"
            };
            crearEncabezado(workbook, sheet, columnas);

            int rowIdx = 1;
            for (Pago p : pagos) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(
                        p.getCita().getPaciente().getNombres() + " " + p.getCita().getPaciente().getApellidos());
                row.createCell(2).setCellValue(
                        p.getCita().getMedico().getNombres() + " " + p.getCita().getMedico().getApellidos());
                row.createCell(3).setCellValue(p.getMonto() != null ? p.getMonto().doubleValue() : 0);
                row.createCell(4).setCellValue(p.getMontoFinal() != null ? p.getMontoFinal().doubleValue() : 0);
                row.createCell(5).setCellValue(p.getMetodoPago() != null ? p.getMetodoPago() : "");
                row.createCell(6).setCellValue(
                        p.getFechaPago() != null ? p.getFechaPago().format(FORMATO_FECHA) : "");
                row.createCell(7).setCellValue(p.getEstado().name());
            }

            autoajustarColumnas(sheet, columnas.length);
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Error al generar reporte Excel de pagos", e);
        }
    }

    // ─── Métodos auxiliares ───────────────────────────────────────

    private void crearEncabezado(Workbook workbook, Sheet sheet, String[] columnas) {
        CellStyle estiloHeader = workbook.createCellStyle();
        Font fuenteHeader = workbook.createFont();
        fuenteHeader.setBold(true);
        estiloHeader.setFont(fuenteHeader);
        estiloHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estiloHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header = sheet.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(estiloHeader);
        }
    }

    private void autoajustarColumnas(Sheet sheet, int cantidadColumnas) {
        for (int i = 0; i < cantidadColumnas; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}