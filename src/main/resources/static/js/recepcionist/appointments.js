console.log('appointments.js cargado');

// ─── Guard ───────────────────────────────────────────────────
const usuario = JSON.parse(localStorage.getItem('usuario'));
if (!localStorage.getItem('token') || !usuario) {
    window.location.href = '/src/main/resources/static/views/auth/login.html';
}

// ─── Logout ──────────────────────────────────────────────────
document.getElementById('btnLogout').addEventListener('click', (e) => {
    e.preventDefault();
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    window.location.href = '/src/main/resources/static/views/auth/login.html';
});

let citaActivaId = null;

// ─── Cargar contadores ───────────────────────────────────────
async function cargarContadores() {
    try {
        const citas = await CitaService.listar();
        const hoy   = new Date().toISOString().split('T')[0];

        document.getElementById('total-hoy').textContent =
            citas.filter(c => c.fechaHora.startsWith(hoy)).length;
        document.getElementById('total-confirmadas').textContent =
            citas.filter(c => c.estado === 'CONFIRMADA').length;
        document.getElementById('total-pendientes').textContent =
            citas.filter(c => c.estado === 'PENDIENTE').length;
        document.getElementById('total-canceladas').textContent =
            citas.filter(c => c.estado === 'CANCELADA').length;
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ─── Cargar tabla ────────────────────────────────────────────
async function cargarCitas() {
    try {
        const citas = await CitaService.listar();
        renderTabla(citas);
    } catch (err) {
        UI.mostrarError(err);
    }
}

function renderTabla(citas) {
    const tbody = document.getElementById('tabla-citas');
    tbody.innerHTML = '';

    if (citas.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-muted">
                    No se encontraron citas
                </td>
            </tr>`;
        return;
    }

    citas.forEach(c => {
        const fecha = c.fechaHora.split('T')[0];
        const hora  = c.fechaHora.split('T')[1].substring(0, 5);
        tbody.insertAdjacentHTML('beforeend', `
            <tr>
                <td>${UI.formatFecha(fecha)}</td>
                <td>${hora}</td>
                <td>${c.pacienteNombre}</td>
                <td>${c.medicoNombre}</td>
                <td>${c.especialidad}</td>
                <td>${UI.badgeEstado(c.estado)}</td>
                <td>
                    <button class="btn btn-sm btn-primary"
                            onclick="abrirVerCita(${c.id})">Ver</button>
                    <button class="btn btn-sm btn-secondary"
                            onclick="abrirReprogramar(${c.id})">Editar</button>
                    <button class="btn btn-sm btn-danger"
                            onclick="abrirCancelar(${c.id}, '${c.pacienteNombre}', '${UI.formatFecha(fecha)}', '${hora}', '${c.especialidad}')">
                        Cancelar
                    </button>
                </td>
            </tr>`);
    });
}

// ─── Filtros ─────────────────────────────────────────────────
async function aplicarFiltros() {
    try {
        const paciente     = document.getElementById('filtro-paciente').value.trim();
        const fecha        = document.getElementById('filtro-fecha').value;
        const especialidad = document.getElementById('filtro-especialidad').value;
        const estado       = document.getElementById('filtro-estado').value;

        const filtros = {};
        if (paciente)     filtros.paciente     = paciente;
        if (fecha)        filtros.fecha        = fecha;
        if (especialidad) filtros.especialidad = especialidad;
        if (estado)       filtros.estado       = estado;

        const citas = Object.keys(filtros).length > 0
            ? await CitaService.buscar(filtros)
            : await CitaService.listar();

        renderTabla(citas);
    } catch (err) {
        UI.mostrarError(err);
    }
}

document.getElementById('filtro-paciente').addEventListener('input', aplicarFiltros);
document.getElementById('filtro-fecha').addEventListener('change', aplicarFiltros);
document.getElementById('filtro-especialidad').addEventListener('change', aplicarFiltros);
document.getElementById('filtro-estado').addEventListener('change', aplicarFiltros);

// ─── Ver cita ────────────────────────────────────────────────
async function abrirVerCita(id) {
    try {
        citaActivaId = id;
        const c = await CitaService.getById(id);
        const fecha = c.fechaHora.split('T')[0];
        const hora  = c.fechaHora.split('T')[1].substring(0, 5);

        document.getElementById('ver-paciente').textContent    = c.pacienteNombre;
        document.getElementById('ver-medico').textContent      = c.medicoNombre;
        document.getElementById('ver-especialidad').textContent = c.especialidad;
        document.getElementById('ver-fechaHora').textContent   = `${UI.formatFecha(fecha)} ${hora}`;
        document.getElementById('ver-estado').innerHTML        = UI.badgeEstado(c.estado);
        document.getElementById('ver-motivo').textContent      = c.motivo ?? '-';

        new bootstrap.Modal(document.getElementById('modalVerCita')).show();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ─── Reprogramar ─────────────────────────────────────────────
function abrirReprogramar(id) {
    citaActivaId = id;
    document.getElementById('reprog-fechaHora').value = '';
    new bootstrap.Modal(document.getElementById('modalEditarCita')).show();
}

async function guardarReprogramacion() {
    const form = {
        nuevaFechaHora: document.getElementById('reprog-fechaHora').value
    };

    if (!Validators.reprogramarCita(form)) return;

    try {
        await CitaService.reprogramar(citaActivaId, {
            nuevaFechaHora: form.nuevaFechaHora
        });
        bootstrap.Modal.getInstance(document.getElementById('modalEditarCita')).hide();
        UI.mostrarAlerta('Cita reprogramada correctamente');
        cargarCitas();
        cargarContadores();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ─── Cancelar ────────────────────────────────────────────────
function abrirCancelar(id, nombre, fecha, hora, especialidad) {
    citaActivaId = id;
    document.getElementById('cancelar-nombre').textContent  = nombre;
    document.getElementById('cancelar-detalle').textContent = `${fecha} — ${hora} — ${especialidad}`;
    document.getElementById('cancelar-motivo').value        = '';
    new bootstrap.Modal(document.getElementById('modalCancelarCita')).show();
}

async function confirmarCancelacion() {
    try {
        const motivo = document.getElementById('cancelar-motivo').value;
        await CitaService.cancelar(citaActivaId, motivo);
        bootstrap.Modal.getInstance(document.getElementById('modalCancelarCita')).hide();
        UI.mostrarAlerta('Cita cancelada', 'warning');
        cargarCitas();
        cargarContadores();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ─── Nueva cita ──────────────────────────────────────────────
async function cargarEspecialidades() {
    try {
        const res = await apiFetch('/especialidades');
        const especialidades = res.data ?? res;
        const sel1 = document.getElementById('nueva-especialidad');
        const sel2 = document.getElementById('filtro-especialidad');

        especialidades.forEach(e => {
            sel1.insertAdjacentHTML('beforeend',
                `<option value="${e.id}">${e.nombre}</option>`);
            sel2.insertAdjacentHTML('beforeend',
                `<option value="${e.nombre}">${e.nombre}</option>`);
        });
    } catch (err) {
        console.error('Error cargando especialidades:', err);
    }
}

document.getElementById('nueva-especialidad').addEventListener('change', async (e) => {
    const especialidadId = e.target.value;
    const selMedico = document.getElementById('cita-medicoId');
    selMedico.innerHTML = '<option value="">Cargando...</option>';

    if (!especialidadId) {
        selMedico.innerHTML = '<option value="">Seleccionar especialidad primero...</option>';
        return;
    }

    try {
        const medicos = await MedicoService.listarPorEspecialidad(especialidadId);
        selMedico.innerHTML = '<option value="">Seleccionar médico...</option>';
        medicos.forEach(m => {
            selMedico.insertAdjacentHTML('beforeend',
                `<option value="${m.id}">Dr(a). ${m.nombres} ${m.apellidos}</option>`);
        });
    } catch (err) {
        UI.mostrarError(err);
    }
});

async function guardarNuevaCita() {
    const form = {
        pacienteId: document.getElementById('cita-pacienteId').value,
        medicoId:   document.getElementById('cita-medicoId').value,
        fechaHora:  document.getElementById('cita-fechaHora').value,
        motivo:     document.getElementById('cita-motivo').value
    };

    if (!Validators.cita(form)) return;

    try {
        await CitaService.crear({
            pacienteId: Number(form.pacienteId),
            medicoId:   Number(form.medicoId),
            fechaHora:  form.fechaHora,
            motivo:     form.motivo
        });
        bootstrap.Modal.getInstance(document.getElementById('modalNuevaCita')).hide();
        UI.mostrarAlerta('Cita registrada correctamente');
        cargarCitas();
        cargarContadores();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ─── Buscar paciente para nueva cita ─────────────────────────
let buscarPacienteTimeout;
document.getElementById('nueva-buscar-paciente').addEventListener('input', (e) => {
    clearTimeout(buscarPacienteTimeout);
    buscarPacienteTimeout = setTimeout(async () => {
        const criterio = e.target.value.trim();
        if (!criterio) return;
        try {
            const pacientes = await PacienteService.buscar(criterio);
            const sel = document.getElementById('cita-pacienteId');
            sel.innerHTML = '<option value="">Seleccionar paciente...</option>';
            pacientes.forEach(p => {
                sel.insertAdjacentHTML('beforeend',
                    `<option value="${p.id}">${p.nombres} ${p.apellidos} — ${p.dni}</option>`);
            });
        } catch (err) {
            console.error(err);
        }
    }, 400);
});

// ─── Init ────────────────────────────────────────────────────
cargarContadores();
cargarEspecialidades();
cargarCitas();