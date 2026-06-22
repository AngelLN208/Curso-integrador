/**
 * citas.js (médico) — Mis citas y flujo de atención
 */

AuthService.requireAuth();
const usuario = Auth.getUsuario();
iniciarSidebar('Mis citas');

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
let todasMisCitas = [];
let citaActivaId = null;

// ── Cargar citas ──────────────────────────────────────────────
async function cargarCitas() {
    if (!usuario.medicoId) {
        document.getElementById('tabla-citas').innerHTML =
            `<tr><td colspan="7" class="empty-row">No se encontró tu perfil de médico</td></tr>`;
        return;
    }

    try {
        todasMisCitas = await CitaService.buscar({ medicoId: usuario.medicoId });
        renderTabla(todasMisCitas);
    } catch (err) {
        UI.mostrarError(err);
    }
}

function renderTabla(citas) {
    document.getElementById('total-mostradas').textContent = `${citas.length} citas`;
    const tbody = document.getElementById('tabla-citas');

    if (!citas.length) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-row">No tienes citas registradas</td></tr>`;
        return;
    }

    const ordenadas = [...citas].sort((a, b) => new Date(b.fechaHora) - new Date(a.fechaHora));

    tbody.innerHTML = ordenadas.map(c => {
        const fecha = new Date(c.fechaHora).toLocaleDateString('es-PE',
            { day: '2-digit', month: '2-digit', year: 'numeric' });
        const hora = new Date(c.fechaHora).toLocaleTimeString('es-PE',
            { hour: '2-digit', minute: '2-digit' });

        const puedeAtender = c.estado === 'CONFIRMADA';
        const yaAtendida = c.estado === 'ATENDIDA';

        return `<tr>
      <td>${fecha}</td>
      <td>${hora}</td>
      <td style="font-weight:500;color:var(--text)">${c.pacienteNombre}</td>
      <td><span style="font-family:monospace;font-size:13px;color:var(--text-2)">${c.pacienteDni || '—'}</span></td>
      <td style="color:var(--text-2)">${c.motivo || '—'}</td>
      <td>${UI.badgeEstado(c.estado)}</td>
      <td>
        ${puedeAtender
                ? `<button class="btn btn-sm btn-indigo" onclick="abrirAtender(${c.id})">
               <i class="bi bi-clipboard2-pulse"></i> Atender
             </button>`
                : yaAtendida
                    ? `<button class="btn btn-sm btn-ghost" onclick="verAtencion(${c.id}, ${c.pacienteId})">
               <i class="bi bi-eye"></i> Ver
             </button>`
                    : '<span style="font-size:12px;color:var(--text-3)">Sin acciones</span>'}
      </td>
    </tr>`;
    }).join('');
}

// ── Filtros ───────────────────────────────────────────────────
function aplicarFiltrosLocal() {
    const paciente = document.getElementById('filtro-paciente').value.toLowerCase().trim();
    const fecha = document.getElementById('filtro-fecha').value;
    const estado = document.getElementById('filtro-estado').value;

    const filtradas = todasMisCitas.filter(c => {
        const matchPaciente = !paciente || c.pacienteNombre.toLowerCase().includes(paciente);
        const matchFecha = !fecha || c.fechaHora.startsWith(fecha);
        const matchEstado = !estado || c.estado === estado;
        return matchPaciente && matchFecha && matchEstado;
    });

    renderTabla(filtradas);
}

document.getElementById('filtro-paciente').addEventListener('input', aplicarFiltrosLocal);
document.getElementById('filtro-fecha').addEventListener('change', aplicarFiltrosLocal);
document.getElementById('filtro-estado').addEventListener('change', aplicarFiltrosLocal);

