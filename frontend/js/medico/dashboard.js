/**
 * dashboard.js (médico) — Panel principal del médico
 */

AuthService.requireAuth();
const usuario = Auth.getUsuario();
iniciarSidebar('Dashboard');

// ── Tema ──────────────────────────────────────────────────────
const themeToggle = document.getElementById('themeToggle');
const themeIcon = document.getElementById('theme-icon');
function aplicarTema(t) {
    document.documentElement.setAttribute('data-theme', t);
    localStorage.setItem('tema', t);
    themeIcon.className = t === 'dark' ? 'bi bi-sun' : 'bi bi-moon-stars';
}
aplicarTema(localStorage.getItem('tema') || 'light');
themeToggle.addEventListener('click', () =>
    aplicarTema(document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark'));

// ── Saludo y fecha ────────────────────────────────────────────
const horaActual = new Date().getHours();
const saludo = horaActual < 12 ? 'Buenos días' : horaActual < 18 ? 'Buenas tardes' : 'Buenas noches';
document.getElementById('greeting').textContent =
    `${saludo}, Dr(a). ${usuario.nombreCompleto.split(' ')[0]} — aquí tienes tu resumen de hoy`;
document.getElementById('fecha-hoy').textContent =
    new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });

// ── Cargar dashboard ──────────────────────────────────────────
async function cargarDashboard() {
    if (!usuario.medicoId) {
        document.getElementById('lista-citas-hoy').innerHTML =
            `<div class="text-center text-neblina py-10 text-[13px]">No se encontró tu perfil de médico. Contacta al administrador.</div>`;
        return;
    }

    try {
        const json = await apiFetch(`/citas/buscar?medicoId=${usuario.medicoId}`);
        const misCitas = json.data ?? json;

        const hoyStr = new Date().toISOString().split('T')[0];
        const citasHoy = misCitas.filter(c => c.fechaHora.startsWith(hoyStr));

        document.getElementById('m-citas-hoy').textContent = citasHoy.length;
        document.getElementById('m-pendientes').textContent =
            citasHoy.filter(c => c.estado === 'CONFIRMADA' || c.estado === 'PENDIENTE').length;

        const mesActual = new Date().getMonth();
        const anioActual = new Date().getFullYear();
        const citasMes = misCitas.filter(c => {
            const f = new Date(c.fechaHora);
            return f.getMonth() === mesActual && f.getFullYear() === anioActual;
        });

        document.getElementById('m-atendidas-mes').textContent =
            citasMes.filter(c => c.estado === 'ATENDIDA').length;

        const pacientesUnicos = new Set(citasMes.map(c => c.pacienteId));
        document.getElementById('m-pacientes-mes').textContent = pacientesUnicos.size;

        renderCitasHoy(citasHoy);

    } catch (err) {
        document.getElementById('lista-citas-hoy').innerHTML =
            `<div class="text-center text-neblina py-10 text-[13px]">No se pudo cargar la agenda</div>`;
    }
}

function renderCitasHoy(citas) {
    const cont = document.getElementById('lista-citas-hoy');

    if (!citas.length) {
        cont.innerHTML = `<div class="text-center text-neblina py-10 text-[13px]">
      <i class="bi bi-calendar-x block text-2xl mb-2"></i>
      No tienes citas programadas para hoy
    </div>`;
        return;
    }

    const ordenadas = [...citas].sort((a, b) => new Date(a.fechaHora) - new Date(b.fechaHora));

    cont.innerHTML = ordenadas.map(c => {
        const hora = new Date(c.fechaHora);
        const h = hora.getHours().toString().padStart(2, '0');
        const m = hora.getMinutes().toString().padStart(2, '0');

        return `
      <div class="flex items-center gap-3 px-5 py-3">
        <div class="w-14 text-center flex-shrink-0">
          <span class="font-jetbrains text-[13px] font-medium text-tinta dark:text-white">${h}:${m}</span>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[13px] font-medium text-tinta dark:text-white truncate">${c.pacienteNombre}</div>
          <div class="text-xs text-neblina truncate">${c.motivo || 'Sin motivo especificado'}</div>
        </div>
        ${UI.badgeEstado(c.estado)}
      </div>`;
    }).join('');
}

cargarDashboard();