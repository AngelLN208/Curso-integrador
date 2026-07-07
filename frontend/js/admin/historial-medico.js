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

// ── Buscar paciente ───────────────────────────────────────────
let buscarTimeout;
let todosPacientes = [];

document.getElementById('buscar-paciente').addEventListener('input', (e) => {
    clearTimeout(buscarTimeout);
    const criterio = e.target.value.trim();

    if (!criterio) {
        document.getElementById('resultados-busqueda').classList.add('hidden');
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
        cont.innerHTML = `<div class="p-2.5 text-neblina text-[13px]">Sin resultados</div>`;
        cont.classList.remove('hidden');
        return;
    }

    cont.innerHTML = pacientes.map(p => `
    <div onclick="seleccionarPaciente(${p.id})"
         class="px-3 py-2.5 rounded-lg cursor-pointer bg-lienzo dark:bg-tinta-dark hover:bg-guia/10 transition-colors mb-1 last:mb-0">
      <span class="font-medium">${p.nombres} ${p.apellidos}</span>
      <span class="text-xs text-neblina ml-2">DNI: ${p.dni}</span>
    </div>`).join('');
    cont.classList.remove('hidden');
}

async function seleccionarPaciente(id) {
    document.getElementById('resultados-busqueda').classList.add('hidden');
    document.getElementById('buscar-paciente').value = '';

    try {
        const p = await PacienteService.getById(id);
        const iniciales = (p.nombres.charAt(0) + p.apellidos.charAt(0)).toUpperCase();

        document.getElementById('paciente-iniciales').textContent = iniciales;
        document.getElementById('paciente-nombre').textContent = `${p.nombres} ${p.apellidos}`;
        document.getElementById('paciente-dni').textContent = `DNI: ${p.dni}`;
        document.getElementById('card-paciente').classList.remove('hidden');
        document.getElementById('card-vacio').classList.add('hidden');

        await cargarHistorial(id);
    } catch (err) { UI.mostrarError(err); }
}

// ── Cargar historial ───────────────────────────────────────────
async function cargarHistorial(pacienteId) {
    const cont = document.getElementById('lista-consultas');
    document.getElementById('card-historial').classList.remove('hidden');
    document.getElementById('metricas-historial').classList.remove('hidden');
    document.getElementById('metricas-historial').classList.add('grid');
    cont.innerHTML = `<div class="text-center text-neblina py-10 text-[13px]">Cargando...</div>`;

    try {
        const json = await apiFetch(`/atencion/historial/${pacienteId}`);
        const consultas = json.data ?? json;

        calcularMetricas(consultas);
        renderConsultas(consultas);

    } catch (err) {
        cont.innerHTML = `<div class="text-center text-neblina py-10 text-[13px]">No se pudo cargar el historial médico</div>`;
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
        cont.innerHTML = `<div class="text-center py-8">
      <i class="bi bi-file-medical text-2xl block mb-2 text-neblina"></i>
      <span class="text-neblina text-[13px]">Este paciente no tiene consultas registradas</span>
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
      <div class="border border-borde dark:border-borde-dark rounded-xl p-4 mb-3 last:mb-0">
        <div class="flex justify-between items-start mb-2.5">
          <div>
            <div class="font-semibold">${c.medicoNombre}</div>
            <div class="text-xs text-neblina font-mono">${fecha} — ${hora}</div>
          </div>
        </div>
        <div class="grid gap-2">
          <div>
            <div class="text-[11px] text-neblina uppercase tracking-wider mb-0.5">Diagnóstico</div>
            <div class="text-[14px]">${c.diagnostico || '—'}</div>
          </div>
          <div>
            <div class="text-[11px] text-neblina uppercase tracking-wider mb-0.5">Tratamiento</div>
            <div class="text-[14px]">${c.tratamiento || '—'}</div>
          </div>
          ${c.observaciones ? `
          <div>
            <div class="text-[11px] text-neblina uppercase tracking-wider mb-0.5">Observaciones</div>
            <div class="text-neblina text-[13px]">${c.observaciones}</div>
          </div>` : ''}
        </div>
      </div>`;
    }).join('');
}