/**
 * acces-control.js — Control de accesos del sistema
 */

AuthService.requireAuth();
const usuarioActual = Auth.getUsuario();
iniciarSidebar('Accesos');

// ── Modales (genérico) ───────────────────────────────────────
function abrirModal(id) {
    document.getElementById(id).classList.add('open');
}
function cerrarModal(id) {
    document.getElementById(id).classList.remove('open');
}

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
let todosLosUsuarios = [];

const ROL_LABEL = {
    ROLE_ADMINISTRADOR: 'Administrador',
    ROLE_RECEPCIONISTA: 'Recepcionista',
    ROLE_MEDICO: 'Médico',
};

const ROL_COLOR = {
    ROLE_ADMINISTRADOR: 'amber',
    ROLE_RECEPCIONISTA: 'indigo',
    ROLE_MEDICO: 'green',
};

// ── Cargar usuarios ───────────────────────────────────────────
async function cargarUsuarios() {
    try {
        todosLosUsuarios = await AdminService.listarUsuarios();
        calcularMetricas(todosLosUsuarios);
        renderTabla(todosLosUsuarios);
    } catch (err) {
        UI.mostrarError(err);
    }
}

function calcularMetricas(usuarios) {
    document.getElementById('m-total').textContent = usuarios.length;
    document.getElementById('m-activas').textContent = usuarios.filter(u => u.activo).length;
    document.getElementById('m-inactivas').textContent = usuarios.filter(u => !u.activo).length;
    document.getElementById('m-admins').textContent = usuarios.filter(u => u.rol === 'ROLE_ADMINISTRADOR').length;
}

function renderTabla(usuarios) {
    document.getElementById('total-mostrados').textContent = `${usuarios.length} cuentas`;
    const tbody = document.getElementById('tabla-usuarios');

    if (!usuarios.length) {
        tbody.innerHTML = `<tr><td colspan="6" class="empty-row">No se encontraron cuentas</td></tr>`;
        return;
    }

    tbody.innerHTML = usuarios.map(u => {
        const fecha = u.creadoEn
            ? new Date(u.creadoEn).toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' })
            : '—';
        const esUsuarioActual = u.username === usuarioActual.username;

        return `<tr>
      <td style="font-weight:600;color:var(--text)">
        ${u.nombreCompleto}
        ${esUsuarioActual ? '<span class="badge badge-confirmada" style="margin-left:6px;font-size:10px">Tú</span>' : ''}
      </td>
      <td style="color:var(--text-2)">${u.username}</td>
      <td>
        <span class="badge" style="background:var(--${ROL_COLOR[u.rol]}-lt);color:var(--${ROL_COLOR[u.rol]})">
          ${ROL_LABEL[u.rol] || u.rol}
        </span>
      </td>
      <td>
        ${u.activo
                ? '<span class="badge badge-confirmada"><i class="bi bi-check-circle"></i> Activo</span>'
                : '<span class="badge badge-cancelada"><i class="bi bi-x-circle"></i> Inactivo</span>'}
      </td>
      <td style="font-size:12px;color:var(--text-3)">${fecha}</td>
      <td>
        ${esUsuarioActual
                ? '<span style="font-size:12px;color:var(--text-3)">No editable</span>'
                : `<button class="btn btn-sm ${u.activo ? 'btn-red' : 'btn-secondary'}"
                      onclick="toggleEstado(${u.id}, ${!u.activo})">
              <i class="bi bi-${u.activo ? 'slash-circle' : 'check-circle'}"></i>
              ${u.activo ? 'Desactivar' : 'Activar'}
            </button>`}
      </td>
    </tr>`;
    }).join('');
}

// ── Activar / desactivar ─────────────────────────────────────
async function toggleEstado(id, nuevoEstado) {
    const accion = nuevoEstado ? 'activar' : 'desactivar';
    if (!confirm(`¿Seguro que deseas ${accion} esta cuenta?`)) return;

    try {
        await AdminService.cambiarEstadoUsuario(id, nuevoEstado);
        UI.mostrarAlerta(`Cuenta ${nuevoEstado ? 'activada' : 'desactivada'} correctamente`, 'success');
        cargarUsuarios();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ── Crear cuenta ──────────────────────────────────────────────
function abrirModalCrear() {
    document.getElementById('cuenta-nombre').value = '';
    document.getElementById('cuenta-correo').value = '';
    document.getElementById('cuenta-password').value = '';
    document.getElementById('cuenta-rol').value = 'ROLE_RECEPCIONISTA';
    abrirModal('modalCrearCuenta');
}

async function guardarCuenta() {
    const nombreCompleto = document.getElementById('cuenta-nombre').value.trim();
    const username = document.getElementById('cuenta-correo').value.trim();
    const password = document.getElementById('cuenta-password').value;
    const rol = document.getElementById('cuenta-rol').value;

    if (!nombreCompleto || !username || !password) {
        UI.mostrarError(new Error('Completa todos los campos obligatorios'));
        return;
    }
    if (password.length < 8) {
        UI.mostrarError(new Error('La contraseña debe tener al menos 8 caracteres'));
        return;
    }

    try {
        await AdminService.crearUsuario({ nombreCompleto, username, password, rol });
        UI.mostrarAlerta('Cuenta creada correctamente', 'success');
        cerrarModal('modalCrearCuenta');
        cargarUsuarios();
    } catch (err) {
        UI.mostrarError(err);
    }
}

// ── Filtros ───────────────────────────────────────────────────
function aplicarFiltros() {
    const buscar = document.getElementById('filtro-buscar').value.toLowerCase().trim();
    const rol = document.getElementById('filtro-rol').value;

    const filtrado = todosLosUsuarios.filter(u => {
        const matchBuscar = !buscar ||
            u.nombreCompleto.toLowerCase().includes(buscar) ||
            u.username.toLowerCase().includes(buscar);
        const matchRol = !rol || u.rol === rol;
        return matchBuscar && matchRol;
    });

    renderTabla(filtrado);
}

document.getElementById('filtro-buscar').addEventListener('input', aplicarFiltros);
document.getElementById('filtro-rol').addEventListener('change', aplicarFiltros);

// ── Init ──────────────────────────────────────────────────────
cargarUsuarios();