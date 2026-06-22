package pe.edu.utp.clinica.dto.atencion;

import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO para editar el triaje de una cita ya registrado.
 * Solo permitido dentro de la ventana de 45 min desde la cita.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Data
public class TriajeEditarRequest {

    private String presionArterial;
    private BigDecimal temperatura;
    private BigDecimal peso;
}