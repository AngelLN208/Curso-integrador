/**
 * ui.js — Utilidades de interfaz compartidas
 * Requiere: global.css con las clases .badge-*
 */
const UI = {

  badgeEstado: (estado) => {
    const map = {
      'CONFIRMADA': ['badge-confirmada', '✓ Confirmada'],
      'PENDIENTE': ['badge-pendiente', '⏳ Pendiente'],
      'CANCELADA': ['badge-cancelada', '✕ Cancelada'],
      'REPROGRAMADA': ['badge-reprogramada', '↺ Reprogramada'],
      'ATENDIDA': ['badge-atendida', '★ Atendida'],
    };
    const [cls, label] = map[estado] || ['badge-pendiente', estado];
    return `<span class="badge ${cls}">${label}</span>`;
  },

  formatFecha: (isoDate) => {
    if (!isoDate) return '—';
    const [y, m, d] = isoDate.split('-');
    return `${d}/${m}/${y}`;
  },

  mostrarAlerta: (mensaje, tipo = 'success') => {
    // buscar o crear contenedor de alertas
    let box = document.getElementById('alerta-global');
    if (!box) {
      box = document.createElement('div');
      box.id = 'alerta-global';
      box.style.cssText = 'position:fixed;top:20px;right:24px;z-index:9999;min-width:300px';
      document.body.appendChild(box);
    }
    const cls = tipo === 'success' ? 'alert-success'
      : tipo === 'danger' ? 'alert-danger'
        : 'alert-warning';
    box.innerHTML = `<div class="alert ${cls}" style="box-shadow:0 4px 16px rgba(0,0,0,.12)">
      ${mensaje}
    </div>`;
    setTimeout(() => box.innerHTML = '', 4000);
  },


  mostrarError: (err) => {
    UI.mostrarAlerta(err?.message || 'Ocurrió un error inesperado', 'danger');
  },
  togglePassword: (inputId, iconId) => {
    const input = document.getElementById(inputId);
    const icon = document.getElementById(iconId);
    if (input.type === 'password') {
      input.type = 'text';
      icon.className = 'bi bi-eye-slash';
    } else {
      input.type = 'password';
      icon.className = 'bi bi-eye';
    }
  }
};