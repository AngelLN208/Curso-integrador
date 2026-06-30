/**
 * directorio.js — Directorio público de médicos y agendamiento de citas
 */

PortalAuthService.requireAuth();
const pacienteActual = PortalAuth.getPaciente();
document.getElementById('saludo-usuario').textContent = pacienteActual?.nombreCompleto?.split(' ')[0] || '';

let todasLasEspecialidades = [];
let medicoSeleccionadoId = null;
let medicoSeleccionadoNombre = '';
let especialidadActivaId = '';

async function cargarDirectorio() {
    try {
        todasLasEspecialidades = await PortalService.directorio();
        renderFiltrosEspecialidad(todasLasEspecialidades);
        aplicarFiltros();
    } catch (err) {
        document.getElementById('grid-medicos').innerHTML =
            `<div class="empty-row" style="grid-column:1/-1">No se pudo cargar el directorio médico</div>`;
    }
}

function obtenerTodosLosMedicos(especialidades) {
    const medicos = [];
    especialidades.forEach(esp => {
        (esp.medicos || []).forEach(m => medicos.push({ ...m, especialidadNombre: esp.nombre }));
    });
    return medicos;
}

function renderFiltrosEspecialidad(especialidades) {
    const cont = document.getElementById('filtros-especialidad');
    especialidades.forEach(esp => {
        const btn = document.createElement('button');
        btn.className = 'btn btn-ghost btn-sm';
        btn.dataset.esp = esp.id;
        btn.innerHTML = `<i class="bi bi-heart-pulse"></i> ${esp.nombre} (${esp.totalMedicos})`;
        btn.onclick = () => filtrarPorEspecialidad(esp.id, btn);
        cont.appendChild(btn);
    });
}

function filtrarPorEspecialidad(especialidadId, btnClickeado) {
    document.querySelectorAll('#filtros-especialidad button').forEach(b => {
        b.className = 'btn btn-ghost btn-sm';
    });
    btnClickeado.className = 'btn btn-primary btn-sm';
    especialidadActivaId = especialidadId;
    aplicarFiltros();
}

// Convierte 'yyyy-MM-dd' al día de semana en formato MONDAY/TUESDAY/... (igual al backend)
function diaSemanaDeFecha(fechaStr) {
    const dias = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    const [y, m, d] = fechaStr.split('-').map(Number);
    return dias[new Date(y, m - 1, d).getDay()];
}

function aplicarFiltros() {
    const texto = document.getElementById('filtro-texto').value.trim().toLowerCase();
    const diaSeleccionado = document.getElementById('filtro-dia').value;
    const fechaSeleccionada = document.getElementById('filtro-fecha').value;

    // Base: todos los médicos o solo los de la especialidad activa
    let medicos;
    if (!especialidadActivaId) {
        medicos = obtenerTodosLosMedicos(todasLasEspecialidades);
    } else {
        const especialidad = todasLasEspecialidades.find(e => e.id == especialidadActivaId);
        medicos = (especialidad?.medicos || []).map(m => ({ ...m, especialidadNombre: especialidad.nombre }));
    }

    // Filtro por nombre/apellido (nombre completo, búsqueda parcial)
    if (texto) {
        medicos = medicos.filter(m => m.nombreCompleto.toLowerCase().includes(texto));
    }

    // Filtro por día de la semana
    if (diaSeleccionado) {
        medicos = medicos.filter(m =>
            (m.horarios || []).some(h => h.dia === diaSeleccionado)
        );
    }

    // Filtro por fecha específica (se traduce al día de semana correspondiente)
    if (fechaSeleccionada) {
        const diaDeLaFecha = diaSemanaDeFecha(fechaSeleccionada);
        medicos = medicos.filter(m =>
            (m.horarios || []).some(h => h.dia === diaDeLaFecha)
        );
    }

    renderMedicos(medicos);
}

function limpiarFiltros() {
    document.getElementById('filtro-texto').value = '';
    document.getElementById('filtro-dia').value = '';
    document.getElementById('filtro-fecha').value = '';
    especialidadActivaId = '';
    document.querySelectorAll('#filtros-especialidad button').forEach(b => b.className = 'btn btn-ghost btn-sm');
    document.querySelector('#filtros-especialidad button[data-esp=""]').className = 'btn btn-primary btn-sm';
    aplicarFiltros();
}

