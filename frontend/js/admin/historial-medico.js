/**
 * historial-medico.js — Historial médico consolidado por paciente
 */

AuthService.requireAuth();
iniciarSidebar('Hist. médico');

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

// ── Modales (no usados aquí, pero por consistencia) ───────────
function abrirModal(id) { document.getElementById(id)?.classList.add('open'); }
function cerrarModal(id) { document.getElementById(id)?.classList.remove('open'); }

// ── Buscar paciente ───────────────────────────────────────────
let buscarTimeout;
let todosPacientes = [];

document.getElementById('buscar-paciente').addEventListener('input', (e) => {
    clearTimeout(buscarTimeout);
    const criterio = e.target.value.trim();

    if (!criterio) {
        document.getElementById('resultados-busqueda').style.display = 'none';
        return;
    }

    buscarTimeout = setTimeout(async () => {
        try {
            const resultados = await PacienteService.buscar(criterio);
            mostrarResultados(resultados);
        } catch (err) { UI.mostrarError(err); }
    }, 350);
});

function mostrarResultados(pacientes) {
    const cont = document.getElementById('resultados-busqueda');

    if (!pacientes.length) {
        cont.innerHTML = `<div style="padding:10px;color:var(--text-3);font-size:13px">Sin resultados</div>`;
        cont.style.display = 'block';
        return;
    }

    cont.innerHTML = pacientes.map(p => `
    <div onclick="seleccionarPaciente(${p.id})"
         style="padding:10px 12px;border-radius:8px;cursor:pointer;
                background:var(--bg-main);margin-bottom:4px;transition:background .15s"
         onmouseover="this.style.background='var(--indigo-lt)'"
         onmouseout="this.style.background='var(--bg-main)'">
      <span style="font-weight:500;color:var(--text)">${p.nombres} ${p.apellidos}</span>
      <span style="font-size:12px;color:var(--text-3);margin-left:8px">DNI: ${p.dni}</span>
    </div>`).join('');
    cont.style.display = 'block';
}

async function seleccionarPaciente(id) {
    document.getElementById('resultados-busqueda').style.display = 'none';
    document.getElementById('buscar-paciente').value = '';

    try {
        const p = await PacienteService.getById(id);
        const iniciales = (p.nombres.charAt(0) + p.apellidos.charAt(0)).toUpperCase();

        document.getElementById('paciente-iniciales').textContent = iniciales;
        document.getElementById('paciente-nombre').textContent = `${p.nombres} ${p.apellidos}`;
        document.getElementById('paciente-dni').textContent = `DNI: ${p.dni}`;
        document.getElementById('card-paciente').style.display = 'block';
        document.getElementById('card-vacio').style.display = 'none';

        await cargarHistorial(id);
    } catch (err) { UI.mostrarError(err); }
}

// ── Cargar historial ───────────────────────────────────────────
async function cargarHistorial(pacienteId) {
    const cont = document.getElementById('lista-consultas');
    document.getElementById('card-historial').style.display = 'block';
    document.getElementById('metricas-historial').style.display = 'grid';
    cont.innerHTML = `<div class="empty-row">Cargando...</div>`;

    try {
        const json = await apiFetch(`/atencion/historial/${pacienteId}`);
        const consultas = json.data ?? json;

        calcularMetricas(consultas);
        renderConsultas(consultas);

    } catch (err) {
        cont.innerHTML = `<div class="empty-row">No se pudo cargar el historial médico</div>`;
    }
}

function calcularMetricas(consultas) {
    document.getElementById('m-total-consultas').textContent = consultas.length;

    if (!consultas.length) {
        document.getElementById('m-ultima-consulta').textContent = '—';
        document.getElementById('m-medico-frecuente').textContent = '—';
        return;
    }

    const ordenadas = [...consultas].sort((a, b) => new Date(b.fechaCita) - new Date(a.fechaCita));
    document.getElementById('m-ultima-consulta').textContent =
        new Date(ordenadas[0].fechaCita).toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' });

    const conteoMedicos = {};
    consultas.forEach(c => { conteoMedicos[c.medicoNombre] = (conteoMedicos[c.medicoNombre] || 0) + 1; });
    const medicoTop = Object.entries(conteoMedicos).sort((a, b) => b[1] - a[1])[0];
    document.getElementById('m-medico-frecuente').textContent = medicoTop ? medicoTop[0] : '—';
}

function renderConsultas(consultas) {
    const cont = document.getElementById('lista-consultas');

    if (!consultas.length) {
        cont.innerHTML = `<div class="empty-row">
      <i class="bi bi-file-medical" style="font-size:24px;display:block;margin-bottom:8px;color:var(--text-3)"></i>
      Este paciente no tiene consultas registradas
    </div>`;
        return;
    }

    const ordenadas = [...consultas].sort((a, b) => new Date(b.fechaCita) - new Date(a.fechaCita));

    cont.innerHTML = ordenadas.map(c => {
        const fecha = new Date(c.fechaCita).toLocaleDateString('es-PE',
            { day: '2-digit', month: '2-digit', year: 'numeric' });
        const hora = new Date(c.fechaCita).toLocaleTimeString('es-PE',
            { hour: '2-digit', minute: '2-digit' });

        return `
      <div style="border:1px solid var(--border);border-radius:12px;padding:16px;margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:start;margin-bottom:10px">
          <div>
            <div style="font-weight:600;color:var(--text)">${c.medicoNombre}</div>
            <div style="font-size:12px;color:var(--text-3)">${fecha} — ${hora}</div>
          </div>
        </div>
        <div style="display:grid;gap:8px">
          <div>
            <div style="font-size:11px;color:var(--text-3);text-transform:uppercase;letter-spacing:.05em;margin-bottom:2px">
              Diagnóstico
            </div>
            <div style="color:var(--text);font-size:14px">${c.diagnostico || '—'}</div>
          </div>
          <div>
            <div style="font-size:11px;color:var(--text-3);text-transform:uppercase;letter-spacing:.05em;margin-bottom:2px">
              Tratamiento
            </div>
            <div style="color:var(--text);font-size:14px">${c.tratamiento || '—'}</div>
          </div>
          ${c.observaciones ? `
          <div>
            <div style="font-size:11px;color:var(--text-3);text-transform:uppercase;letter-spacing:.05em;margin-bottom:2px">
              Observaciones
            </div>
            <div style="color:var(--text-2);font-size:13px">${c.observaciones}</div>
          </div>` : ''}
        </div>
      </div>`;
    }).join('');
}