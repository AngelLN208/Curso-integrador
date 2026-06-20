/**
 * appointments.js — Gestión de citas médicas
 */

AuthService.requireAuth();
const usuario = Auth.getUsuario();
iniciarSidebar('Citas');

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

// ── Modales ───────────────────────────────────────────────────
function abrirModal(id)  { document.getElementById(id).classList.add('open'); }
function cerrarModal(id) { document.getElementById(id).classList.remove('open'); }
document.querySelectorAll('.modal-backdrop').forEach(m =>
  m.addEventListener('click', e => { if (e.target === m) m.classList.remove('open'); }));

// ── Estado global ─────────────────────────────────────────────
let citaActivaId   = null;
let todasLasCitas  = [];
let diasLaborables = []; // días en que trabaja el médico seleccionado
let slotSeleccionado = null;

// ── Cargar datos iniciales ────────────────────────────────────
async function cargarContadores() {
  try {
    const citas = await CitaService.listar();
    todasLasCitas = citas;
    const hoy = new Date().toISOString().split('T')[0];
    document.getElementById('total-hoy').textContent        = citas.filter(c => c.fechaHora.startsWith(hoy)).length;
    document.getElementById('total-confirmadas').textContent = citas.filter(c => c.estado === 'CONFIRMADA').length;
    document.getElementById('total-pendientes').textContent  = citas.filter(c => c.estado === 'PENDIENTE').length;
    document.getElementById('total-canceladas').textContent  = citas.filter(c => c.estado === 'CANCELADA').length;
    renderTabla(citas);
  } catch (err) { UI.mostrarError(err); }
}

