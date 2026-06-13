package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import pe.edu.utp.clinica.common.enums.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa el pago asociado a una cita médica.
 *
 * RF-11: Se genera automáticamente al registrar una cita (estado PENDIENTE).
 * RF-14: Se registra el pago con monto, fecha y método de pago.
 * RF-17: El estado cambia a PAGADO al confirmar la transacción.
 * RF-19: No se permite pagar una cita CANCELADA.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "pagos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cita médica a la que corresponde este pago */
    @NotNull(message = "La cita es obligatoria")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private CitaMedica cita;

    /** Monto total a pagar */
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    /** Monto final después de aplicar cobertura de seguro (RF-16) */
    @Column(precision = 10, scale = 2)
    private BigDecimal montoFinal;

    /** Método de pago: EFECTIVO, TARJETA, TRANSFERENCIA */
    @Column(length = 30)
    private String metodoPago;

    /** Fecha y hora en que se realizó el pago */
    @Column
    private LocalDateTime fechaPago;

    /**
     * Estado del pago.
     * RNF-13: Se usa enum, no texto libre.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoPago estado = EstadoPago.PENDIENTE;

    /** Fecha de creación del registro */
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}