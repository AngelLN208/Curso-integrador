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

function renderTabla(datos) {
    document.getElementById('total-mostrados').textContent = `${datos.length} registros`;
    const tbody = document.getElementById('tabla-auditoria');

    if (!datos.length) {
        tbody.innerHTML = `<tr><td colspan="6" class="empty-row">
      <i class="bi bi-shield-x" style="font-size:24px;display:block;margin-bottom:8px;color:var(--text-3)"></i>
      No se encontraron registros
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

        return `<tr>
      <td style="font-size:12px;color:var(--text-2)">${fecha} <span style="color:var(--text-3)">${hora}</span></td>
      <td>${usuarioNombre}</td>
      <td>${formatearAccion(a.tipoAccion)}</td>
      <td style="color:var(--text-2)">${pacienteInfo}</td>
      <td>${a.estadoAnterior ? UI.badgeEstado(a.estadoAnterior) : '<span style="color:var(--text-3)">—</span>'}</td>
      <td>${a.estadoNuevo ? UI.badgeEstado(a.estadoNuevo) : '<span style="color:var(--text-3)">—</span>'}</td>
    </tr>`;
    }).join('');
}

function formatearAccion(tipo) {
    const map = {
        CREACION: '<span class="badge badge-confirmada"><i class="bi bi-plus-circle"></i> Creación</span>',
        CONFIRMACION: '<span class="badge badge-reprogramada"><i class="bi bi-check-circle"></i> Confirmación</span>',
        REPROGRAMACION: '<span class="badge badge-pendiente"><i class="bi bi-arrow-repeat"></i> Reprogramación</span>',
        CANCELACION: '<span class="badge badge-cancelada"><i class="bi bi-x-circle"></i> Cancelación</span>',
        ATENDIDA: '<span class="badge badge-atendida"><i class="bi bi-clipboard-check"></i> Atendida</span>',
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