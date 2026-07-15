/**
 * patients.js — Gestión de pacientes (recepcionista)
 */

AuthService.requireAuth();
const usuario = Auth.getUsuario();
iniciarSidebar('Pacientes');

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

// ── Modales (Tailwind: toggle hidden/flex) ─────────────────────
function abrirModal(id) {
  const m = document.getElementById(id);
  m.classList.remove('hidden');
  m.classList.add('flex');
}
function cerrarModal(id) {
  const m = document.getElementById(id);
  m.classList.add('hidden');
  m.classList.remove('flex');
}
document.querySelectorAll('[id^="modal"]').forEach(m =>
  m.addEventListener('click', e => { if (e.target === m) cerrarModal(m.id); }));

// ── Estado global ─────────────────────────────────────────────
let pacienteActivoId = null;
let todosPacientes = [];

// ── Paginación ────────────────────────────────────────────────
const pagerPacientes = new Paginador({
  contenedorId: 'pager-pacientes',
  porPagina: 10,
  onRenderPagina: (itemsPagina) => pintarTabla(itemsPagina)
});

// ── Contadores ────────────────────────────────────────────────
async function cargarContadores() {
  try {
    const todos = await PacienteService.listar();
    todosPacientes = todos;

    const hoy = new Date();
    const nuevos = todos.filter(p => {
      const c = new Date(p.creadoEn);
      return c.getMonth() === hoy.getMonth() && c.getFullYear() === hoy.getFullYear();
    });

    document.getElementById('total-pacientes').textContent = todos.length;
    document.getElementById('nuevos-mes').textContent = nuevos.length;
    renderTabla(todos);
  } catch (err) { UI.mostrarError(err); }
}

// ── Tabla ─────────────────────────────────────────────────────
// renderTabla ordena y entrega el dataset completo al paginador.
function renderTabla(pacientes) {
  document.getElementById('total-filtrados').textContent = pacientes.length;
  const ordenados = [...pacientes].sort((a, b) =>
    `${a.nombres} ${a.apellidos}`.localeCompare(`${b.nombres} ${b.apellidos}`));
  pagerPacientes.setDatos(ordenados);
}

