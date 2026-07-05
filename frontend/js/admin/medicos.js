/**
 * medicos.js — Gestión de médicos (admin)
 */

AuthService.requireAuth();
iniciarSidebar('Médicos');

// ── Modales ───────────────────────────────────────────────────
function abrirModal(id) { document.getElementById(id).classList.add('open'); }
function cerrarModal(id) { document.getElementById(id).classList.remove('open'); }
document.querySelectorAll('.modal-backdrop').forEach(m =>
    m.addEventListener('click', e => { if (e.target === m) m.classList.remove('open'); }));

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

// ── Estado global ─────────────────────────────────────────────
let todosLosMedicos = [];
let medicoHorarioActivoId = null;
let todasLasEspecialidades = [];
let especialidadEditandoId = null;

const DIAS_LABEL_ADMIN = {
    MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles',
    THURSDAY: 'Jueves', FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo'
};

// ── Cargar médicos ──────────────────────────────────────────────
async function cargarMedicos() {
    try {
        todosLosMedicos = await MedicoService.listarTodos();
        calcularMetricas(todosLosMedicos);
        renderTabla(todosLosMedicos);
    } catch (err) {
        UI.mostrarError(err);
    }
}

async function cargarEspecialidadesSelect() {
    const sel = document.getElementById('med-especialidad');
    try {
        const json = await apiFetch('/especialidades');
        const especialidades = json.data ?? json;
        sel.innerHTML = '<option value="">Seleccionar...</option>';
        especialidades.forEach(e => sel.insertAdjacentHTML('beforeend',
            `<option value="${e.id}">${e.nombre}</option>`));
    } catch (err) {
        sel.innerHTML = '<option value="">Error al cargar</option>';
    }
}

function calcularMetricas(medicos) {
    document.getElementById('m-total').textContent = medicos.length;
    document.getElementById('m-activos').textContent = medicos.filter(m => m.activo).length;
    const especialidadesUnicas = new Set(medicos.map(m => m.especialidadNombre));
    document.getElementById('m-especialidades').textContent = especialidadesUnicas.size;
}

function renderTabla(medicos) {
    document.getElementById('total-mostrados').textContent = `${medicos.length} médicos`;
    const tbody = document.getElementById('tabla-medicos');

    if (!medicos.length) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-row">No se encontraron médicos</td></tr>`;
        return;
    }

    tbody.innerHTML = medicos.map(m => {
        const iniciales = (m.nombres.charAt(0) + m.apellidos.charAt(0)).toUpperCase();
        return `<tr>
      <td>
        <div style="display:flex;align-items:center;gap:10px">
          <div style="width:36px;height:36px;border-radius:50%;flex-shrink:0;
                      background:linear-gradient(135deg,var(--indigo),var(--blue));
                      display:flex;align-items:center;justify-content:center;
                      font-size:13px;font-weight:700;color:white">${iniciales}</div>
          <span style="font-weight:500;color:var(--text)">${m.nombres} ${m.apellidos}</span>
        </div>
      </td>
      <td><span style="font-family:monospace;font-size:13px;color:var(--text-2)">${m.dni}</span></td>
      <td><span class="badge badge-reprogramada">${m.especialidadNombre}</span></td>
      <td>${m.celular}</td>
      <td style="color:var(--text-2)">${m.correo}</td>
      <td>
        ${m.activo
                ? '<span class="badge badge-confirmada"><i class="bi bi-check-circle"></i> Activo</span>'
                : '<span class="badge badge-cancelada"><i class="bi bi-x-circle"></i> Inactivo</span>'}
      </td>
      <td>
        <div style="display:flex;gap:6px">
          <button class="btn btn-sm btn-ghost" onclick="gestionarHorario(${m.id}, '${m.nombres} ${m.apellidos}', '${m.especialidadNombre}')" title="Horario">
            <i class="bi bi-clock"></i>
          </button>
          ${m.activo
                ? `<button class="btn btn-sm btn-red" onclick="desactivarMedico(${m.id}, '${m.nombres} ${m.apellidos}')">
                 <i class="bi bi-slash-circle"></i> Desactivar
               </button>`
                : `<button class="btn btn-sm btn-secondary" onclick="activarMedico(${m.id}, '${m.nombres} ${m.apellidos}')">
                 <i class="bi bi-check-circle"></i> Activar
               </button>`}
        </div>
      </td>
    </tr>`;
    }).join('');
}

