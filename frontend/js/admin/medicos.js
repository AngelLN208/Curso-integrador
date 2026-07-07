/**
 * medicos.js — Gestión de médicos (admin)
 */

AuthService.requireAuth();
iniciarSidebar('Médicos');

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
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-neblina py-10 text-[13px]">No se encontraron médicos</td></tr>`;
        return;
    }

    tbody.innerHTML = medicos.map(m => {
        const iniciales = (m.nombres.charAt(0) + m.apellidos.charAt(0)).toUpperCase();
        return `<tr class="hover:bg-lienzo dark:hover:bg-tinta-dark transition-colors">
      <td class="px-4 py-3">
        <div class="flex items-center gap-2.5">
          <div class="w-9 h-9 rounded-full flex-shrink-0 bg-guia flex items-center justify-center text-[13px] font-display font-bold text-tinta">${iniciales}</div>
          <span class="font-medium">${m.nombres} ${m.apellidos}</span>
        </div>
      </td>
      <td class="px-4 py-3"><span class="font-mono text-xs text-neblina">${m.dni}</span></td>
      <td class="px-4 py-3"><span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-500/10 text-blue-600 dark:text-blue-400">${m.especialidadNombre}</span></td>
      <td class="px-4 py-3">${m.celular}</td>
      <td class="px-4 py-3 text-neblina">${m.correo}</td>
      <td class="px-4 py-3">
        ${m.activo
                ? '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rumbo/10 text-rumbo"><i class="bi bi-check-circle"></i> Activo</span>'
                : '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-alerta/10 text-alerta"><i class="bi bi-x-circle"></i> Inactivo</span>'}
      </td>
      <td class="px-4 py-3">
        <div class="flex gap-1.5">
          <button onclick="gestionarHorario(${m.id}, '${m.nombres} ${m.apellidos}', '${m.especialidadNombre}')" title="Horario"
            class="p-2 rounded-lg text-neblina hover:text-guia hover:bg-guia/10 transition-colors">
            <i class="bi bi-clock"></i>
          </button>
          ${m.activo
                ? `<button onclick="desactivarMedico(${m.id}, '${m.nombres} ${m.apellidos}')"
                 class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-alerta text-white hover:opacity-90 transition-opacity">
                 <i class="bi bi-slash-circle"></i> Desactivar
               </button>`
                : `<button onclick="activarMedico(${m.id}, '${m.nombres} ${m.apellidos}')"
                 class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-white dark:bg-tinta border border-borde dark:border-borde-dark text-neblina hover:text-tinta dark:hover:text-white transition-colors">
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
    cont.innerHTML = `<div class="text-center text-neblina py-6 text-[13px]">Cargando...</div>`;

    try {
        const horarios = await HorarioService.listarPorMedico(medicoId);

        if (!horarios.length) {
            cont.innerHTML = `<div class="text-center text-neblina py-6 text-[13px]">Sin horario asignado todavía</div>`;
            return;
        }

        cont.innerHTML = horarios.map(h => `
      <div class="flex justify-between items-center bg-lienzo dark:bg-tinta-dark rounded-lg px-3.5 py-2.5 mb-1.5">
        <div>
          <span class="font-semibold">${DIAS_LABEL_ADMIN[h.dia]}</span>
          <span class="text-neblina ml-2 font-mono text-[13px]">${h.horaInicio} — ${h.horaFin}</span>
        </div>
        <button onclick="eliminarBloqueHorario(${h.id})" class="p-1.5 rounded-lg bg-alerta text-white hover:opacity-90 transition-opacity">
          <i class="bi bi-trash3 text-xs"></i>
        </button>
      </div>`).join('');

    } catch (err) {
        cont.innerHTML = `<div class="text-center text-neblina py-6 text-[13px]">No se pudo cargar el horario</div>`;
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
    cont.innerHTML = `<div class="text-center text-neblina py-6 text-[13px]">Cargando...</div>`;
    try {
        const json = await apiFetch('/especialidades/todas');
        todasLasEspecialidades = json.data ?? json;
        renderListaEspecialidades(todasLasEspecialidades);
    } catch (err) {
        cont.innerHTML = `<div class="text-center text-neblina py-6 text-[13px]">No se pudo cargar la lista</div>`;
    }
}

function renderListaEspecialidades(especialidades) {
    const cont = document.getElementById('lista-especialidades');

    if (!especialidades.length) {
        cont.innerHTML = `<div class="text-center text-neblina py-6 text-[13px]">Sin especialidades encontradas</div>`;
        return;
    }

    cont.innerHTML = especialidades.map(e => `
      <div class="flex justify-between items-center bg-lienzo dark:bg-tinta-dark rounded-lg px-3.5 py-2.5 mb-1.5">
        <div class="flex items-center gap-2 flex-wrap">
          <span class="font-semibold">${e.nombre}</span>
          <span class="text-neblina font-mono text-[13px]">S/ ${Number(e.costo).toFixed(2)}</span>
          ${e.activo
            ? '<span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-rumbo/10 text-rumbo"><i class="bi bi-check-circle"></i> Activa</span>'
            : '<span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-alerta/10 text-alerta"><i class="bi bi-x-circle"></i> Inactiva</span>'}
        </div>
        <div class="flex gap-1.5 flex-shrink-0">
          <button onclick='abrirEdicionEspecialidad(${JSON.stringify(e)})' title="Editar"
            class="p-2 rounded-lg text-neblina hover:text-guia hover:bg-guia/10 transition-colors">
            <i class="bi bi-pencil"></i>
          </button>
          ${e.activo
            ? `<button onclick="desactivarEspecialidad(${e.id}, '${e.nombre}')"
                 class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-alerta text-white hover:opacity-90 transition-opacity">
                 <i class="bi bi-slash-circle"></i> Desactivar
               </button>`
            : `<button onclick="activarEspecialidad(${e.id}, '${e.nombre}')"
                 class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-white dark:bg-tinta border border-borde dark:border-borde-dark text-neblina hover:text-tinta dark:hover:text-white transition-colors">
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
    document.getElementById('esp-btn-cancelar-edicion').classList.remove('hidden');
}

function cancelarEdicionEspecialidad() {
    especialidadEditandoId = null;
    document.getElementById('esp-nombre').value = '';
    document.getElementById('esp-descripcion').value = '';
    document.getElementById('esp-costo').value = '';
    document.getElementById('esp-form-titulo').innerHTML = '<i class="bi bi-plus-circle"></i> Nueva especialidad';
    document.getElementById('esp-btn-guardar').innerHTML = '<i class="bi bi-check-lg"></i> Registrar especialidad';
    document.getElementById('esp-btn-cancelar-edicion').classList.add('hidden');
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