/**
 * patients.js — Gestión de pacientes
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

// ── Modales ───────────────────────────────────────────────────
function abrirModal(id) { document.getElementById(id).classList.add('open'); }
function cerrarModal(id) { document.getElementById(id).classList.remove('open'); }
document.querySelectorAll('.modal-backdrop').forEach(m =>
  m.addEventListener('click', e => { if (e.target === m) m.classList.remove('open'); }));

// ── Estado global ─────────────────────────────────────────────
let pacienteActivoId = null;
let todosPacientes = [];

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
function renderTabla(pacientes) {
  document.getElementById('total-filtrados').textContent = pacientes.length;
  const tbody = document.getElementById('tabla-pacientes');

  if (!pacientes.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-row">
      <i class="bi bi-people" style="font-size:24px;display:block;margin-bottom:8px;color:var(--text-3)"></i>
      No se encontraron pacientes
    </td></tr>`;
    return;
  }

  tbody.innerHTML = pacientes.map(p => {
    const iniciales = (p.nombres.charAt(0) + p.apellidos.charAt(0)).toUpperCase();
    const sexoIcon = p.sexo === 'M'
      ? '<span class="badge badge-confirmada">♂ Masculino</span>'
      : '<span class="badge badge-reprogramada">♀ Femenino</span>';

    return `<tr>
      <td>
        <div style="display:flex;align-items:center;gap:10px">
          <div style="width:36px;height:36px;border-radius:50%;flex-shrink:0;
                      background:linear-gradient(135deg,var(--indigo),var(--blue));
                      display:flex;align-items:center;justify-content:center;
                      font-size:13px;font-weight:700;color:white">${iniciales}</div>
          <div>
            <div style="font-weight:500;color:var(--text)">${p.nombres} ${p.apellidos}</div>
            <div style="font-size:11px;color:var(--text-3)">ID: ${p.id}</div>
          </div>
        </div>
      </td>
      <td><span style="font-family:monospace;font-size:13px;color:var(--text-2)">${p.dni}</span></td>
      <td>${p.celular}</td>
      <td style="color:var(--text-2)">${p.correo || '—'}</td>
      <td>${sexoIcon}</td>
      <td>
        <div style="display:flex;gap:6px">
          <button class="btn btn-sm btn-ghost" onclick="abrirVer(${p.id})" title="Ver detalle">
            <i class="bi bi-eye"></i>
          </button>
          <button class="btn btn-sm btn-ghost" onclick="abrirEditar(${p.id})" title="Editar">
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

    // Seguros vinculados
    const segurosEl = document.getElementById('ver-seguros');
    if (p.seguros && p.seguros.length > 0) {
      segurosEl.innerHTML = `
        <div style="max-height:200px;overflow-y:auto;display:flex;flex-direction:column;gap:8px;
                    padding-right:4px">
          ${p.seguros.map(s => `
            <div style="background:var(--bg-card);border:1px solid var(--border);
                        border-radius:10px;padding:12px">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
                <span style="font-weight:600;color:var(--text)">${s.nombre}</span>
                <div style="display:flex;align-items:center;gap:6px">
                  ${s.convenioActivo
          ? '<span class="badge badge-confirmada">Convenio activo</span>'
          : '<span class="badge badge-pendiente">Sin convenio</span>'}
                  <button class="btn btn-sm btn-red" title="Quitar seguro"
                          onclick="quitarSeguro(${pacienteActivoId}, ${s.id}, '${s.nombre}')">
                    <i class="bi bi-trash3"></i>
                  </button>
                </div>
              </div>
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px;font-size:12px">
                <div style="color:var(--text-3)">Tipo: <span style="color:var(--text)">${s.tipo}</span></div>
                <div style="color:var(--text-3)">Cobertura:
                  <span style="color:var(--green);font-weight:600">${s.porcentajeCobertura}%</span>
                </div>
                ${s.deducible ? `<div style="color:var(--text-3)">Deducible:
                  <span style="color:var(--text)">S/ ${s.deducible}</span></div>` : ''}
                ${s.numeroPoliza ? `<div style="color:var(--text-3)">Póliza:
                  <span style="color:var(--text)">${s.numeroPoliza}</span></div>` : ''}
              </div>
              <div style="margin-top:6px;font-size:11px;color:var(--green)">
                <i class="bi bi-info-circle"></i>
                Descuento automático de ${s.porcentajeCobertura}% al pagar una cita
              </div>
            </div>
          `).join('')}
        </div>`;
    } else {
      segurosEl.innerHTML = `
        <div style="color:var(--text-3);font-size:13px;padding:10px;
                    background:var(--bg-main);border-radius:10px;text-align:center">
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
    // Limpiar formulario
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
    // Re-abrir el modal ver con datos actualizados
    await abrirVer(pacienteActivoId);
  } catch (err) { UI.mostrarError(err); }
}

// ── Quitar seguro ─────────────────────────────────────────────
async function quitarSeguro(pacienteId, vinculoId, nombreSeguro) {
  if (!confirm(`¿Quitar el seguro "${nombreSeguro}" de este paciente?`)) return;
  try {
    await SeguroService.desvincular(pacienteId, vinculoId);
    UI.mostrarAlerta('Seguro quitado correctamente');
    await abrirVer(pacienteId); // recargar modal con datos actualizados
  } catch (err) { UI.mostrarError(err); }
}

cargarContadores();