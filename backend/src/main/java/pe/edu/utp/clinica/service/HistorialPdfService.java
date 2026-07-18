package pe.edu.utp.clinica.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.model.ConsultaMedica;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.repository.ConsultaMedicaRepository;
import pe.edu.utp.clinica.repository.PacienteRepository;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para generar el historial médico del paciente en PDF.
 *
 * RF-53: El paciente puede descargar su historial médico completo
 * en formato PDF con diagnósticos, tratamientos y triaje.
 * RNF-19: El PDF se genera en menos de 3 segundos.
 *
 * Librería: OpenPDF (fork de iText 4, licencia LGPL — uso libre)
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistorialPdfService {

        private final PacienteRepository pacienteRepository;
        private final ConsultaMedicaRepository consultaRepository;

        private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // ── Colores institucionales ─────────────────────────────────────────────
        private static final Color COLOR_PRIMARIO = new Color(30, 58, 95); // azul oscuro
        private static final Color COLOR_SECUNDARIO = new Color(46, 109, 164); // azul medio
        private static final Color COLOR_FONDO = new Color(238, 242, 247);// gris claro

        /**
         * Genera el historial médico completo del paciente en bytes PDF.
         * RF-53: Incluye datos personales, todas las consultas con
         * diagnóstico, tratamiento y observaciones.
         *
         * @param username correo del paciente autenticado
         * @return array de bytes del PDF generado
         */
        @Transactional(readOnly = true)
        public byte[] generarHistorial(String username) {
                Paciente paciente = pacienteRepository.findByCorreo(username)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Paciente no encontrado: " + username));

                List<ConsultaMedica> consultas = consultaRepository
                                .findConHistorialCompletoPorPaciente(paciente);

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
                        PdfWriter writer = PdfWriter.getInstance(doc, baos);

                        // Header y footer en cada página
                        writer.setPageEvent(new HeaderFooterEvent(
                                        paciente.getNombres() + " " + paciente.getApellidos()));

                        doc.open();

                        agregarEncabezado(doc, paciente);
                        agregarResumen(doc, consultas.size());

                        if (consultas.isEmpty()) {
                                doc.add(new Paragraph(
                                                "\nNo se encontraron consultas registradas.",
                                                fuenteNormal(12, Color.GRAY)));
                        } else {
                                for (int i = 0; i < consultas.size(); i++) {
                                        agregarConsulta(doc, consultas.get(i), i + 1);
                                }
                        }

                        doc.close();
                        log.info("PDF generado — paciente ID: {} | consultas: {}",
                                        paciente.getId(), consultas.size());
                        return baos.toByteArray();

                } catch (Exception ex) {
                        log.error("Error al generar PDF para paciente {}: {}",
                                        username, ex.getMessage());
                        throw new RuntimeException("Error al generar el historial PDF", ex);
                }
        }

        // ─── Secciones del PDF ────────────────────────────────────────────────────

        private void agregarEncabezado(Document doc, Paciente p) throws Exception {
                // Título clínica
                Font fuenteTitulo = fuenteTitulo(20, Color.WHITE);
                PdfPTable headerTable = new PdfPTable(1);
                headerTable.setWidthPercentage(100);

                PdfPCell celdaHeader = new PdfPCell();
                celdaHeader.setBackgroundColor(COLOR_PRIMARIO);
                celdaHeader.setPadding(15);
                celdaHeader.setBorder(Rectangle.NO_BORDER);

                Paragraph titulo = new Paragraph("CLÍNICA STELLA MARIS", fuenteTitulo);
                titulo.setAlignment(Element.ALIGN_CENTER);
                celdaHeader.addElement(titulo);

                Paragraph subtitulo = new Paragraph(
                                "Historial Médico del Paciente",
                                fuenteNormal(12, Color.WHITE));
                subtitulo.setAlignment(Element.ALIGN_CENTER);
                celdaHeader.addElement(subtitulo);

                headerTable.addCell(celdaHeader);
                doc.add(headerTable);
                doc.add(Chunk.NEWLINE);

                // Datos del paciente
                PdfPTable datosTable = new PdfPTable(2);
                datosTable.setWidthPercentage(100);
                datosTable.setWidths(new float[] { 1, 1 });
                datosTable.setSpacingBefore(10);

                agregarCeldaDato(datosTable, "Paciente:",
                                p.getNombres() + " " + p.getApellidos());
                agregarCeldaDato(datosTable, "DNI:", p.getDni());
                agregarCeldaDato(datosTable, "Fecha de nacimiento:",
                                p.getFechaNacimiento().toString());
                agregarCeldaDato(datosTable, "Correo:", p.getCorreo());
                agregarCeldaDato(datosTable, "Celular:", p.getCelular());
                agregarCeldaDato(datosTable, "Fecha de impresión:",
                                java.time.LocalDateTime.now().format(FORMATO_FECHA));

                doc.add(datosTable);
                doc.add(Chunk.NEWLINE);
        }

        private void agregarResumen(Document doc, int totalConsultas) throws Exception {
                PdfPTable resumen = new PdfPTable(1);
                resumen.setWidthPercentage(100);

                PdfPCell celda = new PdfPCell();
                celda.setBackgroundColor(COLOR_FONDO);
                celda.setPadding(10);
                celda.setBorderColor(COLOR_SECUNDARIO);

                Paragraph p = new Paragraph(
                                "Total de consultas registradas: " + totalConsultas,
                                fuenteNormal(11, COLOR_PRIMARIO));
                p.setAlignment(Element.ALIGN_CENTER);
                celda.addElement(p);
                resumen.addCell(celda);

                doc.add(resumen);
                doc.add(Chunk.NEWLINE);
        }

        private void agregarConsulta(Document doc, ConsultaMedica c,
                        int numero) throws Exception {
                // Título de la consulta
                PdfPTable tituloTable = new PdfPTable(1);
                tituloTable.setWidthPercentage(100);
                tituloTable.setSpacingBefore(8);

                PdfPCell celdaTitulo = new PdfPCell();
                celdaTitulo.setBackgroundColor(COLOR_SECUNDARIO);
                celdaTitulo.setPadding(8);
                celdaTitulo.setBorder(Rectangle.NO_BORDER);

                String fechaStr = c.getCita().getFechaHora().format(FORMATO_FECHA);
                Paragraph pTitulo = new Paragraph(
                                "Consulta #" + numero + " — " + fechaStr,
                                fuenteNormal(11, Color.WHITE));
                celdaTitulo.addElement(pTitulo);
                tituloTable.addCell(celdaTitulo);
                doc.add(tituloTable);

                // Datos de la consulta
                PdfPTable datos = new PdfPTable(2);
                datos.setWidthPercentage(100);
                datos.setWidths(new float[] { 0.4f, 0.6f });

                String medico = c.getCita().getMedico().getNombres()
                                + " " + c.getCita().getMedico().getApellidos();
                String especialidad = c.getCita().getMedico()
                                .getEspecialidad().getNombre();

                agregarCeldaDato(datos, "Médico:", medico);
                agregarCeldaDato(datos, "Especialidad:", especialidad);
                agregarCeldaDato(datos, "Diagnóstico:", c.getDiagnostico());
                agregarCeldaDato(datos, "Tratamiento:", c.getTratamiento());

                if (c.getObservaciones() != null && !c.getObservaciones().isBlank()) {
                        agregarCeldaDato(datos, "Observaciones:", c.getObservaciones());
                }

                doc.add(datos);
        }

        // ─── Utilidades de formato ────────────────────────────────────────────────

        private void agregarCeldaDato(PdfPTable table,
                        String etiqueta, String valor) {
                PdfPCell celdaEtiqueta = new PdfPCell(
                                new Phrase(etiqueta, fuenteNormal(10, COLOR_PRIMARIO)));
                celdaEtiqueta.setBorderColor(Color.LIGHT_GRAY);
                celdaEtiqueta.setPadding(6);
                celdaEtiqueta.setBackgroundColor(COLOR_FONDO);
                table.addCell(celdaEtiqueta);

                PdfPCell celdaValor = new PdfPCell(
                                new Phrase(valor != null ? valor : "—",
                                                fuenteNormal(10, Color.DARK_GRAY)));
                celdaValor.setBorderColor(Color.LIGHT_GRAY);
                celdaValor.setPadding(6);
                table.addCell(celdaValor);
        }

        private Font fuenteTitulo(int size, Color color) {
                Font f = new Font(Font.HELVETICA, size, Font.BOLD);
                f.setColor(color);
                return f;
        }

        private Font fuenteNormal(int size, Color color) {
                Font f = new Font(Font.HELVETICA, size, Font.NORMAL);
                f.setColor(color);
                return f;
        }

        // ─── Header/Footer por página ─────────────────────────────────────────────

        private static class HeaderFooterEvent extends PdfPageEventHelper {
                private final String nombrePaciente;

                HeaderFooterEvent(String nombrePaciente) {
                        this.nombrePaciente = nombrePaciente;
                }

                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                        PdfContentByte cb = writer.getDirectContent();

                        // Footer con número de página
                        Font fPie = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
                        Phrase pie = new Phrase(
                                        "Clínica Stella Maris  |  " + nombrePaciente
                                                        + "  |  Página " + writer.getPageNumber(),
                                        fPie);
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, pie,
                                        (document.left() + document.right()) / 2,
                                        document.bottom() - 10, 0);
                }
        }
}