/**
 * seguros.js — Catálogo de seguros médicos (admin)
 */

AuthService.requireAuth();
iniciarSidebar('Seguros');

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

// ── Cargar seguros ─────────────────────────────────────────────
async function cargarSeguros() {
    try {
        const seguros = await SeguroService.listar();
        calcularMetricas(seguros);
        renderTabla(seguros);
    } catch (err) {
        UI.mostrarError(err);
    }
}

function calcularMetricas(seguros) {
    document.getElementById('m-total').textContent = seguros.length;
    document.getElementById('m-activos').textContent = seguros.filter(s => s.convenioActivo).length;

    if (seguros.length) {
        const promedio = seguros.reduce((sum, s) => sum + parseFloat(s.porcentajeCobertura), 0) / seguros.length;
        document.getElementById('m-cobertura-promedio').textContent = `${promedio.toFixed(0)}%`;
    } else {
        document.getElementById('m-cobertura-promedio').textContent = '—';
    }
}

function renderTabla(seguros) {
    document.getElementById('total-mostrados').textContent = `${seguros.length} seguros`;
    const tbody = document.getElementById('tabla-seguros');

    if (!seguros.length) {
        tbody.innerHTML = `<tr><td colspan="6" class="empty-row">No hay seguros registrados</td></tr>`;
        return;
    }

    tbody.innerHTML = seguros.map(s => `
    <tr>
      <td style="font-weight:600;color:var(--text)">${s.nombre}</td>
      <td>
        <span class="badge ${s.tipo === 'PUBLICO' ? 'badge-confirmada' : 'badge-reprogramada'}">
          ${s.tipo === 'PUBLICO' ? 'Público' : 'Privado'}
        </span>
      </td>
      <td style="color:var(--green);font-weight:600">${s.porcentajeCobertura}%</td>
      <td style="color:var(--text-2)">S/ ${parseFloat(s.deducible || 0).toFixed(2)}</td>
      <td>
        ${s.convenioActivo
            ? '<span class="badge badge-confirmada"><i class="bi bi-check-circle"></i> Activo</span>'
            : '<span class="badge badge-cancelada"><i class="bi bi-x-circle"></i> Inactivo</span>'}
      </td>
      <td>
        ${s.convenioActivo
            ? `<button class="btn btn-sm btn-red" onclick="desactivarSeguro(${s.id}, '${s.nombre}')">
               <i class="bi bi-slash-circle"></i> Desactivar
             </button>`
            : `<button class="btn btn-sm btn-secondary" onclick="activarSeguro(${s.id}, '${s.nombre}')">
               <i class="bi bi-check-circle"></i> Activar
             </button>`}
      </td>
    </tr>`).join('');
}

// ── Crear seguro ───────────────────────────────────────────────
function abrirModalCrear() {
    document.getElementById('seg-nombre').value = '';
    document.getElementById('seg-tipo').value = 'PRIVADO';
    document.getElementById('seg-cobertura').value = '';
    document.getElementById('seg-deducible').value = '';
    abrirModal('modalCrearSeguro');
}

async function guardarSeguro() {
    const nombre = document.getElementById('seg-nombre').value.trim();
    const tipo = document.getElementById('seg-tipo').value;
    const cobertura = document.getElementById('seg-cobertura').value;
    const deducible = document.getElementById('seg-deducible').value;

    if (!nombre) return UI.mostrarError(new Error('El nombre del seguro es obligatorio'));
    if (!cobertura || cobertura < 0 || cobertura > 100)
        return UI.mostrarError(new Error('La cobertura debe ser un porcentaje entre 0 y 100'));

    try {
        await AdminService.registrarSeguro({
            nombre,
            tipo,
            porcentajeCobertura: parseFloat(cobertura),
            deducible: deducible ? parseFloat(deducible) : 0
        });
        UI.mostrarAlerta('Seguro creado correctamente', 'success');
        cerrarModal('modalCrearSeguro');
        cargarSeguros();
    } catch (err) {
        UI.mostrarError(err);
    }
}
async function activarSeguro(id, nombre) {
    if (!confirm(`¿Reactivar el convenio "${nombre}"? Volverá a estar disponible para vincular a pacientes.`)) return;

    try {
        await SeguroService.activarSeguro(id);
        UI.mostrarAlerta('Seguro reactivado correctamente', 'success');
        cargarSeguros();
    } catch (err) {
        UI.mostrarError(err);
    }
}
// ── Desactivar seguro ───────────────────────────────────────────
async function desactivarSeguro(id, nombre) {
    if (!confirm(`¿Desactivar el convenio "${nombre}"? Ya no podrá vincularse a nuevos pacientes.`)) return;

    try {
        await apiFetch(`/admin/seguros/${id}`, { method: 'DELETE' });
        UI.mostrarAlerta('Seguro desactivado correctamente', 'success');
        cargarSeguros();
    } catch (err) {
        UI.mostrarError(err);
    }
}

cargarSeguros();