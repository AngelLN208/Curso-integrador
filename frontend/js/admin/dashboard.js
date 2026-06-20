/**
 * dashboard.js (admin) — Panel de administración
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
    `${saludo}, ${usuario.nombreCompleto.split(' ')[0]} — aquí tienes el resumen de hoy`;
document.getElementById('fecha-hoy').textContent =
    new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });

// ── Colores por especialidad (paleta consistente con el sistema) ──
const COLORES_ESP = ['indigo', 'blue', 'green', 'amber', 'purple', 'teal', 'red'];

// ── Cargar dashboard ──────────────────────────────────────────
async function cargarDashboard() {
    try {
        const [citasRes, pacientes, medicos, usuarios] = await Promise.all([
            CitaService.listar(),
            PacienteService.listar(),
            MedicoService.listar(),
            AdminService.listarUsuarios(),
        ]);

        // Métricas principales
        const hoyStr = new Date().toISOString().split('T')[0];
        const citasHoy = citasRes.filter(c => c.fechaHora.startsWith(hoyStr));

        document.getElementById('m-pacientes').textContent = pacientes.length;
        document.getElementById('m-medicos').textContent = medicos.length;
        document.getElementById('m-citas-hoy').textContent = citasHoy.length;
        document.getElementById('m-usuarios').textContent = usuarios.length;

        // Gráfico de citas por especialidad (mes actual)
        const mesActual = new Date().getMonth();
        const anioActual = new Date().getFullYear();
        const citasMes = citasRes.filter(c => {
            const f = new Date(c.fechaHora);
            return f.getMonth() === mesActual && f.getFullYear() === anioActual;
        });

        const conteoPorEspecialidad = {};
        citasMes.forEach(c => {
            conteoPorEspecialidad[c.especialidad] = (conteoPorEspecialidad[c.especialidad] || 0) + 1;
        });

        const nombreMes = new Date().toLocaleDateString('es-PE', { month: 'long' });
        document.getElementById('periodo-label').textContent =
            nombreMes.charAt(0).toUpperCase() + nombreMes.slice(1);

        renderChartEspecialidades(conteoPorEspecialidad);

        // Actividad reciente — usa auditoría sin filtros (las más recientes)
        await cargarActividadReciente();

    } catch (err) {
        console.error('Error cargando dashboard admin:', err);
    }
}

function renderChartEspecialidades(conteo) {
    const el = document.getElementById('chart-especialidades');
    const entradas = Object.entries(conteo).sort((a, b) => b[1] - a[1]);

    if (!entradas.length) {
        el.innerHTML = `<div class="empty-row">Sin citas registradas este mes</div>`;
        return;
    }

    const maxValor = Math.max(...entradas.map(e => e[1]));
    const ALTURA_MAX = 180; // px

    const barras = entradas.map(([especialidad, cantidad], i) => {
        const alturaPx = Math.max(8, Math.round((cantidad / maxValor) * ALTURA_MAX));
        const color = COLORES_ESP[i % COLORES_ESP.length];

        return `
      <div style="display:flex;flex-direction:column;align-items:center;
                  flex:1;min-width:0;gap:8px">
        <div style="font-size:13px;font-weight:700;color:var(--text)">${cantidad}</div>
        <div style="width:100%;max-width:48px;height:${ALTURA_MAX}px;
                    display:flex;align-items:flex-end;justify-content:center">
          <div class="chart-bar"
               style="width:100%;height:${alturaPx}px;
                      background:linear-gradient(180deg,var(--${color}),var(--${color}));
                      border-radius:8px 8px 4px 4px;
                      box-shadow:0 2px 8px rgba(0,0,0,.08);
                      transition:height .6s ease, transform .15s;
                      cursor:default"
               onmouseover="this.style.transform='scaleY(1.03)'"
               onmouseout="this.style.transform='scaleY(1)'"
               title="${especialidad}: ${cantidad} citas"></div>
        </div>
        <div style="font-size:11px;color:var(--text-3);text-align:center;
                    line-height:1.3;max-width:80px;overflow:hidden;
                    text-overflow:ellipsis;white-space:nowrap"
             title="${especialidad}">${especialidad}</div>
      </div>`;
    }).join('');

    el.innerHTML = `
    <div style="display:flex;align-items:flex-end;gap:12px;
                padding:10px 4px 0;min-height:${ALTURA_MAX + 50}px">
      ${barras}
    </div>`;
}

async function cargarActividadReciente() {
    const tbody = document.getElementById('tabla-actividad');
    try {
        const auditoria = await AdminService.filtrarAuditoria(null, null);

        const recientes = auditoria
            .sort((a, b) => new Date(b.fechaAccion) - new Date(a.fechaAccion))
            .slice(0, 8);

        if (!recientes.length) {
            tbody.innerHTML = `<tr><td colspan="5" class="empty-row">Sin actividad registrada aún</td></tr>`;
            return;
        }

        tbody.innerHTML = recientes.map(a => {
            const fecha = new Date(a.fechaAccion).toLocaleDateString('es-PE',
                { day: '2-digit', month: '2-digit', year: 'numeric' });
            const hora = new Date(a.fechaAccion).toLocaleTimeString('es-PE',
                { hour: '2-digit', minute: '2-digit' });
            const usuarioNombre = a.usuarioNombre || 'Sistema';
            const accionLabel = formatearAccion(a.tipoAccion);

            return `<tr>
        <td style="font-size:12px;color:var(--text-2)">${fecha} ${hora}</td>
        <td>${usuarioNombre}</td>
        <td>${accionLabel}</td>
        <td>${a.estadoAnterior ? UI.badgeEstado(a.estadoAnterior) : '—'}</td>
        <td>${a.estadoNuevo ? UI.badgeEstado(a.estadoNuevo) : '—'}</td>
      </tr>`;
        }).join('');

    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty-row">
      No se pudo cargar la actividad reciente
    </td></tr>`;
    }
}

function formatearAccion(tipo) {
    const map = {
        CREACION: '<i class="bi bi-plus-circle" style="color:var(--green)"></i> Cita creada',
        CONFIRMACION: '<i class="bi bi-check-circle" style="color:var(--blue)"></i> Cita confirmada',
        REPROGRAMACION: '<i class="bi bi-arrow-repeat" style="color:var(--amber)"></i> Cita reprogramada',
        CANCELACION: '<i class="bi bi-x-circle" style="color:var(--red)"></i> Cita cancelada',
        ATENDIDA: '<i class="bi bi-clipboard-check" style="color:var(--purple)"></i> Cita atendida',
    };
    return map[tipo] || tipo;
}

cargarDashboard();