// ── Atender paciente (flujo nuevo: triaje + consulta) ─────────
async function abrirAtender(citaId) {
    citaActivaId = citaId;
    const cita = todasMisCitas.find(c => c.id === citaId);
    if (!cita) return;

    document.getElementById('atender-paciente-nombre').textContent = cita.pacienteNombre;
    const fecha = new Date(cita.fechaHora).toLocaleDateString('es-PE',
        { day: '2-digit', month: '2-digit', year: 'numeric' });
    const hora = new Date(cita.fechaHora).toLocaleTimeString('es-PE',
        { hour: '2-digit', minute: '2-digit' });
    document.getElementById('atender-info').textContent =
        `${fecha} — ${hora} · ${cita.motivo || 'Sin motivo especificado'}`;

    // Reset de formularios
    ['tri-presion', 'tri-temperatura', 'tri-peso', 'con-diagnostico', 'con-tratamiento', 'con-observaciones']
        .forEach(id => document.getElementById(id).value = '');

    // Asegura que los campos de consulta estén habilitados
    ['con-diagnostico', 'con-tratamiento', 'con-observaciones'].forEach(id => {
        document.getElementById(id).disabled = false;
    });

    document.getElementById('seccion-triaje').style.display = 'block';
    document.getElementById('seccion-triaje-readonly').style.display = 'none';
    document.getElementById('seccion-consulta').style.display = 'none';
    document.getElementById('btn-finalizar-consulta').style.display = 'none';
    document.getElementById('btn-editar-consulta').style.display = 'none';
    document.getElementById('btn-guardar-edicion').style.display = 'none';
    document.getElementById('aviso-no-editable').style.display = 'none';
    document.getElementById('btn-guardar-triaje').disabled = false;
    document.getElementById('btn-guardar-triaje').innerHTML = '<i class="bi bi-check-lg"></i> Guardar triaje';

    abrirModal('modalAtender');

    // Verifica si esta cita ya tiene triaje registrado (de un intento anterior)
    // y en ese caso salta directo a la sección de consulta.
    try {
        const historial = await AtencionService.historial(cita.pacienteId);
        const yaConsultada = historial.some(h => h.citaId === citaId);

        if (yaConsultada) {
            document.getElementById('btn-guardar-triaje').disabled = true;
            document.getElementById('btn-guardar-triaje').innerHTML = '<i class="bi bi-check-circle"></i> Ya atendida';
            document.getElementById('seccion-consulta').style.display = 'none';
        }
    } catch (err) {
        // Si falla la verificación, dejamos el flujo normal (triaje primero)
    }
}

async function guardarTriaje() {
    const presionArterial = document.getElementById('tri-presion').value.trim();
    const temperatura = document.getElementById('tri-temperatura').value;
    const peso = document.getElementById('tri-peso').value;

    try {
        await AtencionService.registrarTriaje({
            citaId: citaActivaId,
            presionArterial: presionArterial || null,
            temperatura: temperatura ? parseFloat(temperatura) : null,
            peso: peso ? parseFloat(peso) : null
        });

        document.getElementById('btn-guardar-triaje').disabled = true;
        document.getElementById('btn-guardar-triaje').innerHTML = '<i class="bi bi-check-circle"></i> Triaje guardado';
        document.getElementById('seccion-consulta').style.display = 'block';
        document.getElementById('btn-finalizar-consulta').style.display = 'inline-flex';
        UI.mostrarAlerta('Triaje registrado correctamente', 'success');

    } catch (err) {
        if (err.message && err.message.toLowerCase().includes('ya existe un triaje')) {
            document.getElementById('btn-guardar-triaje').disabled = true;
            document.getElementById('btn-guardar-triaje').innerHTML = '<i class="bi bi-check-circle"></i> Triaje ya registrado';
            document.getElementById('seccion-consulta').style.display = 'block';
            document.getElementById('btn-finalizar-consulta').style.display = 'inline-flex';
        } else {
            UI.mostrarError(err);
        }
    }
}

async function finalizarConsulta() {
    const diagnostico = document.getElementById('con-diagnostico').value.trim();
    const tratamiento = document.getElementById('con-tratamiento').value.trim();
    const observaciones = document.getElementById('con-observaciones').value.trim();

    if (!diagnostico) return UI.mostrarError(new Error('El diagnóstico es obligatorio'));
    if (!tratamiento) return UI.mostrarError(new Error('El tratamiento es obligatorio'));

    try {
        await AtencionService.registrarConsulta({
            citaId: citaActivaId,
            diagnostico,
            tratamiento,
            observaciones: observaciones || null
        });

        UI.mostrarAlerta('Atención registrada correctamente', 'success');
        cerrarModal('modalAtender');
        cargarCitas();

    } catch (err) {
        UI.mostrarError(err);
    }
}

