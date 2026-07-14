package pe.edu.utp.clinica.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.utp.clinica.common.enums.EstadoCita;
import pe.edu.utp.clinica.common.enums.EstadoPago;
import pe.edu.utp.clinica.dto.pago.PagoRequest;
import pe.edu.utp.clinica.dto.pago.PagoResponse;
import pe.edu.utp.clinica.model.*;
import pe.edu.utp.clinica.repository.*;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;

import java.time.format.DateTimeFormatter;

/**
 * Servicio para gestión de pagos y comprobantes.
 *
 * RF-14: Registrar pago con monto, fecha y método.
 * RF-15: Confirmar cita al validar pago.
 * RF-16: Aplicar cobertura del seguro del paciente al monto final.
 * RF-17: Actualizar estado del pago a PAGADO.
 * RF-18: Generar comprobante automáticamente con número único.
 * RF-19: Restringir pago en cita cancelada.
 * RF-35: Listar pagos por paciente (query JPQL, no findAll).
 * RNF-08: Comprobante disponible antes de respuesta HTTP.
 *
 * BUGS CORREGIDOS:
 * - BUG 1 (RF-35): listarPorPaciente() hacía findAll() + filter en memoria.
 * Corregido: usa query JPQL directo en PagoRepository.
 * - BUG 2 (RF-18): generarComprobante() usaba count()+1, race condition.
 * Corregido: usa el ID del pago (único por BD) como base del número.
 * - BUG 3 (RF-16): seguro nunca se consultaba, montoFinal = monto siempre.
 * Corregido: se busca seguro activo y se aplica cobertura con BigDecimal.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

        private final PagoRepository pagoRepository;
        private final ComprobanteRepository comprobanteRepository;
        private final PacienteSeguroRepository pacienteSeguroRepository;
        private final CitaService citaService;
        private final NotificacionRepository notificacionRepository;

        /**
         * Registra el pago de una cita, aplica el seguro si existe,
         * confirma la cita y genera el comprobante.
         *
         * @param request  datos del pago (citaId, monto, metodoPago)
         * @param username usuario que registra el cobro (recepcionista)
         * @return PagoResponse con todos los datos del pago
         */
        @Transactional
        public PagoResponse registrarPago(PagoRequest request, String username) {
                CitaMedica cita = citaService.buscarEntidadPorId(request.getCitaId());

                // RF-19: No se puede pagar una cita cancelada
                if (cita.getEstado() == EstadoCita.CANCELADA) {
                        log.warn("Intento de pago sobre cita cancelada — cita ID: {}", request.getCitaId());
                        throw new IllegalStateException(
                                        "No se puede registrar el pago de una cita cancelada.");
                }

                Pago pago = pagoRepository.findByCita(cita)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No se encontró el pago asociado a la cita ID: "
                                                                + request.getCitaId()));

                if (pago.getEstado() == EstadoPago.PAGADO) {
                        log.warn("Intento de pago duplicado — cita ID: {}", request.getCitaId());
                        throw new IllegalStateException(
                                        "Esta cita ya tiene un pago registrado.");
                }

                // RF-16: Calcular monto final aplicando cobertura del seguro con BigDecimal
                BigDecimal montoFinal = calcularMontoConSeguro(
                                request.getMonto(), cita.getPaciente());

                // RF-17: Actualizar estado del pago a PAGADO
                pago.setMonto(request.getMonto());
                pago.setMontoFinal(montoFinal);
                pago.setMetodoPago(request.getMetodoPago());
                pago.setFechaPago(LocalDateTime.now());
                pago.setEstado(EstadoPago.PAGADO);
                pago = pagoRepository.save(pago);

                // RF-15: Confirmar cita al validar pago
                citaService.confirmar(cita.getId(), username);

                // RF-18: Generar comprobante automáticamente (RNF-08)
                generarComprobante(pago);

                // RF-20: Notificar al paciente que su pago fue confirmado
                Notificacion notifPago = Notificacion.builder()
                                .paciente(cita.getPaciente())
                                .cita(cita)
                                .tipo("PAGO_CONFIRMADO")
                                .mensaje("Su pago de S/ " + montoFinal.toPlainString()
                                                + " ha sido registrado. Su cita queda CONFIRMADA.")
                                .estado("PENDIENTE")
                                .build();
                notificacionRepository.save(notifPago);

                log.info("Pago registrado — cita ID: {} | monto: {} | montoFinal: {}",
                                cita.getId(), request.getMonto(), montoFinal);
                return toResponse(pago);
        }

        /**
         * Previsualiza el descuento de seguro para una cita, sin registrar el pago.
         * RF-16: Permite a la recepcionista ver el descuento ANTES de confirmar el
         * cobro.
         *
         * @param citaId ID de la cita a pagar
         * @return datos del cálculo: monto bruto, descuento, monto final, nombre del
         *         seguro
         */
        @Transactional(readOnly = true)
        public pe.edu.utp.clinica.dto.pago.PrevisualizarPagoResponse previsualizarPago(Long citaId) {
                CitaMedica cita = citaService.buscarEntidadPorId(citaId);
                BigDecimal montoBruto = cita.getMedico().getEspecialidad().getCosto();

                return pacienteSeguroRepository
                                .findFirstByPacienteAndActivoTrue(cita.getPaciente())
                                .map(ps -> {
                                        BigDecimal cobertura = ps.getSeguro().getPorcentajeCobertura();
                                        BigDecimal descuento = montoBruto
                                                        .multiply(cobertura)
                                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                                        BigDecimal montoFinal = montoBruto.subtract(descuento);

                                        return pe.edu.utp.clinica.dto.pago.PrevisualizarPagoResponse.builder()
                                                        .monto(montoBruto)
                                                        .tieneSeguro(true)
                                                        .nombreSeguro(ps.getSeguro().getNombre())
                                                        .porcentajeCobertura(cobertura)
                                                        .descuento(descuento)
                                                        .montoFinal(montoFinal)
                                                        .build();
                                })
                                .orElse(pe.edu.utp.clinica.dto.pago.PrevisualizarPagoResponse.builder()
                                                .monto(montoBruto)
                                                .tieneSeguro(false)
                                                .descuento(BigDecimal.ZERO)
                                                .montoFinal(montoBruto)
                                                .build());
        }

        /**
         * Obtiene los datos completos del pago asociado a una cita.
         * Usado para mostrar el comprobante/boleta.
         *
         * @param citaId ID de la cita
         * @return datos del pago
         */
        @Transactional(readOnly = true)
        public PagoResponse obtenerPorCita(Long citaId) {
                CitaMedica cita = citaService.buscarEntidadPorId(citaId);

                Pago pago = pagoRepository.findByCita(cita)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No se encontró el pago asociado a la cita ID: " + citaId));

                return toResponse(pago);
        }

        /**
         * Lista todos los pagos de un paciente específico.
         * RF-35: Query JPQL directo — evita findAll() + filter en memoria.
         *
         * @param pacienteId ID del paciente
         * @return lista de pagos del paciente ordenados por fecha descendente
         */
        @Transactional(readOnly = true)
        public List<PagoResponse> listarPorPaciente(Long pacienteId) {
                return pagoRepository.findByCitaPacienteId(pacienteId)
                                .stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        // ─── Métodos privados ─────────────────────────────────────────────────────

        /**
         * Calcula el monto final aplicando la cobertura del seguro (RF-16).
         * Usa BigDecimal para evitar errores de precisión en operaciones monetarias.
         * Ejemplo: monto=100.00, cobertura=30% → montoFinal=70.00
         */
        private BigDecimal calcularMontoConSeguro(BigDecimal montoBruto, Paciente paciente) {
                return pacienteSeguroRepository
                                .findFirstByPacienteAndActivoTrue(paciente)
                                .map(ps -> {
                                        BigDecimal cobertura = ps.getSeguro().getPorcentajeCobertura();
                                        BigDecimal descuento = montoBruto
                                                        .multiply(cobertura)
                                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                                        BigDecimal montoFinal = montoBruto.subtract(descuento);
                                        log.info("Seguro aplicado — paciente ID: {} | cobertura: {}% | "
                                                        + "bruto: {} | descuento: {} | final: {}",
                                                        paciente.getId(), cobertura,
                                                        montoBruto, descuento, montoFinal);
                                        return montoFinal;
                                })
                                .orElse(montoBruto);
        }

        /**
         * Genera el comprobante de pago con número único.
         * RF-18: Formato COMP-{año}-{id con ceros}.
         * CORRECCIÓN: usa pago.getId() en lugar de count()+1 — sin race condition.
         */
        private void generarComprobante(Pago pago) {
                String numero = String.format("COMP-%d-%06d",
                                Year.now().getValue(), pago.getId());

                Comprobante comprobante = Comprobante.builder()
                                .numero(numero)
                                .pago(pago)
                                .tipo("BOLETA")
                                .enviado(false)
                                .build();

                comprobanteRepository.save(comprobante);
                log.info("Comprobante generado: {}", numero);
        }

        private PagoResponse toResponse(Pago p) {
                return PagoResponse.builder()
                                .id(p.getId())
                                .citaId(p.getCita().getId())
                                .pacienteNombre(p.getCita().getPaciente().getNombres()
                                                + " " + p.getCita().getPaciente().getApellidos())
                                .medicoNombre(p.getCita().getMedico().getNombres() // ← agregar
                                                + " " + p.getCita().getMedico().getApellidos()) // ← agregar
                                .monto(p.getMonto())
                                .montoFinal(p.getMontoFinal())
                                .metodoPago(p.getMetodoPago())
                                .fechaPago(p.getFechaPago())
                                .estado(p.getEstado())
                                .creadoEn(p.getCreadoEn())
                                .build();
        }

        /**
         * Genera la boleta de pago en PDF para adjuntar al correo.
         * RF-18 (extendido): el paciente recibe su comprobante por correo.
         *
         * @param pago pago confirmado con todos sus datos
         * @return bytes del PDF generado
         */
        public byte[] generarBoletaPdf(Pago pago) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
                try {
                        PdfWriter.getInstance(doc, out);
                        doc.open();

                        Font fontTitulo = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 58, 95));
                        Font fontSub = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(80, 80, 80));
                        Font fontLabel = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
                        Font fontValor = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
                        Font fontTotal = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(30, 58, 95));
                        Font fontFooter = new Font(Font.HELVETICA, 9, Font.ITALIC, new Color(120, 120, 120));

                        CitaMedica cita = pago.getCita();
                        Paciente paciente = cita.getPaciente();
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                        // Encabezado
                        Paragraph titulo = new Paragraph("🏥 Clínica Stella Maris", fontTitulo);
                        titulo.setAlignment(Element.ALIGN_CENTER);
                        titulo.setSpacingAfter(4);
                        doc.add(titulo);

                        Paragraph sub = new Paragraph("Comprobante de Pago", fontSub);
                        sub.setAlignment(Element.ALIGN_CENTER);
                        sub.setSpacingAfter(20);
                        doc.add(sub);

                        // Línea separadora
                        PdfPTable linea = new PdfPTable(1);
                        linea.setWidthPercentage(100);
                        PdfPCell celdaLinea = new PdfPCell();
                        celdaLinea.setBorderWidthTop(1.5f);
                        celdaLinea.setBorderColorTop(new Color(30, 58, 95));
                        celdaLinea.setBorderWidthBottom(0);
                        celdaLinea.setBorderWidthLeft(0);
                        celdaLinea.setBorderWidthRight(0);
                        celdaLinea.setFixedHeight(8);
                        linea.addCell(celdaLinea);
                        doc.add(linea);

                        // Tabla de datos
                        PdfPTable tabla = new PdfPTable(2);
                        tabla.setWidthPercentage(100);
                        tabla.setWidths(new float[] { 35, 65 });
                        tabla.setSpacingBefore(10);
                        tabla.setSpacingAfter(16);

                        java.util.function.BiConsumer<String, String> fila = (label, valor) -> {
                                PdfPCell cl = new PdfPCell(new Phrase(label, fontLabel));
                                cl.setBorder(Rectangle.BOTTOM);
                                cl.setBorderColor(new Color(220, 220, 220));
                                cl.setPadding(7);
                                cl.setBackgroundColor(new Color(245, 248, 252));

                                PdfPCell cv = new PdfPCell(new Phrase(valor, fontValor));
                                cv.setBorder(Rectangle.BOTTOM);
                                cv.setBorderColor(new Color(220, 220, 220));
                                cv.setPadding(7);

                                tabla.addCell(cl);
                                tabla.addCell(cv);
                        };

                        fila.accept("Paciente", paciente.getNombres() + " " + paciente.getApellidos());
                        fila.accept("DNI", paciente.getDni());
                        fila.accept("Médico", "Dr(a). " + cita.getMedico().getNombres()
                                        + " " + cita.getMedico().getApellidos());
                        fila.accept("Especialidad", cita.getMedico().getEspecialidad().getNombre());
                        fila.accept("Fecha de cita", cita.getFechaHora().format(fmt));
                        fila.accept("Método de pago", pago.getMetodoPago() != null
                                        ? pago.getMetodoPago().toString()
                                        : "—");
                        fila.accept("Fecha de pago", pago.getFechaPago() != null
                                        ? pago.getFechaPago().format(fmt)
                                        : "—");

                        if (pago.getMonto() != null && pago.getMontoFinal() != null
                                        && pago.getMonto().compareTo(pago.getMontoFinal()) != 0) {
                                fila.accept("Precio base", "S/ " + pago.getMonto().toPlainString());
                                java.math.BigDecimal descuento = pago.getMonto().subtract(pago.getMontoFinal());
                                fila.accept("Descuento seguro", "-S/ " + descuento.toPlainString());
                        }

                        doc.add(tabla);

                        // Total
                        Paragraph total = new Paragraph(
                                        "TOTAL PAGADO: S/ " + pago.getMontoFinal().toPlainString(), fontTotal);
                        total.setAlignment(Element.ALIGN_RIGHT);
                        total.setSpacingAfter(30);
                        doc.add(total);

                        // Footer
                        Paragraph footer = new Paragraph(
                                        "Gracias por confiar en Clínica Stella Maris.\n"
                                                        + "Horario de atención: Lunes a Sábado, 7:00 AM – 8:00 PM\n"
                                                        + "Tel: (01) 234-5678",
                                        fontFooter);
                        footer.setAlignment(Element.ALIGN_CENTER);
                        doc.add(footer);

                } catch (Exception e) {
                        log.error("Error generando boleta PDF: {}", e.getMessage());
                } finally {
                        if (doc.isOpen())
                                doc.close();
                }
                return out.toByteArray();
        }

        /** Expone el repositorio de pagos para uso interno del scheduler. */
        public PagoRepository getPagoRepository() {
                return pagoRepository;
        }
}