/**
 * historial-medico.js (médico) — Historial médico consolidado por paciente
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

// ── Paginación ────────────────────────────────────────────────
const pagerHistorial = new Paginador({
  contenedorId: 'pager-historial',
  porPagina: 10,
  onRenderPagina: (itemsPagina) => pintarConsultas(itemsPagina)
});

// ── Buscar paciente ───────────────────────────────────────────
let buscarTimeout;

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
    cont.innerHTML = `<div class="px-3 py-2.5 text-neblina text-[13px]">Sin resultados</div>`;
    cont.classList.remove('hidden');
    return;
  }

  cont.innerHTML = pacientes.map(p => `
    <div onclick="seleccionarPaciente(${p.id})"
         class="px-3 py-2.5 rounded-lg cursor-pointer bg-lienzo dark:bg-tinta-dark mb-1 hover:bg-guia/10 dark:hover:bg-guia/10 transition-colors">
      <span class="font-medium text-tinta dark:text-white">${p.nombres} ${p.apellidos}</span>
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
    const consultas = await AtencionService.historial(pacienteId);

    calcularMetricas(consultas);
    renderConsultas(consultas);

  } catch (err) {
    cont.innerHTML = `<div class="text-center text-neblina py-10 text-[13px]">No se pudo cargar el historial médico</div>`;
  }
}

// calcularMetricas siempre trabaja con el dataset COMPLETO (no paginado),
// ya que el total, la última consulta y el médico frecuente deben
// reflejar todo el historial, no solo la página visible.
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

// renderConsultas ordena (más recientes primero) y entrega el dataset al paginador.
function renderConsultas(consultas) {
  if (!consultas.length) {
    pagerHistorial.setDatos([]);
    return;
  }

  const ordenadas = [...consultas].sort((a, b) => new Date(b.fechaCita) - new Date(a.fechaCita));
  pagerHistorial.setDatos(ordenadas);
}

// pintarConsultas recibe SOLO los items de la página actual (ya ordenados) y los dibuja.
function pintarConsultas(consultas) {
  const cont = document.getElementById('lista-consultas');

  if (!consultas.length) {
    cont.innerHTML = `<div class="text-center text-neblina py-10 text-[13px]">
      <i class="bi bi-file-medical block text-2xl mb-2"></i>
      Este paciente no tiene consultas registradas
    </div>`;
    return;
  }

  cont.innerHTML = consultas.map(c => {
    const fecha = new Date(c.fechaCita).toLocaleDateString('es-PE',
      { day: '2-digit', month: '2-digit', year: 'numeric' });
    const hora = new Date(c.fechaCita).toLocaleTimeString('es-PE',
      { hour: '2-digit', minute: '2-digit' });

    const tieneTriaje = c.presionArterial || c.temperatura || c.peso;

    return `
      <div class="border border-borde dark:border-borde-dark rounded-lg p-4 mb-3 last:mb-0">
        <div class="flex items-start justify-between mb-2.5">
          <div>
            <div class="font-medium text-tinta dark:text-white">${c.medicoNombre}</div>
            <div class="text-xs text-neblina">${fecha} — ${hora}</div>
          </div>
        </div>

        ${tieneTriaje ? `
        <div class="grid grid-cols-3 gap-2 mb-3">
          <div class="bg-lienzo dark:bg-tinta-dark rounded-lg px-2.5 py-2">
            <div class="text-[10px] text-neblina">Presión</div>
            <div class="text-[13px] font-semibold text-tinta dark:text-white">${c.presionArterial || '—'}</div>
          </div>
          <div class="bg-lienzo dark:bg-tinta-dark rounded-lg px-2.5 py-2">
            <div class="text-[10px] text-neblina">Temperatura</div>
            <div class="text-[13px] font-semibold text-tinta dark:text-white">${c.temperatura ? c.temperatura + ' °C' : '—'}</div>
          </div>
          <div class="bg-lienzo dark:bg-tinta-dark rounded-lg px-2.5 py-2">
            <div class="text-[10px] text-neblina">Peso</div>
            <div class="text-[13px] font-semibold text-tinta dark:text-white">${c.peso ? c.peso + ' kg' : '—'}</div>
          </div>
        </div>` : ''}

        <div class="grid gap-2">
          <div>
            <div class="text-[11px] text-neblina uppercase tracking-wider mb-0.5">Diagnóstico</div>
            <div class="text-tinta dark:text-white text-[14px]">${c.diagnostico || '—'}</div>
          </div>
          <div>
            <div class="text-[11px] text-neblina uppercase tracking-wider mb-0.5">Tratamiento</div>
            <div class="text-tinta dark:text-white text-[14px]">${c.tratamiento || '—'}</div>
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