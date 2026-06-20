/**
 * dashboard.js — Panel de recepcionista
 */
AuthService.requireAuth();
const usuario = Auth.getUsuario();
iniciarSidebar('Dashboard');

// ── Saludo y fecha ────────────────────────────────────────────
const horaActual = new Date().getHours();
const saludo = horaActual < 12 ? 'Buenos días' : horaActual < 18 ? 'Buenas tardes' : 'Buenas noches';
document.getElementById('greeting').textContent =
  `${saludo}, ${usuario.nombreCompleto.split(' ')[0]}`;
document.getElementById('fecha-hoy').textContent =
  new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });

// ── Tema ──────────────────────────────────────────────────────
const themeToggle = document.getElementById('themeToggle');
const themeIcon   = document.getElementById('theme-icon');
function aplicarTema(t) {
  document.documentElement.setAttribute('data-theme', t);
  localStorage.setItem('tema', t);
  themeIcon.className = t === 'dark' ? 'bi bi-sun' : 'bi bi-moon-stars';
}
aplicarTema(localStorage.getItem('tema') || 'light');
themeToggle.addEventListener('click', () =>
  aplicarTema(document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark'));

// ── Estado global ─────────────────────────────────────────────
let todasLasCitas = [];

// ── Filtro calendario próximas citas ─────────────────────────
document.getElementById('filtro-fecha-proximas').addEventListener('change', () =>
  renderProximas(todasLasCitas));

function renderProximas(citas) {
  const hoyStr      = new Date().toISOString().split('T')[0];
  const filtroFecha = document.getElementById('filtro-fecha-proximas').value;
  let proximas = citas.filter(c => {
    const fechaCita = c.fechaHora.split('T')[0];
    return fechaCita > hoyStr
      && ['PENDIENTE','CONFIRMADA'].includes(c.estado)
      && (!filtroFecha || fechaCita === filtroFecha);
  });
  proximas = proximas
    .sort((a, b) => new Date(a.fechaHora) - new Date(b.fechaHora))
    .slice(0, 8);
  const el = document.getElementById('proximas-citas');
  if (!proximas.length) {
    el.innerHTML = `<div class="empty-row">
      <i class="bi bi-calendar2-check"
         style="font-size:22px;display:block;margin-bottom:6px;color:var(--text-3)"></i>
      No hay próximas citas${filtroFecha ? ' para esa fecha' : ''}
    </div>`;
    return;
  }
  el.innerHTML = proximas.map(c => {
    const partes   = c.fechaHora.split('T');
    const fechaStr = partes[0];
    const horaStr  = partes[1].substring(0, 5);
    const [h, m]   = horaStr.split(':');
    const fechaDisp = new Date(fechaStr + 'T12:00:00')
      .toLocaleDateString('es-PE', { weekday: 'short', day: 'numeric', month: 'short' });
    return `<div class="appt-item">
      <div class="appt-time">
        <div class="appt-time-h">${h}</div>
        <div class="appt-time-m">${m}</div>
      </div>
      <div class="appt-info">
        <div class="appt-name">${c.pacienteNombre}</div>
        <div class="appt-esp">
          <span style="color:var(--indigo);font-size:11px;font-weight:600">${fechaDisp}</span>
          · ${c.medicoNombre} · ${c.especialidad}
        </div>
      </div>
      ${UI.badgeEstado(c.estado)}
    </div>`;
  }).join('');
}

// ── Cargar dashboard ──────────────────────────────────────────
async function cargarDashboard() {
  try {
    const [citas, pacientes] = await Promise.all([
      CitaService.listar(),
      PacienteService.listar(),
    ]);
    todasLasCitas = citas;
    const hoyStr   = new Date().toISOString().split('T')[0];
    const citasHoy = citas.filter(c => {
      const fechaCita    = c.fechaHora.split('T')[0];
      const estadoValido = ['PENDIENTE','CONFIRMADA','CANCELADA'].includes(c.estado);
      return fechaCita === hoyStr && estadoValido;
    });
    const confirmadas = citas.filter(c => c.estado === 'CONFIRMADA');
    const pendientes  = citas.filter(c => c.estado === 'PENDIENTE');

    document.getElementById('m-citas-hoy').textContent   = citasHoy.length;
    document.getElementById('m-confirmadas').textContent = confirmadas.length;
    document.getElementById('m-pendientes').textContent  = pendientes.length;
    document.getElementById('m-pacientes').textContent   = pacientes.length;

    if (citasHoy.length > 0) {
      const conf = citasHoy.filter(c => c.estado === 'CONFIRMADA').length;
      const pct  = Math.round((conf / citasHoy.length) * 100);
      document.getElementById('m-confirmadas-pct').textContent =
        `${pct}% de hoy confirmadas`;
    }

    const tbody = document.getElementById('tabla-citas-hoy');
    if (!citasHoy.length) {
      tbody.innerHTML = `<tr><td colspan="4" class="empty-row">No hay citas para hoy</td></tr>`;
    } else {
      tbody.innerHTML = citasHoy
        .sort((a, b) => new Date(a.fechaHora) - new Date(b.fechaHora))
        .map(c => {
          const hora = c.fechaHora.split('T')[1].substring(0, 5);
          return `<tr>
            <td><strong>${hora}</strong></td>
            <td>${c.pacienteNombre}</td>
            <td>${c.especialidad}</td>
            <td>${UI.badgeEstado(c.estado)}</td>
          </tr>`;
        }).join('');
    }
    renderProximas(citas);
  } catch (err) {
    console.error('Error cargando dashboard:', err);
  }
}

cargarDashboard();