/**
 * notify.js — Sistema de notificaciones del portal
 */
const PortalNotify = {
    mostrar: (mensaje, tipo = 'error') => {
        let box = document.getElementById('portal-toast');
        if (!box) {
            box = document.createElement('div');
            box.id = 'portal-toast';
            box.style.cssText = `
        position: fixed; top: 20px; right: 20px; z-index: 9999;
        max-width: 380px; display: flex; flex-direction: column; gap: 8px;
      `;
            document.body.appendChild(box);
        }

        const colores = {
            error: { bg: '#FEF2F2', text: '#EF4444', border: '#FECACA', icon: 'bi-x-circle' },
            success: { bg: '#ECFDF5', text: '#10B981', border: '#A7F3D0', icon: 'bi-check-circle' },
            info: { bg: '#EFF6FF', text: '#2563EB', border: '#BFDBFE', icon: 'bi-info-circle' },
        };
        const c = colores[tipo] || colores.error;

        const toast = document.createElement('div');
        toast.style.cssText = `
      background:${c.bg}; color:${c.text}; border:1px solid ${c.border};
      border-radius:14px; padding:14px 16px; font-size:13.5px; font-weight:500;
      display:flex; align-items:flex-start; gap:10px; box-shadow:0 8px 24px rgba(0,0,0,.12);
      animation: portalToastIn .25s ease;
    `;
        toast.innerHTML = `<i class="bi ${c.icon}" style="font-size:16px;margin-top:1px"></i><span>${mensaje}</span>`;
        box.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transition = 'opacity .3s';
            setTimeout(() => toast.remove(), 300);
        }, 5000);
    },

    error: (msg) => PortalNotify.mostrar(msg, 'error'),
    success: (msg) => PortalNotify.mostrar(msg, 'success'),
    info: (msg) => PortalNotify.mostrar(msg, 'info'),
};

const estiloToast = document.createElement('style');
estiloToast.textContent = `
  @keyframes portalToastIn {
    from { opacity: 0; transform: translateX(20px); }
    to   { opacity: 1; transform: translateX(0); }
  }
`;
document.head.appendChild(estiloToast);