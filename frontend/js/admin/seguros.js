/**
 * seguros.js — Catálogo de seguros médicos (admin)
 */

AuthService.requireAuth();
iniciarSidebar('Seguros');

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
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-neblina py-10 text-[13px]">No hay seguros registrados</td></tr>`;
        return;
    }

    tbody.innerHTML = seguros.map(s => `
    <tr class="hover:bg-lienzo dark:hover:bg-tinta-dark transition-colors">
      <td class="px-4 py-3 font-semibold">${s.nombre}</td>
      <td class="px-4 py-3">
        <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${s.tipo === 'PUBLICO' ? 'bg-rumbo/10 text-rumbo' : 'bg-blue-500/10 text-blue-600 dark:text-blue-400'}">
          ${s.tipo === 'PUBLICO' ? 'Público' : 'Privado'}
        </span>
      </td>
      <td class="px-4 py-3 font-semibold text-rumbo">${s.porcentajeCobertura}%</td>
      <td class="px-4 py-3 text-neblina font-mono">S/ ${parseFloat(s.deducible || 0).toFixed(2)}</td>
      <td class="px-4 py-3">
        ${s.convenioActivo
            ? '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rumbo/10 text-rumbo"><i class="bi bi-check-circle"></i> Activo</span>'
            : '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-alerta/10 text-alerta"><i class="bi bi-x-circle"></i> Inactivo</span>'}
      </td>
      <td class="px-4 py-3">
        ${s.convenioActivo
            ? `<button onclick="desactivarSeguro(${s.id}, '${s.nombre}')"
               class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-alerta text-white hover:opacity-90 transition-opacity">
               <i class="bi bi-slash-circle"></i> Desactivar
             </button>`
            : `<button onclick="activarSeguro(${s.id}, '${s.nombre}')"
               class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-white dark:bg-tinta border border-borde dark:border-borde-dark text-neblina hover:text-tinta dark:hover:text-white transition-colors">
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