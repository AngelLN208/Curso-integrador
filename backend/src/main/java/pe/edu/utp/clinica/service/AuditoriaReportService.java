package pe.edu.utp.clinica.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import pe.edu.utp.clinica.dto.auditoria.AuditoriaCitaResponse;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para generar el reporte de auditoría en formato PDF.
 *
 * RF-43: Exportar el reporte de auditoría (ya filtrado) a PDF.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
public class AuditoriaReportService {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarPdf(List<AuditoriaCitaResponse> registros) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(40, 53, 147));
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f);

            Paragraph titulo = new Paragraph("Reporte de Auditoría — Clínica Stella Maris", tituloFont);
            titulo.setSpacingAfter(4);
            document.add(titulo);

            Paragraph generado = new Paragraph(
                    "Generado el " + LocalDateTime.now().format(FORMATO_FECHA)
                            + "   ·   Total de registros: " + registros.size(),
                    subFont);
            generado.setSpacingAfter(16);
            document.add(generado);

            PdfPTable tabla = new PdfPTable(6);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{2.2f, 2f, 1.8f, 2.5f, 1.8f, 1.8f});

            String[] encabezados = {"Fecha y hora", "Usuario", "Acción", "Paciente / Cita", "Estado anterior", "Estado nuevo"};
            Color colorEncabezado = new Color(63, 81, 181);
            for (String h : encabezados) {
                PdfPCell celda = new PdfPCell(new Phrase(h, headFont));
                celda.setBackgroundColor(colorEncabezado);
                celda.setPadding(6);
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(celda);
            }

            boolean filaClara = true;
            for (AuditoriaCitaResponse r : registros) {
                Color fondo = filaClara ? Color.WHITE : new Color(245, 246, 250);
                filaClara = !filaClara;

                agregarCelda(tabla, r.getFechaAccion() != null ? r.getFechaAccion().format(FORMATO_FECHA) : "—", cellFont, fondo);
                agregarCelda(tabla, r.getUsuarioNombre() != null ? r.getUsuarioNombre() : "Sistema", cellFont, fondo);
                agregarCelda(tabla, formatearAccion(r.getTipoAccion()), cellFont, fondo);
                agregarCelda(tabla, r.getPacienteNombre() != null ? r.getPacienteNombre() : "Cita #" + r.getCitaId(), cellFont, fondo);
                agregarCelda(tabla, r.getEstadoAnterior() != null ? r.getEstadoAnterior().toString() : "—", cellFont, fondo);
                agregarCelda(tabla, r.getEstadoNuevo() != null ? r.getEstadoNuevo().toString() : "—", cellFont, fondo);
            }

            document.add(tabla);
            document.close();

            return baos.toByteArray();
        } catch (DocumentException e) {
            log.error("Error generando el PDF de auditoría", e);
            throw new IllegalStateException("No se pudo generar el reporte PDF de auditoría");
        }
    }

    private void agregarCelda(PdfPTable tabla, String texto, Font font, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, font));
        celda.setPadding(5);
        celda.setBackgroundColor(fondo);
        tabla.addCell(celda);
    }

    private String formatearAccion(Object tipoAccion) {
        if (tipoAccion == null) return "—";
        String valor = tipoAccion.toString();
        return switch (valor) {
            case "CREACION" -> "Creación";
            case "CONFIRMACION" -> "Confirmación";
            case "REPROGRAMACION" -> "Reprogramación";
            case "CANCELACION" -> "Cancelación";
            case "ATENDIDA" -> "Atendida";
            default -> valor;
        };
    }
}