// ── Buscador ──────────────────────────────────────────────────
let buscarTimeout;
document.getElementById('buscar-input').addEventListener('input', (e) => {
    clearTimeout(buscarTimeout);
    buscarTimeout = setTimeout(() => {
        const criterio = e.target.value.toLowerCase().trim();
        const filtrados = !criterio ? todosLosMedicos : todosLosMedicos.filter(m =>
            `${m.nombres} ${m.apellidos}`.toLowerCase().includes(criterio) ||
            m.dni.includes(criterio));
        renderTabla(filtrados);
    }, 300);
});

// ── Crear médico ────────────────────────────────────────────────
function abrirModalCrear() {
    ['med-nombres', 'med-apellidos', 'med-dni', 'med-celular', 'med-correo', 'med-username', 'med-password']
        .forEach(id => document.getElementById(id).value = '');
    document.getElementById('med-especialidad').value = '';
    cargarEspecialidadesSelect();
    abrirModal('modalCrearMedico');
}

async function guardarMedico() {
    const dni = document.getElementById('med-dni').value.trim();
    const nombres = document.getElementById('med-nombres').value.trim();
    const apellidos = document.getElementById('med-apellidos').value.trim();
    const especialidadId = document.getElementById('med-especialidad').value;
    const celular = document.getElementById('med-celular').value.trim();
    const correo = document.getElementById('med-correo').value.trim();
    const username = document.getElementById('med-username').value.trim();
    const password = document.getElementById('med-password').value;

    if (!dni || dni.length !== 8 || !/^\d{8}$/.test(dni))
        return UI.mostrarError(new Error('El DNI debe tener exactamente 8 dígitos'));
    if (!nombres || !apellidos)
        return UI.mostrarError(new Error('Nombres y apellidos son obligatorios'));
    if (!especialidadId)
        return UI.mostrarError(new Error('Selecciona una especialidad'));
    if (!celular || celular.length !== 9)
        return UI.mostrarError(new Error('El celular debe tener 9 dígitos'));
    if (!correo || !username)
        return UI.mostrarError(new Error('Correo y usuario son obligatorios'));
    if (!password || password.length < 8)
        return UI.mostrarError(new Error('La contraseña debe tener al menos 8 caracteres'));

    try {
        await AdminService.registrarMedico({
            dni, nombres, apellidos,
            especialidadId: parseInt(especialidadId),
            celular, correo, username, password
        });
        UI.mostrarAlerta('Médico registrado correctamente', 'success');
        cerrarModal('modalCrearMedico');
        cargarMedicos();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ── Activar / Desactivar médico ──────────────────────────────────
async function activarMedico(id, nombre) {
    if (!confirm(`¿Reactivar a ${nombre}? Volverá a poder recibir citas.`)) return;
    try {
        await MedicoService.activar(id);
        UI.mostrarAlerta('Médico activado correctamente', 'success');
        cargarMedicos();
    } catch (err) {
        UI.mostrarError(err);
    }
}

async function desactivarMedico(id, nombre) {
    if (!confirm(`¿Desactivar a ${nombre}? Ya no podrá recibir nuevas citas.`)) return;
    try {
        await apiFetch(`/medicos/${id}`, { method: 'DELETE' });
        UI.mostrarAlerta('Médico desactivado correctamente', 'success');
        cargarMedicos();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ── Gestión de horario ───────────────────────────────────────
async function gestionarHorario(medicoId, nombreMedico, especialidad) {
    medicoHorarioActivoId = medicoId;
    document.getElementById('horario-medico-nombre').textContent = nombreMedico;
    document.getElementById('horario-medico-especialidad').textContent = especialidad;
    document.getElementById('hor-dia').value = 'MONDAY';
    document.getElementById('hor-inicio').value = '';
    document.getElementById('hor-fin').value = '';

    abrirModal('modalHorario');
    await cargarHorarios(medicoId);
}

async function cargarHorarios(medicoId) {
    const cont = document.getElementById('lista-horarios');
    cont.innerHTML = `<div class="empty-row">Cargando...</div>`;

    try {
        const horarios = await HorarioService.listarPorMedico(medicoId);

        if (!horarios.length) {
            cont.innerHTML = `<div class="empty-row">Sin horario asignado todavía</div>`;
            return;
        }

        cont.innerHTML = horarios.map(h => `
      <div style="display:flex;justify-content:space-between;align-items:center;
                  background:var(--bg-main);border-radius:10px;padding:10px 14px;margin-bottom:6px">
        <div>
          <span style="font-weight:600;color:var(--text)">${DIAS_LABEL_ADMIN[h.dia]}</span>
          <span style="color:var(--text-2);margin-left:8px">${h.horaInicio} — ${h.horaFin}</span>
        </div>
        <button class="btn btn-sm btn-red" onclick="eliminarBloqueHorario(${h.id})">
          <i class="bi bi-trash3"></i>
        </button>
      </div>`).join('');

    } catch (err) {
        cont.innerHTML = `<div class="empty-row">No se pudo cargar el horario</div>`;
    }
}

async function agregarBloqueHorario() {
    const dia = document.getElementById('hor-dia').value;
    const horaInicio = document.getElementById('hor-inicio').value;
    const horaFin = document.getElementById('hor-fin').value;

    if (!horaInicio || !horaFin) return UI.mostrarError(new Error('Selecciona hora de inicio y fin'));
    if (horaInicio >= horaFin) return UI.mostrarError(new Error('La hora de fin debe ser posterior a la de inicio'));

    try {
        await HorarioService.asignar({
            medicoId: medicoHorarioActivoId,
            dia,
            horaInicio: horaInicio + ':00',
            horaFin: horaFin + ':00'
        });
        UI.mostrarAlerta('Bloque de horario agregado', 'success');
        document.getElementById('hor-inicio').value = '';
        document.getElementById('hor-fin').value = '';
        cargarHorarios(medicoHorarioActivoId);
    } catch (err) {
        UI.mostrarError(err);
    }
}

async function eliminarBloqueHorario(id) {
    if (!confirm('¿Eliminar este bloque de horario?')) return;
    try {
        await HorarioService.eliminar(id);
        UI.mostrarAlerta('Bloque eliminado', 'success');
        cargarHorarios(medicoHorarioActivoId);
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ── Gestión de especialidades ───────────────────────────────
async function abrirModalEspecialidades() {
    cancelarEdicionEspecialidad();
    document.getElementById('esp-buscar').value = '';
    abrirModal('modalEspecialidades');
    await cargarListaEspecialidades();
}

async function cargarListaEspecialidades() {
    const cont = document.getElementById('lista-especialidades');
    cont.innerHTML = `<div class="empty-row">Cargando...</div>`;
    try {
        const json = await apiFetch('/especialidades/todas');
        todasLasEspecialidades = json.data ?? json;
        renderListaEspecialidades(todasLasEspecialidades);
    } catch (err) {
        cont.innerHTML = `<div class="empty-row">No se pudo cargar la lista</div>`;
    }
}

function renderListaEspecialidades(especialidades) {
    const cont = document.getElementById('lista-especialidades');

    if (!especialidades.length) {
        cont.innerHTML = `<div class="empty-row">Sin especialidades encontradas</div>`;
        return;
    }

    cont.innerHTML = especialidades.map(e => `
      <div style="display:flex;justify-content:space-between;align-items:center;
                  background:var(--bg-main);border-radius:10px;padding:10px 14px;margin-bottom:6px">
        <div>
          <span style="font-weight:600;color:var(--text)">${e.nombre}</span>
          <span style="color:var(--text-2);margin-left:8px">S/ ${Number(e.costo).toFixed(2)}</span>
          ${e.activo
            ? '<span class="badge badge-confirmada" style="margin-left:8px"><i class="bi bi-check-circle"></i> Activa</span>'
            : '<span class="badge badge-cancelada" style="margin-left:8px"><i class="bi bi-x-circle"></i> Inactiva</span>'}
        </div>
        <div style="display:flex;gap:6px">
          <button class="btn btn-sm btn-ghost" onclick='abrirEdicionEspecialidad(${JSON.stringify(e)})' title="Editar">
            <i class="bi bi-pencil"></i>
          </button>
          ${e.activo
            ? `<button class="btn btn-sm btn-red" onclick="desactivarEspecialidad(${e.id}, '${e.nombre}')">
                 <i class="bi bi-slash-circle"></i> Desactivar
               </button>`
            : `<button class="btn btn-sm btn-secondary" onclick="activarEspecialidad(${e.id}, '${e.nombre}')">
                 <i class="bi bi-check-circle"></i> Activar
               </button>`}
        </div>
      </div>`).join('');
}

function filtrarEspecialidades() {
    const criterio = document.getElementById('esp-buscar').value.toLowerCase().trim();
    const filtradas = !criterio ? todasLasEspecialidades : todasLasEspecialidades.filter(e =>
        e.nombre.toLowerCase().includes(criterio));
    renderListaEspecialidades(filtradas);
}

function abrirEdicionEspecialidad(especialidad) {
    especialidadEditandoId = especialidad.id;
    document.getElementById('esp-nombre').value = especialidad.nombre;
    document.getElementById('esp-descripcion').value = especialidad.descripcion || '';
    document.getElementById('esp-costo').value = especialidad.costo;
    document.getElementById('esp-form-titulo').innerHTML = '<i class="bi bi-pencil"></i> Editar especialidad';
    document.getElementById('esp-btn-guardar').innerHTML = '<i class="bi bi-check-lg"></i> Guardar cambios';
    document.getElementById('esp-btn-cancelar-edicion').style.display = 'inline-flex';
}

function cancelarEdicionEspecialidad() {
    especialidadEditandoId = null;
    document.getElementById('esp-nombre').value = '';
    document.getElementById('esp-descripcion').value = '';
    document.getElementById('esp-costo').value = '';
    document.getElementById('esp-form-titulo').innerHTML = '<i class="bi bi-plus-circle"></i> Nueva especialidad';
    document.getElementById('esp-btn-guardar').innerHTML = '<i class="bi bi-check-lg"></i> Registrar especialidad';
    document.getElementById('esp-btn-cancelar-edicion').style.display = 'none';
}

async function guardarEspecialidad() {
    const nombre = document.getElementById('esp-nombre').value.trim();
    const descripcion = document.getElementById('esp-descripcion').value.trim();
    const costo = document.getElementById('esp-costo').value;

    if (!nombre) return UI.mostrarError(new Error('El nombre de la especialidad es obligatorio'));
    if (!costo || parseFloat(costo) < 0) return UI.mostrarError(new Error('Ingresa un costo válido'));

    const body = JSON.stringify({ nombre, descripcion, costo: parseFloat(costo) });

    try {
        if (especialidadEditandoId) {
            await apiFetch(`/especialidades/${especialidadEditandoId}`, { method: 'PUT', body });
            UI.mostrarAlerta('Especialidad actualizada correctamente', 'success');
        } else {
            await apiFetch('/especialidades', { method: 'POST', body });
            UI.mostrarAlerta('Especialidad registrada correctamente', 'success');
        }
        cancelarEdicionEspecialidad();
        cargarListaEspecialidades();
        cargarEspecialidadesSelect();
    } catch (err) {
        UI.mostrarError(err);
    }
}

async function desactivarEspecialidad(id, nombre) {
    if (!confirm(`¿Desactivar la especialidad "${nombre}"? Ya no podrá asignarse a nuevos médicos.`)) return;
    try {
        await apiFetch(`/especialidades/${id}`, { method: 'DELETE' });
        UI.mostrarAlerta('Especialidad desactivada correctamente', 'success');
        cargarListaEspecialidades();
        cargarEspecialidadesSelect();
    } catch (err) {
        UI.mostrarError(err);
    }
}

async function activarEspecialidad(id, nombre) {
    try {
        await apiFetch(`/especialidades/${id}/activar`, { method: 'PUT' });
        UI.mostrarAlerta('Especialidad activada correctamente', 'success');
        cargarListaEspecialidades();
        cargarEspecialidadesSelect();
    } catch (err) {
        UI.mostrarError(err);
    }
}

cargarMedicos();