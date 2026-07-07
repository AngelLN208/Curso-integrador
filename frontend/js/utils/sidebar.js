/**
 * sidebar.js — Sidebar y modal de perfil compartidos (Tailwind)
 * Inyecta el HTML del sidebar y el modal en cualquier página que lo incluya.
 * Requiere: config.js y api.js cargados antes.
 */
function iniciarSidebar(paginaActiva) {
  const usuario = Auth.getUsuario();
  if (!usuario) return;

  const esAdmin = usuario.rol === 'ROLE_ADMINISTRADOR';
  const esMedico = usuario.rol === 'ROLE_MEDICO';

  const navRecepcionista = [
    { href: '/views/recepcionist/dashboard.html', icon: 'bi-grid-1x2', label: 'Dashboard' },
    { href: '/views/recepcionist/appointments.html', icon: 'bi-calendar3', label: 'Citas' },
    { href: '/views/recepcionist/patients.html', icon: 'bi-people', label: 'Pacientes' },
    { href: '/views/recepcionist/payments.html', icon: 'bi-receipt', label: 'Pagos' },
  ];
  const navMedico = [
    { href: '/views/medico/dashboard.html', icon: 'bi-grid-1x2', label: 'Dashboard', section: 'Principal' },
    { href: '/views/medico/citas.html', icon: 'bi-calendar3', label: 'Mis citas', section: 'Atención' },
    { href: '/views/medico/historial-medico.html', icon: 'bi-file-medical', label: 'Hist. médico', section: 'Atención' },
    { href: '/views/medico/horario.html', icon: 'bi-clock', label: 'Mi horario', section: 'Atención' },
  ];
  const navAdmin = [
    { href: '/views/admin/dashboard.html', icon: 'bi-grid-1x2', label: 'Dashboard', section: 'Principal' },
    { href: '/views/admin/appointments.html', icon: 'bi-calendar3', label: 'Citas', section: 'Operación' },
    { href: '/views/admin/patients.html', icon: 'bi-people', label: 'Pacientes', section: 'Operación' },
    { href: '/views/admin/medicos.html', icon: 'bi-person-badge', label: 'Médicos', section: 'Operación' },
    { href: '/views/admin/payments.html', icon: 'bi-receipt', label: 'Pagos', section: 'Operación' },
    { href: '/views/admin/historial-medico.html', icon: 'bi-file-medical', label: 'Hist. médico', section: 'Operación' },
    { href: '/views/admin/audit.html', icon: 'bi-shield-check', label: 'Auditoría', section: 'Administración' },
    { href: '/views/admin/reports.html', icon: 'bi-bar-chart', label: 'Reportes', section: 'Administración' },
    { href: '/views/admin/seguros.html', icon: 'bi-shield-plus', label: 'Seguros', section: 'Administración' },
    { href: '/views/admin/acces-control.html', icon: 'bi-key', label: 'Accesos', section: 'Administración' },
  ];

  const nav = esAdmin ? navAdmin : esMedico ? navMedico : navRecepcionista;
  const usaSecciones = esAdmin || esMedico;

  let navHtml = '';
  let seccionActual = null;

  nav.forEach(item => {
    if (usaSecciones && item.section !== seccionActual) {
      seccionActual = item.section;
      navHtml += `<div class="text-[10px] font-semibold uppercase tracking-wider text-neblina/50 px-3 pt-4 pb-1.5 first:pt-1">${seccionActual}</div>`;
    }
    const activo = item.label === paginaActiva;
    navHtml += `
      <a href="${item.href}"
         class="flex items-center gap-2.5 px-3 py-2.5 text-sm rounded-none transition-colors relative
                ${activo
        ? 'border-l-[3px] border-guia bg-guia/10 text-white font-medium'
        : 'border-l-[3px] border-transparent text-neblina hover:text-white hover:bg-white/5'}">
        <i class="bi ${item.icon} text-[17px] w-5 flex-shrink-0 ${activo ? 'text-guia-light' : ''}"></i>
        <span class="truncate">${item.label}</span>
        ${activo ? '<i class="bi bi-circle-fill text-guia text-[7px] ml-auto"></i>' : ''}
      </a>`;
  });

  if (!usaSecciones) {
    navHtml = `<div class="text-[10px] font-semibold uppercase tracking-wider text-neblina/50 px-3 pt-1 pb-1.5">Principal</div>${navHtml}`;
  }

  const cargo = usuario.rol === 'ROLE_ADMINISTRADOR' ? 'Administrador' :
    usuario.rol === 'ROLE_MEDICO' ? 'Médico' : 'Recepcionista';
  const inicial = usuario.nombreCompleto.charAt(0).toUpperCase();

  document.getElementById('sidebar-placeholder').innerHTML = `
    <div class="lg:hidden fixed top-0 inset-x-0 h-14 z-30 bg-tinta dark:bg-tinta-dark flex items-center justify-between px-4">
      <div class="flex items-center gap-2">
        <i class="bi bi-compass text-guia-light text-xl"></i>
        <span class="font-display font-bold text-sm text-white">Stella Maris</span>
      </div>
      <button id="menu-toggle-btn" class="text-white text-2xl leading-none" aria-label="Abrir menú">
        <i class="bi bi-list"></i>
      </button>
    </div>

    <div id="sidebar-overlay" class="hidden fixed inset-0 bg-black/50 z-40 lg:hidden"></div>

    <aside id="app-sidebar"
           class="fixed inset-y-0 left-0 z-50 w-64 bg-tinta dark:bg-tinta-dark -translate-x-full transition-transform duration-300 ease-in-out lg:translate-x-0 lg:sticky lg:top-0 lg:h-screen flex flex-col">
      <div class="flex items-center gap-2.5 px-5 py-5 border-b border-white/10 flex-shrink-0">
        <div class="w-9 h-9 rounded-lg bg-guia/15 flex items-center justify-center flex-shrink-0">
          <i class="bi bi-compass text-guia-light text-lg"></i>
        </div>
        <div class="min-w-0">
          <div class="font-display font-bold text-[15px] text-white truncate">Stella Maris</div>
          <div class="text-[11px] text-neblina truncate">${cargo}</div>
        </div>
      </div>

      <nav class="flex-1 min-h-0 overflow-y-auto px-2 py-2 flex flex-col gap-0.5">
        ${navHtml}
      </nav>

      <div class="flex items-center gap-2.5 px-3 py-3.5 border-t border-white/10 flex-shrink-0">
        <button id="sidebar-user-btn" class="flex items-center gap-2.5 flex-1 min-w-0 text-left">
          <div class="w-8.5 h-8.5 w-9 h-9 rounded-full bg-guia flex items-center justify-center text-tinta font-display font-semibold text-[13px] flex-shrink-0">
            ${inicial}
          </div>
          <div class="min-w-0">
            <div class="text-[13px] font-medium text-white truncate">${usuario.nombreCompleto}</div>
            <div class="text-[11px] text-neblina truncate">${usuario.username}</div>
          </div>
        </button>
        <button id="btnLogout" title="Cerrar sesión" class="text-neblina hover:text-alerta hover:bg-alerta/10 rounded-lg p-2 transition-colors flex-shrink-0">
          <i class="bi bi-box-arrow-right text-lg"></i>
        </button>
      </div>
    </aside>

    <div id="modalPerfil" class="hidden fixed inset-0 bg-black/45 z-[200] items-center justify-center p-5">
      <div class="bg-white dark:bg-superficie-dark rounded-2xl w-full max-w-[420px] overflow-hidden">
        <div class="flex items-center justify-between px-6 py-5 border-b border-borde dark:border-borde-dark">
          <h3 class="font-display font-semibold text-base text-tinta dark:text-white">Mi perfil</h3>
          <button id="cerrarPerfil" class="text-neblina hover:text-alerta hover:bg-alerta/10 rounded-lg p-1.5 transition-colors">
            <i class="bi bi-x text-xl"></i>
          </button>
        </div>
        <div class="px-6 py-6 text-center">
          <div class="w-[72px] h-[72px] rounded-full bg-guia mx-auto mb-4 flex items-center justify-center text-2xl font-display font-bold text-tinta">
            ${inicial}
          </div>
          <div class="text-xl font-display font-semibold text-tinta dark:text-white mb-1">${usuario.nombreCompleto}</div>
          <div class="mb-5">
            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rumbo/10 text-rumbo">${cargo}</span>
          </div>
          <div class="grid gap-2.5 text-left">
            <div class="flex items-center gap-3 p-3 bg-lienzo dark:bg-tinta-dark rounded-lg">
              <i class="bi bi-envelope text-guia text-lg w-6"></i>
              <div class="min-w-0">
                <div class="text-[11px] text-neblina mb-0.5">Correo</div>
                <div class="text-[13px] font-medium text-tinta dark:text-white truncate">${usuario.username}</div>
              </div>
            </div>
            <div class="flex items-center gap-3 p-3 bg-lienzo dark:bg-tinta-dark rounded-lg">
              <i class="bi bi-shield-check text-rumbo text-lg w-6"></i>
              <div>
                <div class="text-[11px] text-neblina mb-0.5">Rol</div>
                <div class="text-[13px] font-medium text-tinta dark:text-white">${cargo}</div>
              </div>
            </div>
            <div class="flex items-center gap-3 p-3 bg-lienzo dark:bg-tinta-dark rounded-lg">
              <i class="bi bi-building text-blue-500 text-lg w-6"></i>
              <div>
                <div class="text-[11px] text-neblina mb-0.5">Clínica</div>
                <div class="text-[13px] font-medium text-tinta dark:text-white">Stella Maris</div>
              </div>
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-2.5 px-6 py-4 bg-lienzo dark:bg-tinta-dark border-t border-borde dark:border-borde-dark">
          <button onclick="AuthService.logout()" class="px-4 py-2 rounded-lg text-sm font-medium bg-alerta text-white hover:opacity-90 transition-opacity">
            <i class="bi bi-box-arrow-right"></i> Cerrar sesión
          </button>
          <button id="cerrarPerfil2" class="px-4 py-2 rounded-lg text-sm font-medium bg-white dark:bg-tinta border border-borde dark:border-borde-dark text-neblina hover:text-tinta dark:hover:text-white transition-colors">
            Cerrar
          </button>
        </div>
      </div>
    </div>
  `;

  document.getElementById('btnLogout').addEventListener('click', () => AuthService.logout());

  const modal = document.getElementById('modalPerfil');
  document.getElementById('sidebar-user-btn').addEventListener('click', () => {
    modal.classList.remove('hidden');
    modal.classList.add('flex');
  });
  function cerrarModalPerfil() {
    modal.classList.add('hidden');
    modal.classList.remove('flex');
  }
  document.getElementById('cerrarPerfil').addEventListener('click', cerrarModalPerfil);
  document.getElementById('cerrarPerfil2').addEventListener('click', cerrarModalPerfil);
  modal.addEventListener('click', e => { if (e.target === modal) cerrarModalPerfil(); });

  // ── Responsive: hamburguesa + overlay ──────────────────────
  const sidebarEl = document.getElementById('app-sidebar');
  const overlay = document.getElementById('sidebar-overlay');
  const btnMenu = document.getElementById('menu-toggle-btn');

  function abrirSidebarMovil() {
    sidebarEl.classList.remove('-translate-x-full');
    sidebarEl.classList.add('translate-x-0');
    overlay.classList.remove('hidden');
  }
  function cerrarSidebarMovil() {
    sidebarEl.classList.add('-translate-x-full');
    sidebarEl.classList.remove('translate-x-0');
    overlay.classList.add('hidden');
  }

  btnMenu.addEventListener('click', abrirSidebarMovil);
  overlay.addEventListener('click', cerrarSidebarMovil);
  sidebarEl.querySelectorAll('a').forEach(a => a.addEventListener('click', cerrarSidebarMovil));
}