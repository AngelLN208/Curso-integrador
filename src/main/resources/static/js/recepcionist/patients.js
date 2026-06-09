console.log('patients.js cargado');
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

let pacienteActivoId = null;


// ─── Cargar tabla ────────────────────────────────────────────
async function cargarPacientes(criterio = '') {
    console.log('cargarPacientes ejecutado');
    try {
        const pacientes = criterio
            ? await PacienteService.buscar(criterio)
            : await PacienteService.listar();

        const tbody = document.getElementById('tabla-pacientes');
        tbody.innerHTML = '';

        // Tarjetas
        document.getElementById('total-pacientes').textContent = pacientes.length;

        const hoy = new Date();
        const nuevos = pacientes.filter(p => {
            const creado = new Date(p.creadoEn);
            return creado.getMonth() === hoy.getMonth() &&
                   creado.getFullYear() === hoy.getFullYear();
        });
        document.getElementById('nuevos-mes').textContent = nuevos.length;

        if (pacientes.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted">
                        No se encontraron pacientes
                    </td>
                </tr>`;
            return;
        }

        pacientes.forEach(p => {
            tbody.insertAdjacentHTML('beforeend', `
                <tr>
                    <td>${p.dni}</td>
                    <td>${p.nombres} ${p.apellidos}</td>
                    <td>${p.celular}</td>
                    <td>${p.correo ?? '-'}</td>
                    <td>
                        <button class="btn btn-sm btn-primary"
                                onclick="abrirVer(${p.id})">Ver</button>
                        <button class="btn btn-sm btn-secondary"
                                onclick="abrirEditar(${p.id})">Editar</button>
                    </td>
                </tr>`);
        });

    } catch (err) {
        UI.mostrarError(err);
    }
}

// ─── Ver paciente ────────────────────────────────────────────
async function abrirVer(id) {
    try {
        pacienteActivoId = id;
        const p = await PacienteService.getById(id);

        const iniciales = (p.nombres[0] + p.apellidos[0]).toUpperCase();
        document.getElementById('ver-iniciales').textContent      = iniciales;
        document.getElementById('ver-nombre').textContent         = `${p.nombres} ${p.apellidos}`;
        document.getElementById('ver-dni').textContent            = `DNI: ${p.dni}`;
        document.getElementById('ver-fechaNacimiento').textContent = UI.formatFecha(p.fechaNacimiento);
        document.getElementById('ver-celular').textContent        = p.celular;
        document.getElementById('ver-correo').textContent         = p.correo ?? '-';
        document.getElementById('ver-sexo').textContent           = p.sexo === 'M' ? 'Masculino' : 'Femenino';

        new bootstrap.Modal(document.getElementById('modalVer')).show();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ─── Editar paciente ─────────────────────────────────────────
async function abrirEditar(id) {
    try {
        pacienteActivoId = id;
        const p = await PacienteService.getById(id);

        document.getElementById('edit-pac-nombres').value          = p.nombres;
        document.getElementById('edit-pac-apellidos').value        = p.apellidos;
        document.getElementById('edit-pac-dni').value              = p.dni;
        document.getElementById('edit-pac-fechaNacimiento').value  = p.fechaNacimiento;
        document.getElementById('edit-pac-celular').value          = p.celular;
        document.getElementById('edit-pac-correo').value           = p.correo ?? '';
        document.getElementById('edit-pac-sexo').value             = p.sexo;

        new bootstrap.Modal(document.getElementById('modalEditar')).show();
    } catch (err) {
        UI.mostrarError(err);
    }
}

async function guardarEdicion() {
    const form = {
        dni:              document.getElementById('edit-pac-dni').value,
        nombres:          document.getElementById('edit-pac-nombres').value,
        apellidos:        document.getElementById('edit-pac-apellidos').value,
        fechaNacimiento:  document.getElementById('edit-pac-fechaNacimiento').value,
        celular:          document.getElementById('edit-pac-celular').value,
        correo:           document.getElementById('edit-pac-correo').value,
        sexo:             document.getElementById('edit-pac-sexo').value
    };

    // Reutiliza los validators pero con los IDs del modal editar
    // Validación manual simple aquí para no colisionar con el modal registrar
    if (!form.nombres || !form.apellidos || !form.celular) {
        UI.mostrarAlerta('Completa los campos obligatorios', 'danger');
        return;
    }

    try {
        await PacienteService.actualizar(pacienteActivoId, form);
        bootstrap.Modal.getInstance(document.getElementById('modalEditar')).hide();
        UI.mostrarAlerta('Paciente actualizado correctamente');
        cargarPacientes();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ─── Registrar paciente ──────────────────────────────────────
async function guardarPaciente() {
    const form = {
        dni:             document.getElementById('pac-dni').value,
        nombres:         document.getElementById('pac-nombres').value,
        apellidos:       document.getElementById('pac-apellidos').value,
        fechaNacimiento: document.getElementById('pac-fechaNacimiento').value,
        celular:         document.getElementById('pac-celular').value,
        correo:          document.getElementById('pac-correo').value,
        sexo:            document.getElementById('pac-sexo').value
    };

    if (!Validators.paciente(form)) return;

    try {
        await PacienteService.crear(form);
        bootstrap.Modal.getInstance(document.getElementById('modalRegistrar')).hide();
        UI.mostrarAlerta('Paciente registrado correctamente');
        cargarPacientes();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ─── Buscador ────────────────────────────────────────────────
let buscarTimeout;
document.getElementById('buscar-input').addEventListener('input', (e) => {
    clearTimeout(buscarTimeout);
    buscarTimeout = setTimeout(() => {
        cargarPacientes(e.target.value.trim());
    }, 400);
});

// ─── Init ────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', cargarPacientes);