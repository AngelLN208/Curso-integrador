let citaActivaId = null;

async function cargarCitas() {
    try {
        const citas = await CitaService.listar();
        const tbody = document.getElementById('tablaCitas');
        tbody.innerHTML = '';

        citas.forEach(cita => {
            tbody.insertAdjacentHTML('beforeend', `
                <tr>
                    <td>${UI.formatFecha(cita.fecha)}</td>
                    <td>${cita.hora}</td>
                    <td>${cita.pacienteNombre}</td>
                    <td>${cita.medicoNombre}</td>
                    <td>${cita.especialidad}</td>
                    <td>${UI.badgeEstado(cita.estado)}</td>
                    <td>
                        <button class="btn btn-sm btn-primary"
                                onclick="abrirVerCita(${cita.id})">Ver</button>
                        <button class="btn btn-sm btn-secondary"
                                onclick="abrirEditarCita(${cita.id})">Editar</button>
                        <button class="btn btn-sm btn-danger"
                                onclick="abrirCancelarCita(${cita.id})">Cancelar</button>
                    </td>
                </tr>`);
        });
    } catch (err) {
        UI.mostrarError(err);
    }
}

async function abrirVerCita(id) {
    try {
        citaActivaId = id;
        const cita = await CitaService.getById(id);
        document.getElementById('ver-paciente').textContent     = cita.pacienteNombre;
        document.getElementById('ver-medico').textContent       = cita.medicoNombre;
        document.getElementById('ver-especialidad').textContent = cita.especialidad;
        document.getElementById('ver-fecha').textContent        = UI.formatFecha(cita.fecha);
        document.getElementById('ver-hora').textContent         = cita.hora;
        document.getElementById('ver-estado').innerHTML         = UI.badgeEstado(cita.estado);
        document.getElementById('ver-motivo').textContent       = cita.motivo;
        new bootstrap.Modal(document.getElementById('modalVerCita')).show();
    } catch (err) { UI.mostrarError(err); }
}

async function abrirEditarCita(id) {
    try {
        citaActivaId = id;
        const cita = await CitaService.getById(id);
        document.getElementById('edit-especialidad').value = cita.especialidad;
        document.getElementById('edit-medico').value       = cita.medicoNombre;
        document.getElementById('edit-fecha').value        = cita.fecha;
        document.getElementById('edit-hora').value         = cita.hora;
        document.getElementById('edit-estado').value       = cita.estado;
        document.getElementById('edit-motivo').value       = cita.motivo;
        new bootstrap.Modal(document.getElementById('modalEditarCita')).show();
    } catch (err) { UI.mostrarError(err); }
}

async function guardarEdicion() {
    try {
        const data = {
            especialidad: document.getElementById('edit-especialidad').value,
            medicoNombre: document.getElementById('edit-medico').value,
            fecha:        document.getElementById('edit-fecha').value,
            hora:         document.getElementById('edit-hora').value,
            motivo:       document.getElementById('edit-motivo').value
        };
        await CitaService.reprogramar(citaActivaId, data);
        bootstrap.Modal.getInstance(document.getElementById('modalEditarCita')).hide();
        UI.mostrarAlerta('Cita actualizada correctamente');
        cargarCitas();
    } catch (err) { UI.mostrarError(err); }
}

function abrirCancelarCita(id) {
    citaActivaId = id;
    new bootstrap.Modal(document.getElementById('modalCancelarCita')).show();
}

async function confirmarCancelacion() {
    try {
        const motivo = document.getElementById('cancelar-motivo').value;
        await CitaService.cancelar(citaActivaId, motivo);
        bootstrap.Modal.getInstance(document.getElementById('modalCancelarCita')).hide();
        UI.mostrarAlerta('Cita cancelada', 'warning');
        cargarCitas();
    } catch (err) { UI.mostrarError(err); }
}

async function guardarNuevaCita() {
    try {
        const data = {
            pacienteId:   document.getElementById('nueva-pacienteId').value,
            especialidad: document.getElementById('nueva-especialidad').value,
            medicoId:     document.getElementById('nueva-medico').value,
            fecha:        document.getElementById('nueva-fecha').value,
            hora:         document.getElementById('nueva-hora').value,
            motivo:       document.getElementById('nueva-motivo').value
        };
        await CitaService.crear(data);
        bootstrap.Modal.getInstance(document.getElementById('modalNuevaCita')).hide();
        UI.mostrarAlerta('Cita registrada correctamente');
        cargarCitas();
    } catch (err) { UI.mostrarError(err); }
}

document.addEventListener('DOMContentLoaded', cargarCitas);

async function guardarNuevaCita() {
    const form = {
        pacienteId: document.getElementById('cita-pacienteId').value,
        medicoId:   document.getElementById('cita-medicoId').value,
        fechaHora:  document.getElementById('cita-fechaHora').value,
        motivo:     document.getElementById('cita-motivo').value
    };

    // Valida antes de llamar a la API
    if (!Validators.cita(form)) return;

    try {
        await CitaService.crear({
            ...form,
            pacienteId: Number(form.pacienteId),
            medicoId:   Number(form.medicoId)
        });
        bootstrap.Modal.getInstance(document.getElementById('modalNuevaCita')).hide();
        UI.mostrarAlerta('Cita registrada correctamente');
        cargarCitas();
    } catch (err) {
        UI.mostrarError(err);
    }
}
