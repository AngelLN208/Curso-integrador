/**
 * acces-control.js — Control de accesos del sistema
 */

AuthService.requireAuth();
const usuarioActual = Auth.getUsuario();
iniciarSidebar('Accesos');

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
let usuarioEditandoId = null;

const ROL_LABEL = {
    ROLE_ADMINISTRADOR: 'Administrador',
    ROLE_RECEPCIONISTA: 'Recepcionista',
    ROLE_MEDICO: 'Médico',
};

const ROL_ESTILO = {
    ROLE_ADMINISTRADOR: 'bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400',
    ROLE_RECEPCIONISTA: 'bg-blue-500/10 text-blue-600 dark:text-blue-400',
    ROLE_MEDICO: 'bg-rumbo/10 text-rumbo',
};

// ── Paginación ────────────────────────────────────────────────
const pagerAccesos = new Paginador({
    contenedorId: 'pager-accesos',
    porPagina: 10,
    onRenderPagina: (itemsPagina) => pintarTabla(itemsPagina)
});

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

// renderTabla ordena y entrega el dataset completo al paginador.
function renderTabla(usuarios) {
    document.getElementById('total-mostrados').textContent = `${usuarios.length} cuentas`;
    const ordenados = [...usuarios].sort((a, b) => a.nombreCompleto.localeCompare(b.nombreCompleto));
    pagerAccesos.setDatos(ordenados);
}

// pintarTabla recibe SOLO los items de la página actual (ya ordenados) y los dibuja.
function pintarTabla(usuarios) {
    const tbody = document.getElementById('tabla-usuarios');

    if (!usuarios.length) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-neblina py-10 text-[13px]">No se encontraron cuentas</td></tr>`;
        return;
    }

    tbody.innerHTML = usuarios.map(u => {
        const fecha = u.creadoEn
            ? new Date(u.creadoEn).toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' })
            : '—';
        const esUsuarioActual = u.username === usuarioActual.username;

        const botonDesactivar = esUsuarioActual
            ? '<span class="text-xs text-neblina">No editable</span>'
            : `<button onclick="toggleEstado(${u.id}, ${!u.activo})"
                class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors
                ${u.activo
                ? 'bg-alerta text-white hover:opacity-90'
                : 'bg-white dark:bg-tinta border border-borde dark:border-borde-dark text-neblina hover:text-tinta dark:hover:text-white'}">
              <i class="bi bi-${u.activo ? 'slash-circle' : 'check-circle'}"></i>
              ${u.activo ? 'Desactivar' : 'Activar'}
            </button>`;

        return `<tr class="hover:bg-lienzo dark:hover:bg-tinta-dark transition-colors">
      <td class="px-4 py-3 font-semibold">
        ${u.nombreCompleto}
        ${esUsuarioActual ? '<span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-rumbo/10 text-rumbo ml-1.5">Tú</span>' : ''}
      </td>
      <td class="px-4 py-3 text-neblina">${u.username}</td>
      <td class="px-4 py-3">
        <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${ROL_ESTILO[u.rol] || 'bg-neblina/10 text-neblina'}">
          ${ROL_LABEL[u.rol] || u.rol}
        </span>
      </td>
      <td class="px-4 py-3">
        ${u.activo
                ? '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rumbo/10 text-rumbo"><i class="bi bi-check-circle"></i> Activo</span>'
                : '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-alerta/10 text-alerta"><i class="bi bi-x-circle"></i> Inactivo</span>'}
      </td>
      <td class="px-4 py-3 text-xs text-neblina font-mono">${fecha}</td>
      <td class="px-4 py-3">
        <div class="flex gap-1.5">
          <button onclick='abrirEditarCuenta(${JSON.stringify(u)})' title="Editar"
            class="p-2 rounded-lg text-neblina hover:text-blue-500 hover:bg-blue-500/10 transition-colors">
            <i class="bi bi-pencil"></i>
          </button>
          ${botonDesactivar}
        </div>
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

// ── Editar cuenta ──────────────────────────────────────────────
function abrirEditarCuenta(usuario) {
    usuarioEditandoId = usuario.id;
    const esUsuarioActual = usuario.username === usuarioActual.username;

    document.getElementById('edit-cuenta-nombre').value = usuario.nombreCompleto;
    document.getElementById('edit-cuenta-correo').value = usuario.username;
    document.getElementById('edit-cuenta-rol').value = usuario.rol;
    document.getElementById('edit-cambiar-password').checked = false;
    document.getElementById('edit-cuenta-password').value = '';
    document.getElementById('edit-password-container').classList.add('hidden');

    const selectRol = document.getElementById('edit-cuenta-rol');
    const hintRol = document.getElementById('edit-rol-hint');
    if (esUsuarioActual) {
        selectRol.disabled = true;
        hintRol.classList.remove('hidden');
    } else {
        selectRol.disabled = false;
        hintRol.classList.add('hidden');
    }

    abrirModal('modalEditarCuenta');
}

function togglePasswordEdit() {
    const marcado = document.getElementById('edit-cambiar-password').checked;
    const cont = document.getElementById('edit-password-container');
    if (marcado) {
        cont.classList.remove('hidden');
    } else {
        cont.classList.add('hidden');
        document.getElementById('edit-cuenta-password').value = '';
    }
}

async function guardarEdicionCuenta() {
    const nombreCompleto = document.getElementById('edit-cuenta-nombre').value.trim();
    const username = document.getElementById('edit-cuenta-correo').value.trim();
    const rol = document.getElementById('edit-cuenta-rol').value;
    const cambiarPassword = document.getElementById('edit-cambiar-password').checked;
    const password = document.getElementById('edit-cuenta-password').value;

    if (!nombreCompleto || !username) {
        return UI.mostrarError(new Error('Nombre y correo son obligatorios'));
    }
    if (cambiarPassword && password.length < 8) {
        return UI.mostrarError(new Error('La nueva contraseña debe tener al menos 8 caracteres'));
    }

    const body = {
        nombreCompleto,
        username,
        rol,
        password: cambiarPassword ? password : null
    };

    try {
        await apiFetch(`/admin/usuarios/${usuarioEditandoId}`, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
        UI.mostrarAlerta('Cuenta actualizada correctamente', 'success');
        cerrarModal('modalEditarCuenta');
        cargarUsuarios();

        // Si edité mi propia cuenta, actualizo los datos guardados en localStorage
        if (username === usuarioActual.username || nombreCompleto !== usuarioActual.nombreCompleto) {
            const actualizado = { ...usuarioActual, nombreCompleto, username };
            Auth.setUsuario(actualizado);
        }
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