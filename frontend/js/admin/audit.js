/**
 * audit.js — Auditoría del sistema
 */

AuthService.requireAuth();
iniciarSidebar('Auditoría');

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
let todaAuditoria = [];

// ── Paginación ────────────────────────────────────────────────
const pagerAuditoria = new Paginador({
    contenedorId: 'pager-auditoria',
    porPagina: 10,
    onRenderPagina: (itemsPagina) => pintarTabla(itemsPagina)
});

// ── Cargar usuarios para el filtro ───────────────────────────
async function cargarUsuariosFiltro() {
    try {
        const usuarios = await AdminService.listarUsuarios();
        const sel = document.getElementById('filtro-usuario');
        usuarios.forEach(u => sel.insertAdjacentHTML('beforeend',
            `<option value="${u.id}">${u.nombreCompleto}</option>`));
    } catch (err) { console.error('Error cargando usuarios:', err); }
}

// ── Cargar auditoría ──────────────────────────────────────────
async function cargarAuditoria() {
    try {
        const datos = await AdminService.filtrarAuditoria(null, null);
        todaAuditoria = datos.sort((a, b) => new Date(b.fechaAccion) - new Date(a.fechaAccion));
        calcularMetricas(todaAuditoria);
        renderTabla(todaAuditoria);
    } catch (err) { UI.mostrarError(err); }
}

function calcularMetricas(datos) {
    document.getElementById('m-creaciones').textContent = datos.filter(a => a.tipoAccion === 'CREACION').length;
    document.getElementById('m-confirmaciones').textContent = datos.filter(a => a.tipoAccion === 'CONFIRMACION').length;
    document.getElementById('m-reprogramaciones').textContent = datos.filter(a => a.tipoAccion === 'REPROGRAMACION').length;
    document.getElementById('m-cancelaciones').textContent = datos.filter(a => a.tipoAccion === 'CANCELACION').length;
}

// renderTabla entrega el dataset (ya ordenado desde cargarAuditoria) al paginador.
function renderTabla(datos) {
    document.getElementById('total-mostrados').textContent = `${datos.length} registros`;
    pagerAuditoria.setDatos(datos);
}

// pintarTabla recibe SOLO los items de la página actual y los dibuja.
function pintarTabla(datos) {
    const tbody = document.getElementById('tabla-auditoria');

    if (!datos.length) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-10 text-[13px]">
      <i class="bi bi-shield-x text-2xl block mb-2 text-neblina"></i>
      <span class="text-neblina">No se encontraron registros</span>
    </td></tr>`;
        return;
    }

    tbody.innerHTML = datos.map(a => {
        const fecha = new Date(a.fechaAccion).toLocaleDateString('es-PE',
            { day: '2-digit', month: '2-digit', year: 'numeric' });
        const hora = new Date(a.fechaAccion).toLocaleTimeString('es-PE',
            { hour: '2-digit', minute: '2-digit' });
        const usuarioNombre = a.usuarioNombre || 'Sistema';
        const pacienteInfo = a.pacienteNombre || `Cita #${a.citaId || '—'}`;

        return `<tr class="hover:bg-lienzo dark:hover:bg-tinta-dark transition-colors">
      <td class="px-4 py-3 text-xs font-mono text-neblina">${fecha} <span class="opacity-70">${hora}</span></td>
      <td class="px-4 py-3">${usuarioNombre}</td>
      <td class="px-4 py-3">${formatearAccion(a.tipoAccion)}</td>
      <td class="px-4 py-3 text-neblina">${pacienteInfo}</td>
      <td class="px-4 py-3">${a.estadoAnterior ? UI.badgeEstado(a.estadoAnterior) : '<span class="text-neblina">—</span>'}</td>
      <td class="px-4 py-3">${a.estadoNuevo ? UI.badgeEstado(a.estadoNuevo) : '<span class="text-neblina">—</span>'}</td>
    </tr>`;
    }).join('');
}

function formatearAccion(tipo) {
    const map = {
        CREACION: '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rumbo/10 text-rumbo"><i class="bi bi-plus-circle"></i> Creación</span>',
        CONFIRMACION: '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-500/10 text-blue-600 dark:text-blue-400"><i class="bi bi-check-circle"></i> Confirmación</span>',
        REPROGRAMACION: '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-guia/10 text-guia"><i class="bi bi-arrow-repeat"></i> Reprogramación</span>',
        CANCELACION: '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-alerta/10 text-alerta"><i class="bi bi-x-circle"></i> Cancelación</span>',
        ATENDIDA: '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-purple-500/10 text-purple-600 dark:text-purple-400"><i class="bi bi-clipboard-check"></i> Atendida</span>',
    };
    return map[tipo] || tipo;
}

// ── Filtros ───────────────────────────────────────────────────
function aplicarFiltros() {
    const usuarioId = document.getElementById('filtro-usuario').value;
    const accion = document.getElementById('filtro-accion').value;
    const paciente = document.getElementById('filtro-paciente').value.toLowerCase().trim();

    const filtrado = todaAuditoria.filter(a => {
        const matchUsuario = !usuarioId || String(a.usuarioId) === usuarioId;
        const matchAccion = !accion || a.tipoAccion === accion;
        const nombrePaciente = (a.pacienteNombre || '').toLowerCase();
        const matchPaciente = !paciente || nombrePaciente.includes(paciente);
        return matchUsuario && matchAccion && matchPaciente;
    });

    renderTabla(filtrado);
}

function limpiarFiltros() {
    document.getElementById('filtro-usuario').value = '';
    document.getElementById('filtro-accion').value = '';
    document.getElementById('filtro-paciente').value = '';
    renderTabla(todaAuditoria);
}

document.getElementById('filtro-usuario').addEventListener('change', aplicarFiltros);
document.getElementById('filtro-accion').addEventListener('change', aplicarFiltros);
document.getElementById('filtro-paciente').addEventListener('input', aplicarFiltros);

// ── Descargar reporte PDF ─────────────────────────────────────
async function descargarReportePdf() {
    const usuarioId = document.getElementById('filtro-usuario').value;
    const accion = document.getElementById('filtro-accion').value;

    const params = new URLSearchParams();
    if (usuarioId) params.append('usuarioId', usuarioId);
    if (accion) params.append('tipoAccion', accion);

    try {
        const response = await fetch(`${CONFIG.API_URL}/admin/auditoria/reporte-pdf?${params.toString()}`, {
            headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
        });

        if (!response.ok) throw new Error('No se pudo generar el reporte PDF');

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `reporte-auditoria-${new Date().toISOString().slice(0, 10)}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ── Init ──────────────────────────────────────────────────────
cargarUsuariosFiltro();
cargarAuditoria();