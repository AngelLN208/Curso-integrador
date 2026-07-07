/**
 * ui.js — Utilidades de interfaz compartidas (Tailwind)
 */
const UI = {

  badgeEstado: (estado) => {
    const map = {
      'CONFIRMADA': ['bg-rumbo/10 text-rumbo dark:text-rumbo-dark', 'bi-check-circle', 'Confirmada'],
      'PENDIENTE': ['bg-guia/10 text-guia dark:text-guia-dark', 'bi-hourglass-split', 'Pendiente'],
      'CANCELADA': ['bg-alerta/10 text-alerta', 'bi-x-circle', 'Cancelada'],
      'REPROGRAMADA': ['bg-blue-500/10 text-blue-600 dark:text-blue-400', 'bi-arrow-repeat', 'Reprogramada'],
      'ATENDIDA': ['bg-purple-500/10 text-purple-600 dark:text-purple-400', 'bi-star-fill', 'Atendida'],
    };
    const [cls, icon, label] = map[estado] || ['bg-neblina/10 text-neblina', 'bi-question-circle', estado];
    return `<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold whitespace-nowrap ${cls}">
      <i class="bi ${icon}"></i> ${label}
    </span>`;
  },

  formatFecha: (isoDate) => {
    if (!isoDate) return '—';
    const [y, m, d] = isoDate.split('-');
    return `${d}/${m}/${y}`;
  },

  mostrarAlerta: (mensaje, tipo = 'success') => {
    let box = document.getElementById('alerta-global');
    if (!box) {
      box = document.createElement('div');
      box.id = 'alerta-global';
      box.className = 'fixed top-5 right-5 z-[9999] min-w-[300px] max-w-[380px]';
      document.body.appendChild(box);
    }

    const estilos = {
      success: { border: 'border-rumbo', icon: 'bi-check-circle-fill', color: 'text-rumbo' },
      danger: { border: 'border-alerta', icon: 'bi-exclamation-circle-fill', color: 'text-alerta' },
      warning: { border: 'border-guia', icon: 'bi-exclamation-triangle-fill', color: 'text-guia' },
    };
    const s = estilos[tipo] || estilos.success;

    box.innerHTML = `
      <div class="flex items-center gap-3 bg-white dark:bg-superficie-dark border-l-4 ${s.border}
                  rounded-lg shadow-xl px-4 py-3.5 animate-[fadeIn_.2s_ease]">
        <i class="bi ${s.icon} ${s.color} text-lg flex-shrink-0"></i>
        <span class="text-sm font-medium text-tinta dark:text-white">${mensaje}</span>
      </div>`;

    clearTimeout(box._timeout);
    box._timeout = setTimeout(() => { box.innerHTML = ''; }, 4000);
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