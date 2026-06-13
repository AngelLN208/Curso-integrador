package pe.edu.utp.clinica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import pe.edu.utp.clinica.common.enums.RolUsuario;

import java.time.LocalDateTime;

/**
 * Entidad que representa un usuario del sistema.
 *
 * RF-40: Todos los usuarios se autentican con credenciales válidas.
 * RNF-01: La contraseña se almacena cifrada con BCrypt.
 * RNF-03: El rol determina los módulos y acciones permitidas.
 *
 * Roles:
 *   - ROLE_ADMINISTRADOR → gestión de médicos, especialidades, horarios, seguros
 *   - ROLE_RECEPCIONISTA → pacientes, citas, pagos
 *   - ROLE_MEDICO        → triaje, consulta, historial
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Entity
@Table(name = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de usuario para login (correo electrónico) */
    @NotBlank(message = "El username es obligatorio")
    @Email(message = "El username debe ser un correo válido")
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    /**
     * Contraseña cifrada con BCrypt.
     * RNF-01: Nunca se almacena en texto plano.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Column(nullable = false)
    private String password;

    /** Nombre completo del usuario para mostrar en la UI */
    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 150)
    private String nombreCompleto;

    /**
     * Rol del usuario en el sistema.
     * RNF-13: Se usa enum, no texto libre.
     */
    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolUsuario rol;

    /** Indica si la cuenta está activa */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** Fecha de creación del usuario */
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}