// pintarTabla recibe SOLO los items de la página actual (ya ordenados) y los dibuja.
function pintarTabla(pacientes) {
  const tbody = document.getElementById('tabla-pacientes');

  if (!pacientes.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-10 text-[13px]">
      <i class="bi bi-people text-2xl block mb-2 text-neblina"></i>
      <span class="text-neblina">No se encontraron pacientes</span>
    </td></tr>`;
    return;
  }

  tbody.innerHTML = pacientes.map(p => {
    const iniciales = (p.nombres.charAt(0) + p.apellidos.charAt(0)).toUpperCase();
    const sexoIcon = p.sexo === 'M'
      ? '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rumbo/10 text-rumbo">♂ Masculino</span>'
      : '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-500/10 text-blue-600 dark:text-blue-400">♀ Femenino</span>';

    return `<tr class="hover:bg-lienzo dark:hover:bg-tinta-dark transition-colors">
      <td class="px-4 py-3">
        <div class="flex items-center gap-2.5">
          <div class="w-9 h-9 rounded-full flex-shrink-0 bg-guia flex items-center justify-center text-[13px] font-display font-bold text-tinta">${iniciales}</div>
          <div class="min-w-0">
            <div class="font-medium truncate">${p.nombres} ${p.apellidos}</div>
            <div class="text-[11px] text-neblina">ID: ${p.id}</div>
          </div>
        </div>
      </td>
      <td class="px-4 py-3"><span class="font-mono text-xs text-neblina">${p.dni}</span></td>
      <td class="px-4 py-3">${p.celular}</td>
      <td class="px-4 py-3 text-neblina">${p.correo || '—'}</td>
      <td class="px-4 py-3">${sexoIcon}</td>
      <td class="px-4 py-3">
        <div class="flex gap-1.5">
          <button onclick="abrirVer(${p.id})" title="Ver detalle"
            class="p-2 rounded-lg text-neblina hover:text-guia hover:bg-guia/10 transition-colors">
            <i class="bi bi-eye"></i>
          </button>
          <button onclick="abrirEditar(${p.id})" title="Editar"
            class="p-2 rounded-lg text-neblina hover:text-blue-500 hover:bg-blue-500/10 transition-colors">
            <i class="bi bi-pencil"></i>
          </button>
        </div>
      </td>
    </tr>`;
  }).join('');
}

// ── Buscador ──────────────────────────────────────────────────
let buscarTimeout;
document.getElementById('buscar-input').addEventListener('input', (e) => {
  clearTimeout(buscarTimeout);
  buscarTimeout = setTimeout(async () => {
    const criterio = e.target.value.trim();
    try {
      const resultado = criterio
        ? await PacienteService.buscar(criterio)
        : todosPacientes;
      renderTabla(resultado);
    } catch (err) { UI.mostrarError(err); }
  }, 350);
});

// ── Ver paciente ──────────────────────────────────────────────
async function abrirVer(id) {
  try {
    pacienteActivoId = id;
    const p = await PacienteService.getById(id);
    const iniciales = (p.nombres.charAt(0) + p.apellidos.charAt(0)).toUpperCase();

    document.getElementById('ver-iniciales').textContent = iniciales;
    document.getElementById('ver-nombre').textContent = `${p.nombres} ${p.apellidos}`;
    document.getElementById('ver-dni').textContent = `DNI: ${p.dni}`;
    document.getElementById('ver-fechaNacimiento').textContent = UI.formatFecha(p.fechaNacimiento);
    document.getElementById('ver-celular').textContent = p.celular;
    document.getElementById('ver-correo').textContent = p.correo || '—';
    document.getElementById('ver-sexo').textContent = p.sexo === 'M' ? 'Masculino' : 'Femenino';

    const segurosEl = document.getElementById('ver-seguros');
    if (p.seguros && p.seguros.length > 0) {
      segurosEl.innerHTML = `
        <div class="max-h-[200px] overflow-y-auto flex flex-col gap-2 pr-1">
          ${p.seguros.map(s => `
            <div class="bg-white dark:bg-superficie-dark border border-borde dark:border-borde-dark rounded-lg p-3">
              <div class="flex justify-between items-center mb-1.5">
                <span class="font-semibold">${s.nombre}</span>
                <div class="flex items-center gap-1.5">
                  ${s.convenioActivo
          ? '<span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-rumbo/10 text-rumbo">Convenio activo</span>'
          : '<span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-guia/10 text-guia">Sin convenio</span>'}
                  <button title="Quitar seguro" onclick="quitarSeguro(${pacienteActivoId}, ${s.id}, '${s.nombre}')"
                    class="p-1.5 rounded-lg bg-alerta text-white hover:opacity-90 transition-opacity">
                    <i class="bi bi-trash3 text-xs"></i>
                  </button>
                </div>
              </div>
              <div class="grid grid-cols-2 gap-1.5 text-xs">
                <div class="text-neblina">Tipo: <span class="text-tinta dark:text-white">${s.tipo}</span></div>
                <div class="text-neblina">Cobertura: <span class="text-rumbo font-semibold">${s.porcentajeCobertura}%</span></div>
                ${s.deducible ? `<div class="text-neblina">Deducible: <span class="text-tinta dark:text-white">S/ ${s.deducible}</span></div>` : ''}
                ${s.numeroPoliza ? `<div class="text-neblina">Póliza: <span class="text-tinta dark:text-white">${s.numeroPoliza}</span></div>` : ''}
              </div>
              <div class="mt-1.5 text-[11px] text-rumbo">
                <i class="bi bi-info-circle"></i> Descuento automático de ${s.porcentajeCobertura}% al pagar una cita
              </div>
            </div>
          `).join('')}
        </div>`;
    } else {
      segurosEl.innerHTML = `
        <div class="text-neblina text-[13px] p-3 bg-lienzo dark:bg-tinta-dark rounded-lg text-center">
          <i class="bi bi-shield-x"></i> Sin seguro médico vinculado
        </div>`;
    }

    abrirModal('modalVer');
  } catch (err) { UI.mostrarError(err); }
}

// ── Editar paciente ───────────────────────────────────────────
async function abrirEditar(id) {
  try {
    pacienteActivoId = id;
    const p = await PacienteService.getById(id);
    document.getElementById('edit-pac-nombres').value = p.nombres;
    document.getElementById('edit-pac-apellidos').value = p.apellidos;
    document.getElementById('edit-pac-dni').value = p.dni;
    document.getElementById('edit-pac-fechaNacimiento').value = p.fechaNacimiento;
    document.getElementById('edit-pac-celular').value = p.celular;
    document.getElementById('edit-pac-correo').value = p.correo || '';
    document.getElementById('edit-pac-sexo').value = p.sexo;
    abrirModal('modalEditar');
  } catch (err) { UI.mostrarError(err); }
}

async function guardarEdicion() {
  const nombres = document.getElementById('edit-pac-nombres').value.trim();
  const apellidos = document.getElementById('edit-pac-apellidos').value.trim();
  const celular = document.getElementById('edit-pac-celular').value.trim();

  if (!nombres || !apellidos || !celular)
    return UI.mostrarError({ message: 'Nombres, apellidos y celular son obligatorios' });

  try {
    await PacienteService.actualizar(pacienteActivoId, {
      dni: document.getElementById('edit-pac-dni').value,
      nombres, apellidos,
      fechaNacimiento: document.getElementById('edit-pac-fechaNacimiento').value,
      celular,
      correo: document.getElementById('edit-pac-correo').value,
      sexo: document.getElementById('edit-pac-sexo').value
    });
    cerrarModal('modalEditar');
    UI.mostrarAlerta('Paciente actualizado correctamente');
    cargarContadores();
  } catch (err) { UI.mostrarError(err); }
}

// ── Registrar paciente ────────────────────────────────────────
async function guardarPaciente() {
  const dni = document.getElementById('pac-dni').value.trim();
  const nombres = document.getElementById('pac-nombres').value.trim();
  const apellidos = document.getElementById('pac-apellidos').value.trim();
  const celular = document.getElementById('pac-celular').value.trim();
  const sexo = document.getElementById('pac-sexo').value;

  if (!dni || dni.length !== 8 || !/^\d{8}$/.test(dni))
    return UI.mostrarError({ message: 'El DNI debe tener exactamente 8 dígitos numéricos' });
  if (!nombres)
    return UI.mostrarError({ message: 'Los nombres son obligatorios' });
  if (!apellidos)
    return UI.mostrarError({ message: 'Los apellidos son obligatorios' });
  if (!celular || celular.length !== 9)
    return UI.mostrarError({ message: 'El celular debe tener 9 dígitos' });
  if (!sexo)
    return UI.mostrarError({ message: 'Selecciona el sexo' });

  try {
    await PacienteService.crear({
      dni, nombres, apellidos, celular, sexo,
      fechaNacimiento: document.getElementById('pac-fechaNacimiento').value,
      correo: document.getElementById('pac-correo').value
    });
    cerrarModal('modalRegistrar');
    ['pac-dni', 'pac-nombres', 'pac-apellidos', 'pac-celular', 'pac-correo', 'pac-fechaNacimiento']
      .forEach(id => document.getElementById(id).value = '');
    document.getElementById('pac-sexo').value = '';
    UI.mostrarAlerta('Paciente registrado correctamente');
    cargarContadores();
  } catch (err) { UI.mostrarError(err); }
}

// ── Vincular seguro ───────────────────────────────────────────
async function abrirVincularSeguro() {
  cerrarModal('modalVer');
  document.getElementById('vseg-nombre-paciente').textContent =
    document.getElementById('ver-nombre').textContent;
  document.getElementById('vseg-poliza').value = '';

  const sel = document.getElementById('vseg-seguroId');
  sel.innerHTML = '<option value="">Cargando...</option>';
  try {
    const seguros = await SeguroService.listar();
    const segurosActivos = seguros.filter(s => s.convenioActivo);
    sel.innerHTML = '<option value="">Seleccionar seguro...</option>';
    segurosActivos.forEach(s => sel.insertAdjacentHTML('beforeend',
      `<option value="${s.id}">${s.nombre} — ${s.tipo} (${s.porcentajeCobertura}% cobertura)</option>`));
  } catch (err) {
    sel.innerHTML = '<option value="">Error al cargar seguros</option>';
    UI.mostrarError(err);
  }
  abrirModal('modalVincularSeguro');
}

async function confirmarVincularSeguro() {
  const seguroId = document.getElementById('vseg-seguroId').value;
  const numeroPoliza = document.getElementById('vseg-poliza').value.trim();
  if (!seguroId) return UI.mostrarError({ message: 'Selecciona un seguro' });
  try {
    await SeguroService.vincular(pacienteActivoId, seguroId, numeroPoliza || null);
    cerrarModal('modalVincularSeguro');
    UI.mostrarAlerta('Seguro vinculado correctamente');
    await abrirVer(pacienteActivoId);
  } catch (err) { UI.mostrarError(err); }
}

// ── Quitar seguro ─────────────────────────────────────────────
async function quitarSeguro(pacienteId, vinculoId, nombreSeguro) {
  if (!confirm(`¿Quitar el seguro "${nombreSeguro}" de este paciente?`)) return;
  try {
    await SeguroService.desvincular(pacienteId, vinculoId);
    UI.mostrarAlerta('Seguro quitado correctamente');
    await abrirVer(pacienteId);
  } catch (err) { UI.mostrarError(err); }
}

cargarContadores();