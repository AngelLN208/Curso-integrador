const UI = {
    badgeEstado: (estado) => {
        const clases = {
            'CONFIRMADA': 'bg-success',
            'PENDIENTE':  'bg-warning text-dark',
            'CANCELADA':  'bg-danger',
            'REPROGRAMADA': 'bg-info text-dark'
        };
        return `<span class="badge ${clases[estado] || 'bg-secondary'}">${estado}</span>`;
    },

    formatFecha: (isoDate) => {
        if (!isoDate) return '-';
        const [y, m, d] = isoDate.split('-');
        return `${d}/${m}/${y}`;
    },

    mostrarAlerta: (mensaje, tipo = 'success') => {
        const alerta = document.getElementById('alerta-global');
        if (!alerta) return;
        alerta.className = `alert alert-${tipo} alert-dismissible fade show`;
        alerta.innerHTML = `${mensaje}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
        alerta.style.display = 'block';
        setTimeout(() => alerta.style.display = 'none', 4000);
    },

    mostrarError: (err) => {
        UI.mostrarAlerta(err.message || 'Ocurrió un error', 'danger');
    }
};