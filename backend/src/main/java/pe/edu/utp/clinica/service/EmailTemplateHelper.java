package pe.edu.utp.clinica.service;

import org.springframework.stereotype.Component;

/**
 * Helper compartido para construir correos HTML con la identidad visual
 * de Clínica Stella Maris (header navy, tarjeta central, footer).
 *
 * Usado por NotificacionScheduler, AuthPacienteService y
 * StaffPasswordResetService para evitar duplicar el esqueleto HTML.
 *
 * @author Equipo Curso Integrador UTP 2026
 */
@Component
public class EmailTemplateHelper {

    public static final String COLOR_TINTA = "#14213D";
    public static final String COLOR_TINTA_CLARO = "#B9C2D6";
    public static final String COLOR_GUIA = "#FF7A45";
    public static final String COLOR_GUIA_BG = "#FFF4EE";
    public static final String COLOR_GUIA_TEXTO = "#993C1D";
    public static final String COLOR_RUMBO = "#2F9E6E";
    public static final String COLOR_RUMBO_BG = "#EAF7F1";
    public static final String COLOR_RUMBO_TEXTO = "#1F6E4C";
    public static final String COLOR_ALERTA = "#E5484D";
    public static final String COLOR_ALERTA_BG = "#FDEEEE";
    public static final String COLOR_ALERTA_TEXTO = "#A72E32";
    public static final String COLOR_NEBLINA = "#8A94A6";
    public static final String COLOR_LIENZO = "#F7F8FA";
    public static final String COLOR_BORDE = "#E4E7EC";

    /**
     * Arma el esqueleto completo del correo (header navy + tarjeta + footer).
     *
     * @param subtituloHeader    texto pequeño bajo el nombre de la clínica
     * @param nombreDestinatario nombre completo de quien recibe el correo
     * @param introHtml          párrafo introductorio (ya en HTML)
     * @param tablaHtml          bloque de detalles tipo tabla (cadena vacía si no
     *                           aplica)
     * @param notaHtml           caja de nota/alerta (cadena vacía si no aplica)
     * @param ctaTexto           texto del botón CTA (null para omitirlo)
     * @param ctaUrl             URL del botón CTA (null si ctaTexto es null)
     */
    public String plantillaCorreo(String subtituloHeader, String nombreDestinatario, String introHtml,
            String tablaHtml, String notaHtml, String ctaTexto, String ctaUrl) {

        String cta = (ctaTexto == null) ? ""
                : """
                        <div style="text-align:center; margin-bottom:6px;">
                          <a href="%s" style="display:inline-block; background:%s; color:#FFFFFF; font-size:13px; font-weight:bold; padding:10px 22px; border-radius:6px; text-decoration:none;">%s</a>
                        </div>
                        """
                        .formatted(ctaUrl, COLOR_TINTA, ctaTexto);

        return """
                <div style="background:%s; padding:32px 16px; font-family:Arial, Helvetica, sans-serif;">
                <div style="max-width:480px; margin:0 auto; background:#FFFFFF; border-radius:10px; overflow:hidden;">

                  <div style="background:%s; padding:28px 24px; text-align:center;">
                    <div style="color:#FFFFFF; font-size:18px; font-weight:bold;">Clínica Stella Maris</div>
                    <div style="color:%s; font-size:12px; margin-top:4px;">%s</div>
                  </div>

                  <div style="padding:24px;">
                    <p style="font-size:14px; color:%s; margin:0 0 4px; line-height:1.5;">Hola <strong>%s</strong>,</p>
                    <p style="font-size:13px; color:%s; line-height:1.6; margin:0 0 20px;">%s</p>

                    %s

                    %s

                    %s
                  </div>

                  <div style="border-top:1px solid %s; padding:16px 24px; text-align:center;">
                    <div style="font-size:11px; color:%s; line-height:1.6;">Clínica Stella Maris &middot; Tel (01) 234-5678<br/>Lunes a sábado, 7:00 am &ndash; 8:00 pm</div>
                  </div>

                </div>
                </div>
                """
                .formatted(
                        COLOR_LIENZO, COLOR_TINTA, COLOR_TINTA_CLARO, subtituloHeader,
                        COLOR_TINTA, nombreDestinatario,
                        COLOR_NEBLINA, introHtml,
                        tablaHtml, notaHtml, cta,
                        COLOR_BORDE, COLOR_NEBLINA);
    }

    /** Caja de nota/alerta con color según el contexto (info, éxito o error). */
    public String cajaNota(String texto, String colorBorde, String colorBg, String colorTexto) {
        return """
                <div style="background:%s; border-left:3px solid %s; border-radius:0 6px 6px 0; padding:10px 14px; margin-bottom:22px;">
                  <span style="font-size:12px; color:%s; line-height:1.5;">%s</span>
                </div>
                """
                .formatted(colorBg, colorBorde, colorTexto, texto);
    }
}