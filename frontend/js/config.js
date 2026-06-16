/**
 * config.js — Configuración global del panel interno
 * ÚNICO archivo donde se define la URL del backend y rutas.
 * Incluir SIEMPRE como PRIMER script en cada HTML.
 */
const CONFIG = {
  API_URL: 'http://localhost:8080/api',
  // API_URL: 'https://clinica-api.onrender.com/api', // producción

  ROUTES: {
    LOGIN:           '/views/auth/login.html',
    RECEP_DASHBOARD: '/views/recepcionist/dashboard.html',
    RECEP_CITAS:     '/views/recepcionist/appointments.html',
    RECEP_PACIENTES: '/views/recepcionist/patients.html',
    RECEP_PAGOS:     '/views/recepcionist/payments.html',
    ADMIN_DASHBOARD: '/views/admin/dashboard.html',
    ADMIN_AUDITORIA: '/views/admin/audit.html',
    ADMIN_REPORTES:  '/views/admin/reports.html',
    ADMIN_ACCESOS:   '/views/admin/acces-control.html',
  }
};