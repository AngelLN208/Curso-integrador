/**
 * directorio.js — Directorio público de médicos y agendamiento de citas
 */

PortalAuthService.requireAuth();
const pacienteActual = PortalAuth.getPaciente();
const nombreCortoDirectorio = pacienteActual?.nombreCompleto?.split(' ')[0] || '';
document.getElementById('saludo-usuario').textContent = nombreCortoDirectorio;
document.getElementById('saludo-usuario-mobile').textContent = nombreCortoDirectorio;

let todasLasEspecialidades = [];
let medicoSeleccionadoId = null;
let medicoSeleccionadoNombre = '';
let especialidadActivaId = '';

const MEDICOS_POR_PAGINA = 9;
let medicosFiltradosActuales = [];
let paginaActual = 1;

async function cargarDirectorio() {
    try {
        todasLasEspecialidades = await PortalService.directorio();
        renderFiltrosEspecialidad(todasLasEspecialidades);
        aplicarFiltros();
    } catch (err) {
        document.getElementById('grid-medicos').innerHTML =
            `<div class="text-center text-sm text-white/60 py-10 col-span-full">No se pudo cargar el directorio médico</div>`;
    }
}

function obtenerTodosLosMedicos(especialidades) {
    const medicos = [];
    especialidades.forEach(esp => {
        (esp.medicos || []).forEach(m => medicos.push({ ...m, especialidadNombre: esp.nombre }));
    });
    return medicos;
}

const CLASE_CHIP_INACTIVO = 'flex items-center gap-1.5 bg-white/10 border border-white/15 text-white/80 text-xs font-medium rounded-lg px-3.5 py-2 transition hover:bg-white/[0.16]';
const CLASE_CHIP_ACTIVO = 'flex items-center gap-1.5 bg-guia text-white text-xs font-semibold rounded-lg px-3.5 py-2 transition';

function renderFiltrosEspecialidad(especialidades) {
    const cont = document.getElementById('filtros-especialidad');
    especialidades.forEach(esp => {
        const btn = document.createElement('button');
        btn.className = CLASE_CHIP_INACTIVO;
        btn.dataset.esp = esp.id;
        btn.innerHTML = `<i class="bi bi-heart-pulse"></i> ${esp.nombre} (${esp.totalMedicos})`;
        btn.onclick = () => filtrarPorEspecialidad(esp.id, btn);
        cont.appendChild(btn);
    });
}

function filtrarPorEspecialidad(especialidadId, btnClickeado) {
    document.querySelectorAll('#filtros-especialidad button').forEach(b => {
        b.className = CLASE_CHIP_INACTIVO;
    });
    btnClickeado.className = CLASE_CHIP_ACTIVO;
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

    let medicos;
    if (!especialidadActivaId) {
        medicos = obtenerTodosLosMedicos(todasLasEspecialidades);
    } else {
        const especialidad = todasLasEspecialidades.find(e => e.id == especialidadActivaId);
        medicos = (especialidad?.medicos || []).map(m => ({ ...m, especialidadNombre: especialidad.nombre }));
    }

    if (texto) {
        medicos = medicos.filter(m => m.nombreCompleto.toLowerCase().includes(texto));
    }

    if (diaSeleccionado) {
        medicos = medicos.filter(m =>
            (m.horarios || []).some(h => h.dia === diaSeleccionado)
        );
    }

    if (fechaSeleccionada) {
        const diaDeLaFecha = diaSemanaDeFecha(fechaSeleccionada);
        medicos = medicos.filter(m =>
            (m.horarios || []).some(h => h.dia === diaDeLaFecha)
        );
    }

    medicosFiltradosActuales = medicos;
    paginaActual = 1;
    renderPaginaActual();
}

function limpiarFiltros() {
    document.getElementById('filtro-texto').value = '';
    document.getElementById('filtro-dia').value = '';
    document.getElementById('filtro-fecha').value = '';
    especialidadActivaId = '';
    document.querySelectorAll('#filtros-especialidad button').forEach(b => b.className = CLASE_CHIP_INACTIVO);
    document.querySelector('#filtros-especialidad button[data-esp=""]').className = CLASE_CHIP_ACTIVO;
    aplicarFiltros();
}

function renderPaginaActual() {
    const totalPaginas = Math.max(1, Math.ceil(medicosFiltradosActuales.length / MEDICOS_POR_PAGINA));
    if (paginaActual > totalPaginas) paginaActual = totalPaginas;

    const inicio = (paginaActual - 1) * MEDICOS_POR_PAGINA;
    const medicosPagina = medicosFiltradosActuales.slice(inicio, inicio + MEDICOS_POR_PAGINA);

    renderMedicos(medicosPagina);
    renderPaginacion(totalPaginas);
}