// ── Ver atención ya registrada (con triaje + opción de editar) ──
async function verAtencion(citaId, pacienteId) {
    citaActivaId = citaId;
    const cita = todasMisCitas.find(c => c.id === citaId);
    if (!cita) return;

    document.getElementById('atender-paciente-nombre').textContent = cita.pacienteNombre;
    const fecha = new Date(cita.fechaHora).toLocaleDateString('es-PE',
        { day: '2-digit', month: '2-digit', year: 'numeric' });
    const hora = new Date(cita.fechaHora).toLocaleTimeString('es-PE',
        { hour: '2-digit', minute: '2-digit' });
    document.getElementById('atender-info').textContent =
        `${fecha} — ${hora} · ${cita.motivo || 'Sin motivo especificado'}`;

    document.getElementById('seccion-triaje').style.display = 'none';
    document.getElementById('seccion-triaje-readonly').style.display = 'block';
    document.getElementById('seccion-consulta').style.display = 'block';
    document.getElementById('btn-finalizar-consulta').style.display = 'none';
    document.getElementById('btn-guardar-edicion').style.display = 'none';
    document.getElementById('btn-editar-consulta').style.display = 'none';
    document.getElementById('aviso-no-editable').style.display = 'none';

    try {
        const historial = await AtencionService.historial(pacienteId);
        const consulta = historial.find(h => h.citaId === citaId);

        if (consulta) {
            document.getElementById('con-diagnostico').value = consulta.diagnostico || '';
            document.getElementById('con-tratamiento').value = consulta.tratamiento || '';
            document.getElementById('con-observaciones').value = consulta.observaciones || '';

            document.getElementById('ver-presion').value = consulta.presionArterial || '';
            document.getElementById('ver-temperatura').value = consulta.temperatura || '';
            document.getElementById('ver-peso').value = consulta.peso || '';

            // Modo solo lectura por defecto
            ['con-diagnostico', 'con-tratamiento', 'con-observaciones',
                'ver-presion', 'ver-temperatura', 'ver-peso'].forEach(id => {
                    document.getElementById(id).disabled = true;
                });

            // Solo el médico que atendió, y solo dentro de la ventana de edición
            const esMismoMedico = consulta.medicoId === usuario.medicoId;
            if (esMismoMedico && consulta.editable) {
                document.getElementById('btn-editar-consulta').style.display = 'inline-flex';
            } else if (esMismoMedico && !consulta.editable) {
                document.getElementById('aviso-no-editable').style.display = 'block';
            }
        }

        abrirModal('modalAtender');

    } catch (err) {
        UI.mostrarError(err);
    }
}

function habilitarEdicion() {
    ['con-diagnostico', 'con-tratamiento', 'con-observaciones',
        'ver-presion', 'ver-temperatura', 'ver-peso'].forEach(id => {
            document.getElementById(id).disabled = false;
        });
    document.getElementById('btn-editar-consulta').style.display = 'none';
    document.getElementById('btn-guardar-edicion').style.display = 'inline-flex';
}

async function guardarEdicion() {
    const diagnostico = document.getElementById('con-diagnostico').value.trim();
    const tratamiento = document.getElementById('con-tratamiento').value.trim();
    const observaciones = document.getElementById('con-observaciones').value.trim();

    const presionArterial = document.getElementById('ver-presion').value.trim();
    const temperatura = document.getElementById('ver-temperatura').value;
    const peso = document.getElementById('ver-peso').value;

    if (!diagnostico) return UI.mostrarError(new Error('El diagnóstico es obligatorio'));
    if (!tratamiento) return UI.mostrarError(new Error('El tratamiento es obligatorio'));

    try {
        await AtencionService.editarConsulta(citaActivaId, {
            diagnostico, tratamiento, observaciones: observaciones || null
        });

        await AtencionService.editarTriaje(citaActivaId, {
            presionArterial: presionArterial || null,
            temperatura: temperatura ? parseFloat(temperatura) : null,
            peso: peso ? parseFloat(peso) : null
        });

        UI.mostrarAlerta('Atención actualizada correctamente', 'success');
        cerrarModal('modalAtender');
        cargarCitas();

    } catch (err) {
        UI.mostrarError(err);
    }
}

cargarCitas();