// ── Tabla ─────────────────────────────────────────────────────
function renderTabla(citas) {
  const tbody = document.getElementById('tabla-citas');
  document.getElementById('total-mostradas').textContent = `${citas.length} citas`;

  if (!citas.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-row">
      <i class="bi bi-calendar-x" style="font-size:24px;display:block;margin-bottom:8px;color:var(--text-3)"></i>
      No se encontraron citas
    </td></tr>`;
    return;
  }

  tbody.innerHTML = citas.map(c => {
    const fecha = c.fechaHora.split('T')[0];
    const hora  = c.fechaHora.split('T')[1].substring(0, 5);
    return `<tr>
      <td><strong>${UI.formatFecha(fecha)}</strong></td>
      <td><span style="background:var(--indigo-lt);color:var(--indigo);padding:3px 8px;border-radius:6px;font-weight:600;font-size:13px">${hora}</span></td>
      <td>${c.pacienteNombre}</td>
      <td style="color:var(--text-2)">${c.medicoNombre}</td>
      <td style="color:var(--text-2)">${c.especialidad}</td>
      <td>${UI.badgeEstado(c.estado)}</td>
      <td>
        <div style="display:flex;gap:6px">
          <button class="btn btn-sm btn-ghost" onclick="abrirVerCita(${c.id})" title="Ver">
            <i class="bi bi-eye"></i>
          </button>
          <button class="btn btn-sm btn-ghost" onclick="abrirReprogramar(${c.id})" title="Reprogramar">
            <i class="bi bi-pencil"></i>
          </button>
          <button class="btn btn-sm btn-danger-ghost"
            onclick="abrirCancelar(${c.id},'${c.pacienteNombre}','${UI.formatFecha(fecha)}','${hora}','${c.especialidad}')"
            title="Cancelar">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
      </td>
    </tr>`;
  }).join('');
}

// ── Filtros ───────────────────────────────────────────────────
function aplicarFiltros() {
  const paciente = document.getElementById('filtro-paciente').value.toLowerCase().trim();
  const fecha    = document.getElementById('filtro-fecha').value;
  const esp      = document.getElementById('filtro-especialidad').value.toLowerCase();
  const estado   = document.getElementById('filtro-estado').value;

  renderTabla(todasLasCitas.filter(c => {
    return (!paciente || c.pacienteNombre.toLowerCase().includes(paciente))
        && (!fecha    || c.fechaHora.startsWith(fecha))
        && (!esp      || c.especialidad.toLowerCase().includes(esp))
        && (!estado   || c.estado === estado);
  }));
}

function limpiarFiltros() {
  ['filtro-paciente','filtro-fecha','filtro-especialidad','filtro-estado']
    .forEach(id => document.getElementById(id).value = '');
  renderTabla(todasLasCitas);
}

['filtro-paciente','filtro-fecha'].forEach(id =>
  document.getElementById(id).addEventListener('input', aplicarFiltros));
['filtro-especialidad','filtro-estado'].forEach(id =>
  document.getElementById(id).addEventListener('change', aplicarFiltros));

// ── Ver cita ──────────────────────────────────────────────────
async function abrirVerCita(id) {
  try {
    citaActivaId = id;
    const c = await CitaService.getById(id);
    const fecha = c.fechaHora.split('T')[0];
    const hora  = c.fechaHora.split('T')[1].substring(0, 5);
    document.getElementById('ver-paciente').textContent     = c.pacienteNombre;
    document.getElementById('ver-medico').textContent       = c.medicoNombre;
    document.getElementById('ver-especialidad').textContent = c.especialidad;
    document.getElementById('ver-fechaHora').textContent    = `${UI.formatFecha(fecha)} ${hora}`;
    document.getElementById('ver-estado').innerHTML         = UI.badgeEstado(c.estado);
    document.getElementById('ver-motivo').textContent       = c.motivo || '—';
    abrirModal('modalVerCita');
  } catch (err) { UI.mostrarError(err); }
}

// ── Reprogramar ───────────────────────────────────────────────
function abrirReprogramar(id) {
  citaActivaId = id;
  document.getElementById('reprog-fechaHora').value = '';
  abrirModal('modalReprogramar');
}

async function guardarReprogramacion() {
  const nuevaFechaHora = document.getElementById('reprog-fechaHora').value;
  if (!nuevaFechaHora) return UI.mostrarError({ message: 'Selecciona una fecha y hora' });
  try {
    await CitaService.reprogramar(citaActivaId, { nuevaFechaHora });
    cerrarModal('modalReprogramar');
    UI.mostrarAlerta('Cita reprogramada correctamente');
    cargarContadores();
  } catch (err) { UI.mostrarError(err); }
}

// ── Cancelar ──────────────────────────────────────────────────
function abrirCancelar(id, nombre, fecha, hora, esp) {
  citaActivaId = id;
  document.getElementById('cancelar-nombre').textContent  = nombre;
  document.getElementById('cancelar-detalle').textContent = `${fecha} — ${hora} — ${esp}`;
  document.getElementById('cancelar-motivo').value        = '';
  abrirModal('modalCancelar');
}

async function confirmarCancelacion() {
  try {
    await CitaService.cancelar(citaActivaId,
      document.getElementById('cancelar-motivo').value);
    cerrarModal('modalCancelar');
    UI.mostrarAlerta('Cita cancelada', 'warning');
    cargarContadores();
  } catch (err) { UI.mostrarError(err); }
}

// ── Nueva cita — flujo con slots ──────────────────────────────
function abrirNuevaCita() {
  limpiarFormularioNuevaCita();
  abrirModal('modalNuevaCita');
}

function limpiarFormularioNuevaCita() {
  document.getElementById('nueva-buscar-paciente').value = '';
  document.getElementById('cita-pacienteId').innerHTML   = '<option value="">Seleccionar paciente...</option>';
  document.getElementById('nueva-especialidad').value    = '';
  document.getElementById('cita-medicoId').innerHTML     = '<option value="">Selecciona especialidad primero</option>';
  document.getElementById('cita-fecha').value            = '';
  document.getElementById('cita-fecha').disabled         = true;
  document.getElementById('cita-fechaHora').value        = '';
  document.getElementById('cita-motivo').value           = '';
  document.getElementById('slots-container').style.display = 'none';
  document.getElementById('slots-grid').innerHTML        = '';
  document.getElementById('fecha-hint').textContent      = 'Selecciona un médico para ver los días disponibles';
  slotSeleccionado  = null;
  diasLaborables    = [];
}

// Cargar especialidades
async function cargarEspecialidades() {
  try {
    const res = await apiFetch('/especialidades');
    const especialidades = res.data ?? res;
    const sel1 = document.getElementById('nueva-especialidad');
    const sel2 = document.getElementById('filtro-especialidad');
    especialidades.forEach(e => {
      sel1.insertAdjacentHTML('beforeend', `<option value="${e.id}">${e.nombre}</option>`);
      sel2.insertAdjacentHTML('beforeend', `<option value="${e.nombre}">${e.nombre}</option>`);
    });
  } catch (err) { console.error('Error cargando especialidades:', err); }
}

// Al cambiar especialidad → cargar médicos
document.getElementById('nueva-especialidad').addEventListener('change', async (e) => {
  const id  = e.target.value;
  const sel = document.getElementById('cita-medicoId');
  // resetear fecha y slots
  document.getElementById('cita-fecha').value   = '';
  document.getElementById('cita-fecha').disabled = true;
  document.getElementById('slots-container').style.display = 'none';
  document.getElementById('fecha-hint').textContent = 'Selecciona un médico para ver los días disponibles';
  slotSeleccionado = null;

  if (!id) { sel.innerHTML = '<option value="">Selecciona especialidad primero</option>'; return; }
  sel.innerHTML = '<option value="">Cargando médicos...</option>';
  try {
    const medicos = await MedicoService.listarPorEspecialidad(id);
    sel.innerHTML = '<option value="">Seleccionar médico...</option>';
    medicos.forEach(m => sel.insertAdjacentHTML('beforeend',
      `<option value="${m.id}">Dr(a). ${m.nombres} ${m.apellidos}</option>`));
  } catch (err) { UI.mostrarError(err); }
});

// Al cambiar médico → obtener días laborables y habilitar fecha
document.getElementById('cita-medicoId').addEventListener('change', async (e) => {
  const medicoId = e.target.value;
  const inputFecha = document.getElementById('cita-fecha');
  document.getElementById('slots-container').style.display = 'none';
  document.getElementById('slots-grid').innerHTML = '';
  slotSeleccionado = null;

  if (!medicoId) {
    inputFecha.disabled = true;
    document.getElementById('fecha-hint').textContent = 'Selecciona un médico primero';
    return;
  }

  try {
    const res = await apiFetch(`/disponibilidad/medico/${medicoId}/dias`);
    diasLaborables = res.data ?? res;

    if (!diasLaborables.length) {
      inputFecha.disabled = true;
      document.getElementById('fecha-hint').textContent =
        '⚠️ Este médico no tiene horarios asignados';
      return;
    }

    // Mapear nombres de días a números (0=Dom, 1=Lun, ...)
    const mapDias = {
      SUNDAY:0, MONDAY:1, TUESDAY:2, WEDNESDAY:3,
      THURSDAY:4, FRIDAY:5, SATURDAY:6
    };
    const numerosDisponibles = diasLaborables.map(d => mapDias[d]);

    inputFecha.disabled = false;

    // Fecha mínima = hoy
    inputFecha.min = new Date().toISOString().split('T')[0];

    // Hint con días disponibles
    const nombresEs = { MONDAY:'Lun', TUESDAY:'Mar', WEDNESDAY:'Mié',
                        THURSDAY:'Jue', FRIDAY:'Vie', SATURDAY:'Sáb', SUNDAY:'Dom' };
    const diasTexto = diasLaborables.map(d => nombresEs[d] || d).join(', ');
    document.getElementById('fecha-hint').textContent =
      `✓ Días disponibles: ${diasTexto}`;

    // Validar que la fecha seleccionada sea un día laborable
    inputFecha.addEventListener('change', async (ev) => {
      const fechaSeleccionada = new Date(ev.target.value + 'T12:00:00');
      const diaSemana = fechaSeleccionada.getDay(); // 0=Dom

      if (!numerosDisponibles.includes(diaSemana)) {
        UI.mostrarAlerta({
          message: `El Dr/Dra. no trabaja ese día. Días disponibles: ${diasTexto}`
        }, 'warning');
        inputFecha.value = '';
        document.getElementById('slots-container').style.display = 'none';
        return;
      }
      await cargarSlots(medicoId, ev.target.value);
    }, { once: false });

  } catch (err) { UI.mostrarError(err); }
});

// Cargar slots de horario disponibles
async function cargarSlots(medicoId, fecha) {
  const container = document.getElementById('slots-container');
  const grid      = document.getElementById('slots-grid');

  container.style.display = 'block';
  grid.innerHTML = '<span style="color:var(--text-3);font-size:13px">Cargando horarios...</span>';
  slotSeleccionado = null;
  document.getElementById('cita-fechaHora').value = '';

  try {
    const res = await apiFetch(`/disponibilidad/medico/${medicoId}/slots?fecha=${fecha}`);
    const slots = res.data ?? res;

    if (!slots.length) {
      grid.innerHTML = `
        <div style="width:100%;padding:16px;background:var(--amber-lt);
                    border-radius:10px;color:var(--amber);font-size:13px;
                    display:flex;align-items:center;gap:8px">
          <i class="bi bi-calendar-x" style="font-size:18px"></i>
          No hay horarios disponibles para esta fecha.
          Prueba con otro día o médico.
        </div>`;
      return;
    }

    grid.innerHTML = slots.map(slot => `
      <button type="button"
              class="slot-btn"
              data-fechahora="${slot.fechaHora}"
              onclick="seleccionarSlot(this, '${slot.fechaHora}')"
              style="padding:8px 14px;border:1.5px solid var(--border);
                     border-radius:10px;background:var(--bg-card);
                     color:var(--text);font-size:13px;font-weight:500;
                     cursor:pointer;transition:all .15s;font-family:'Inter',sans-serif">
        <i class="bi bi-clock" style="color:var(--indigo);margin-right:4px"></i>
        ${slot.hora}
      </button>
    `).join('');

  } catch (err) {
    grid.innerHTML = `<span style="color:var(--red);font-size:13px">Error al cargar horarios</span>`;
    UI.mostrarError(err);
  }
}

// Seleccionar un slot de tiempo
function seleccionarSlot(btn, fechaHora) {
  // Deseleccionar todos
  document.querySelectorAll('.slot-btn').forEach(b => {
    b.style.background    = 'var(--bg-card)';
    b.style.borderColor   = 'var(--border)';
    b.style.color         = 'var(--text)';
  });
  // Seleccionar el clickeado
  btn.style.background  = 'var(--indigo)';
  btn.style.borderColor = 'var(--indigo)';
  btn.style.color       = 'white';

  slotSeleccionado = fechaHora;
  document.getElementById('cita-fechaHora').value = fechaHora;
}

// Buscar paciente
let buscarTimeout;
document.getElementById('nueva-buscar-paciente').addEventListener('input', (e) => {
  clearTimeout(buscarTimeout);
  buscarTimeout = setTimeout(async () => {
    const criterio = e.target.value.trim();
    if (!criterio) return;
    try {
      const pacientes = await PacienteService.buscar(criterio);
      const sel = document.getElementById('cita-pacienteId');
      sel.innerHTML = '<option value="">Seleccionar paciente...</option>';
      pacientes.forEach(p => sel.insertAdjacentHTML('beforeend',
        `<option value="${p.id}">${p.nombres} ${p.apellidos} — DNI: ${p.dni}</option>`));
    } catch (err) { console.error(err); }
  }, 400);
});

// Guardar nueva cita
async function guardarNuevaCita() {
  const pacienteId = document.getElementById('cita-pacienteId').value;
  const medicoId   = document.getElementById('cita-medicoId').value;
  const fechaHora  = document.getElementById('cita-fechaHora').value;
  const motivo     = document.getElementById('cita-motivo').value;

  if (!pacienteId) return UI.mostrarError({ message: 'Selecciona un paciente' });
  if (!medicoId)   return UI.mostrarError({ message: 'Selecciona un médico' });
  if (!fechaHora)  return UI.mostrarError({ message: 'Selecciona un horario disponible' });

  try {
    await CitaService.crear({
      pacienteId: Number(pacienteId),
      medicoId:   Number(medicoId),
      fechaHora, motivo
    });
    cerrarModal('modalNuevaCita');
    limpiarFormularioNuevaCita();
    UI.mostrarAlerta('Cita registrada correctamente');
    cargarContadores();
  } catch (err) { UI.mostrarError(err); }
}

// ── Init ──────────────────────────────────────────────────────
cargarEspecialidades();
cargarContadores();