function irAPagina(num) {
    paginaActual = num;
    renderPaginaActual();
    document.getElementById('grid-medicos').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function renderPaginacion(totalPaginas) {
    const cont = document.getElementById('paginacion-medicos');

    if (!medicosFiltradosActuales.length || totalPaginas <= 1) {
        cont.innerHTML = '';
        return;
    }

    const claseBase = 'w-9 h-9 flex items-center justify-center rounded-lg text-sm font-medium transition';
    const claseInactiva = `${claseBase} bg-white/10 border border-white/15 text-white/70 hover:bg-white/[0.16]`;
    const claseActiva = `${claseBase} bg-guia border border-guia text-white font-semibold`;
    const claseDeshabilitada = `${claseBase} bg-white/5 border border-white/10 text-white/25 cursor-not-allowed`;

    let html = '';

    html += `<button ${paginaActual === 1 ? 'disabled' : ''}
        onclick="irAPagina(${paginaActual - 1})"
        class="${paginaActual === 1 ? claseDeshabilitada : claseInactiva}">
        <i class="bi bi-chevron-left"></i>
    </button>`;

    for (let i = 1; i <= totalPaginas; i++) {
        html += `<button onclick="irAPagina(${i})" class="${i === paginaActual ? claseActiva : claseInactiva}">${i}</button>`;
    }

    html += `<button ${paginaActual === totalPaginas ? 'disabled' : ''}
        onclick="irAPagina(${paginaActual + 1})"
        class="${paginaActual === totalPaginas ? claseDeshabilitada : claseInactiva}">
        <i class="bi bi-chevron-right"></i>
    </button>`;

    cont.innerHTML = html;
}

function renderMedicos(medicos) {
    const cont = document.getElementById('grid-medicos');

    if (!medicos.length) {
        cont.innerHTML = `<div class="text-center text-sm text-white/60 py-10 col-span-full">No hay médicos disponibles en esta especialidad</div>`;
        return;
    }

    cont.innerHTML = medicos.map(m => {
        const iniciales = m.nombreCompleto.split(' ').slice(0, 2).map(n => n.charAt(0)).join('').toUpperCase();
        const estrellas = renderEstrellas(m.promedioValoracion);

        return `
      <div class="bg-white/[0.07] backdrop-blur-md border border-white/15 rounded-card p-5 text-center hover:bg-white/[0.12] hover:border-guia/40 transition">
        <div class="w-16 h-16 rounded-full mx-auto mb-3.5 bg-gradient-to-br from-guia to-rumbo flex items-center justify-center text-white text-xl font-bold shadow-lg shadow-black/30">${iniciales}</div>
        <div class="font-semibold text-[15px] text-white mb-0.5">${m.nombreCompleto}</div>
        <div class="text-[12.5px] text-guia font-semibold mb-2">${m.especialidadNombre}</div>
        <div class="mb-3.5">${estrellas}
          <span class="text-[11.5px] text-white/50 ml-1">(${m.totalValoraciones || 0})</span>
        </div>
        <button onclick="abrirAgendarCita(${m.id}, '${m.nombreCompleto.replace(/'/g, "\\'")}', '${m.especialidadNombre.replace(/'/g, "\\'")}')"
          class="w-full bg-guia hover:bg-guia/90 text-white text-xs font-semibold rounded-lg py-2.5 transition flex items-center justify-center gap-1.5">
          <i class="bi bi-calendar-plus"></i> Agendar cita
        </button>
      </div>`;
    }).join('');
}

function renderEstrellas(promedio) {
    const valor = promedio || 0;
    let html = '';
    for (let i = 1; i <= 5; i++) {
        html += `<i class="bi bi-star${i <= Math.round(valor) ? '-fill' : ''} text-guia text-[13px]"></i>`;
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
        '<span class="text-[13px] text-neblina">Selecciona una fecha primero</span>';

    document.getElementById('agendar-fecha').min = new Date().toISOString().split('T')[0];

    const modal = document.getElementById('modalAgendar');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function cerrarModalAgendar() {
    const modal = document.getElementById('modalAgendar');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

let slotsDisponiblesActuales = [];

const CLASE_SLOT_INACTIVO = 'border border-borde text-tinta text-xs font-medium rounded-lg px-3 py-1.5 transition hover:border-guia';
const CLASE_SLOT_ACTIVO = 'bg-guia border border-guia text-white text-xs font-semibold rounded-lg px-3 py-1.5 transition';

async function cargarSlotsDisponibles() {
    const fecha = document.getElementById('agendar-fecha').value;
    const cont = document.getElementById('agendar-slots');

    if (!fecha) return;

    cont.innerHTML = '<span class="text-[13px] text-neblina">Cargando horarios...</span>';

    try {
        const slots = await PortalService.disponibilidadSlots(medicoSeleccionadoId, fecha);
        slotsDisponiblesActuales = slots;

        if (!slots.length) {
            cont.innerHTML = '<span class="text-[13px] text-neblina">No hay horarios disponibles este día</span>';
            return;
        }

        cont.innerHTML = slots.map((s, idx) => `
      <button type="button" class="${CLASE_SLOT_INACTIVO}" onclick="elegirSlot(${idx}, this)">
        ${s.hora}
      </button>`).join('');

    } catch (err) {
        cont.innerHTML = '<span class="text-[13px] text-alerta">No se pudo cargar la disponibilidad</span>';
    }
}

function elegirSlot(idx, btnClickeado) {
    document.querySelectorAll('#agendar-slots button').forEach(b => b.className = CLASE_SLOT_INACTIVO);
    btnClickeado.className = CLASE_SLOT_ACTIVO;
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

    const especialidad = todasLasEspecialidades.find(e =>
        e.nombre.toLowerCase() === nombreSugerido.toLowerCase());

    if (especialidad) {
        const btn = document.querySelector(`#filtros-especialidad button[data-esp="${especialidad.id}"]`);
        if (btn) filtrarPorEspecialidad(especialidad.id, btn);
    }
}

cargarDirectorio().then(aplicarEspecialidadSugerida);