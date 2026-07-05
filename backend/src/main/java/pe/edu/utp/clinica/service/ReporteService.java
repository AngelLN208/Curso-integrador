package pe.edu.utp.clinica.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para generación de reportes en Excel y PDF.
 *
 * Exporta pacientes, citas y pagos del sistema en ambos formatos
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

    // ══════════════════════════════════════════════════════════════
    // ─── EXCEL ──────────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════
    // ─── PDF ────────────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════

    public byte[] generarReportePacientesPdf() {
        List<Paciente> pacientes = pacienteRepository.findAll();
        String[] columnas = { "ID", "DNI", "Nombres", "Apellidos", "Celular", "Correo" };
        float[] anchos = { 0.8f, 1.3f, 1.8f, 1.8f, 1.3f, 2.3f };

        List<String[]> filas = pacientes.stream()
                .map(p -> new String[] {
                        String.valueOf(p.getId()),
                        p.getDni(),
                        p.getNombres(),
                        p.getApellidos(),
                        p.getCelular(),
                        p.getCorreo() != null ? p.getCorreo() : ""
                })
                .toList();

        return generarPdfGenerico("Reporte de Pacientes", columnas, anchos, filas);
    }

    public byte[] generarReporteCitasPdf() {
        List<CitaMedica> citas = citaRepository.findAll();
        String[] columnas = { "ID", "Fecha y hora", "Paciente", "Médico", "Especialidad", "Motivo", "Estado" };
        float[] anchos = { 0.6f, 1.6f, 1.8f, 1.8f, 1.5f, 2f, 1.2f };

        List<String[]> filas = citas.stream()
                .map(c -> new String[] {
                        String.valueOf(c.getId()),
                        c.getFechaHora().format(FORMATO_FECHA),
                        c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos(),
                        c.getMedico().getNombres() + " " + c.getMedico().getApellidos(),
                        c.getMedico().getEspecialidad().getNombre(),
                        c.getMotivo() != null ? c.getMotivo() : "",
                        c.getEstado().name()
                })
                .toList();

        return generarPdfGenerico("Reporte de Citas Médicas", columnas, anchos, filas);
    }

    public byte[] generarReportePagosPdf() {
        List<Pago> pagos = pagoRepository.findAll();
        String[] columnas = { "ID", "Paciente", "Médico", "Monto bruto", "Monto final", "Método", "Fecha de pago",
                "Estado" };
        float[] anchos = { 0.6f, 1.8f, 1.8f, 1.1f, 1.1f, 1.3f, 1.6f, 1.1f };

        List<String[]> filas = pagos.stream()
                .map(p -> new String[] {
                        String.valueOf(p.getId()),
                        p.getCita().getPaciente().getNombres() + " " + p.getCita().getPaciente().getApellidos(),
                        p.getCita().getMedico().getNombres() + " " + p.getCita().getMedico().getApellidos(),
                        p.getMonto() != null ? String.format("%.2f", p.getMonto().doubleValue()) : "0.00",
                        p.getMontoFinal() != null ? String.format("%.2f", p.getMontoFinal().doubleValue()) : "0.00",
                        p.getMetodoPago() != null ? p.getMetodoPago() : "—",
                        p.getFechaPago() != null ? p.getFechaPago().format(FORMATO_FECHA) : "—",
                        p.getEstado().name()
                })
                .toList();

        return generarPdfGenerico("Reporte de Pagos", columnas, anchos, filas);
    }

    /**
     * Genera un PDF genérico en formato tabla, reutilizado por los 3 reportes.
     */
    private byte[] generarPdfGenerico(String titulo, String[] columnas, float[] anchos, List<String[]> filas) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16,
                    new Color(40, 53, 147));
            com.lowagie.text.Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            com.lowagie.text.Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            com.lowagie.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);
            

            Paragraph tituloParrafo = new Paragraph(titulo + " — Clínica Stella Maris", tituloFont);
            tituloParrafo.setSpacingAfter(4);
            document.add(tituloParrafo);

            Paragraph generado = new Paragraph(
                    "Generado el " + LocalDateTime.now().format(FORMATO_FECHA)
                            + "   ·   Total de registros: " + filas.size(),
                    subFont);
            generado.setSpacingAfter(16);
            document.add(generado);

            PdfPTable tabla = new PdfPTable(columnas.length);
            tabla.setWidthPercentage(100);
            tabla.setWidths(anchos);

            Color colorEncabezado = new Color(63, 81, 181);
            for (String h : columnas) {
                PdfPCell celda = new PdfPCell(new Phrase(h, headFont));
                celda.setBackgroundColor(colorEncabezado);
                celda.setPadding(6);
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(celda);
            }

            boolean filaClara = true;
            for (String[] fila : filas) {
                Color fondo = filaClara ? Color.WHITE : new Color(245, 246, 250);
                filaClara = !filaClara;

                for (String valor : fila) {
                    PdfPCell celda = new PdfPCell(new Phrase(valor, cellFont));
                    celda.setPadding(5);
                    celda.setBackgroundColor(fondo);
                    tabla.addCell(celda);
                }
            }

            document.add(tabla);
            document.close();

            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Error al generar reporte PDF: " + titulo, e);
        }
    }

    // ─── Métodos auxiliares (Excel) ───────────────────────────────

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