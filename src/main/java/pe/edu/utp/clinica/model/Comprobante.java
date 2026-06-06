package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa el comprobante de pago generado
 * automáticamente al confirmar un pago.
 *
 * RF-18: Se genera con número único y fecha de emisión.
 * RF-20: Se puede enviar al correo del paciente.
 * RNF-08: El comprobante está disponible antes de que
 *         la respuesta HTTP llegue al cliente.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "comprobantes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Número único del comprobante.
     * Formato: COMP-{año}-{id con ceros} → Ej: COMP-2026-000001
     */
    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    /** Pago al que corresponde este comprobante (uno a uno) */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false, unique = true)
    private Pago pago;

    /** Tipo de comprobante: BOLETA o FACTURA */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String tipo = "BOLETA";

    /** Fecha y hora de emisión del comprobante */
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaEmision;

    /** Indica si el comprobante fue enviado por correo (RF-20) */
    @Column(nullable = false)
    @Builder.Default
    private boolean enviado = false;

    @PrePersist
    protected void onCreate() {
        this.fechaEmision = LocalDateTime.now();
    }
}