function renderMedicos(medicos) {
    const cont = document.getElementById('grid-medicos');

    if (!medicos.length) {
        cont.innerHTML = `<div class="empty-row" style="grid-column:1/-1">No hay médicos disponibles en esta especialidad</div>`;
        return;
    }

    cont.innerHTML = medicos.map(m => {
        const iniciales = m.nombreCompleto.split(' ').slice(0, 2).map(n => n.charAt(0)).join('').toUpperCase();
        const estrellas = renderEstrellas(m.promedioValoracion);

        return `
      <div class="card" style="padding:22px;text-align:center">
        <div style="width:64px;height:64px;border-radius:50%;margin:0 auto 14px;
                    background:linear-gradient(135deg,var(--primary),var(--accent));
                    display:flex;align-items:center;justify-content:center;
                    color:white;font-size:22px;font-weight:700">${iniciales}</div>
        <div style="font-weight:700;font-size:15px;margin-bottom:2px">${m.nombreCompleto}</div>
        <div style="font-size:12.5px;color:var(--primary);font-weight:600;margin-bottom:8px">${m.especialidadNombre}</div>
        <div style="margin-bottom:14px">${estrellas}
          <span style="font-size:11.5px;color:var(--text-3);margin-left:4px">
            (${m.totalValoraciones || 0})
          </span>
        </div>
        <button class="btn btn-primary btn-sm btn-block" onclick="abrirAgendarCita(${m.id}, '${m.nombreCompleto.replace(/'/g, "\\'")}', '${m.especialidadNombre.replace(/'/g, "\\'")}')">
          <i class="bi bi-calendar-plus"></i> Agendar cita
        </button>
      </div>`;
    }).join('');
}

function renderEstrellas(promedio) {
    const valor = promedio || 0;
    let html = '';
    for (let i = 1; i <= 5; i++) {
        html += `<i class="bi bi-star${i <= Math.round(valor) ? '-fill' : ''}" style="color:var(--amber);font-size:13px"></i>`;
    }
    return html;
}

// ── Modal de agendar cita ──────────────────────────────────────
function abrirAgendarCita(medicoId, nombreMedico, especialidad) {
    medicoSeleccionadoId = medicoId;
    medicoSeleccionadoNombre = nombreMedico;

    document.getElementById('agendar-medico-nombre').textContent = nombreMedico;
    document.getElementById('agendar-medico-especialidad').textContent = especialidad;
    document.getElementById('agendar-fecha').value = '';
    document.getElementById('agendar-motivo').value = '';
    document.getElementById('agendar-fechahora-elegida').value = '';
    document.getElementById('agendar-slots').innerHTML =
        '<span style="font-size:13px;color:var(--text-3)">Selecciona una fecha primero</span>';

    document.getElementById('agendar-fecha').min = new Date().toISOString().split('T')[0];

    document.getElementById('modalAgendar').style.display = 'flex';
}

function cerrarModalAgendar() {
    document.getElementById('modalAgendar').style.display = 'none';
}

let slotsDisponiblesActuales = [];

async function cargarSlotsDisponibles() {
    const fecha = document.getElementById('agendar-fecha').value;
    const cont = document.getElementById('agendar-slots');

    if (!fecha) return;

    cont.innerHTML = '<span style="font-size:13px;color:var(--text-3)">Cargando horarios...</span>';

    try {
        const slots = await PortalService.disponibilidadSlots(medicoSeleccionadoId, fecha);
        slotsDisponiblesActuales = slots;

        if (!slots.length) {
            cont.innerHTML = '<span style="font-size:13px;color:var(--text-3)">No hay horarios disponibles este día</span>';
            return;
        }

        cont.innerHTML = slots.map((s, idx) => `
      <button type="button" class="btn btn-ghost btn-sm" onclick="elegirSlot(${idx}, this)">
        ${s.hora}
      </button>`).join('');

    } catch (err) {
        cont.innerHTML = '<span style="font-size:13px;color:var(--red)">No se pudo cargar la disponibilidad</span>';
    }
}

function elegirSlot(idx, btnClickeado) {
    document.querySelectorAll('#agendar-slots button').forEach(b => b.className = 'btn btn-ghost btn-sm');
    btnClickeado.className = 'btn btn-primary btn-sm';
    document.getElementById('agendar-fechahora-elegida').value = slotsDisponiblesActuales[idx].fechaHora;
}

async function confirmarAgendarCita() {
    const fechaHora = document.getElementById('agendar-fechahora-elegida').value;
    const motivo = document.getElementById('agendar-motivo').value.trim();

    if (!fechaHora) return PortalNotify.error('Selecciona un horario disponible');

    const btn = document.getElementById('btn-confirmar-cita');
    btn.disabled = true;
    btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Agendando...';

    try {
        await PortalService.agendarCita({
            medicoId: medicoSeleccionadoId,
            fechaHora,
            motivo: motivo || null
        });

        PortalNotify.success('¡Cita agendada correctamente!');
        cerrarModalAgendar();

    } catch (err) {
        PortalNotify.error(err.message || 'No se pudo agendar la cita');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-calendar-check"></i> Confirmar cita';
    }
}
// Si venimos del chatbot con una especialidad sugerida, filtrar automáticamente
async function aplicarEspecialidadSugerida() {
    const nombreSugerido = sessionStorage.getItem('especialidad_sugerida');
    if (!nombreSugerido) return;
    sessionStorage.removeItem('especialidad_sugerida');

    // Esperamos a que las especialidades ya estén cargadas (cargarDirectorio se ejecuta antes)
    const especialidad = todasLasEspecialidades.find(e =>
        e.nombre.toLowerCase() === nombreSugerido.toLowerCase());

    if (especialidad) {
        const btn = document.querySelector(`#filtros-especialidad button[data-esp="${especialidad.id}"]`);
        if (btn) filtrarPorEspecialidad(especialidad.id, btn);
    }
}

cargarDirectorio().then(aplicarEspecialidadSugerida);

cargarDirectorio();