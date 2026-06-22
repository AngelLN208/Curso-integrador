/**
 * sidebar.js — Sidebar y modal de perfil compartidos
 * Inyecta el HTML del sidebar y el modal en cualquier página que lo incluya.
 * Requiere: config.js y api.js cargados antes.
 *
 * El menú se genera según el rol del usuario:
 *  - ROLE_RECEPCIONISTA / ROLE_MEDICO → menú de recepcionista (4 items)
 *  - ROLE_ADMINISTRADOR → menú de administrador (7 items, incluye todo
 *    lo de recepcionista + auditoría, reportes y control de accesos)
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

  // Construir HTML con secciones (solo admin tiene secciones múltiples)
  let navHtml = '';
  let seccionActual = null;

  const usaSecciones = esAdmin || esMedico;

  nav.forEach(item => {
    if (usaSecciones && item.section !== seccionActual) {
      seccionActual = item.section;
      navHtml += `<div class="nav-section-label">${seccionActual}</div>`;
    }
    navHtml += `
      <a href="${item.href}" class="nav-item ${item.label === paginaActiva ? 'active' : ''}">
        <i class="bi ${item.icon}"></i> ${item.label}
      </a>`;
  });

  if (!usaSecciones) {
    navHtml = `<div class="nav-section-label">Principal</div>${navHtml}`;
  }

  const cargo = usuario.rol === 'ROLE_ADMINISTRADOR' ? 'Administrador' :
    usuario.rol === 'ROLE_MEDICO' ? 'Médico' : 'Recepcionista';

  // Inyectar sidebar
  document.getElementById('sidebar-placeholder').innerHTML = `
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-logo">🏥</div>
        <div>
          <div class="sidebar-brand">Stella Maris</div>
          <div class="sidebar-role">${cargo}</div>
        </div>
      </div>
      <nav class="sidebar-nav">
        ${navHtml}
      </nav>
      <div class="sidebar-footer">
        <button id="sidebar-user-btn"
                style="background:none;border:none;cursor:pointer;display:flex;
                       align-items:center;gap:10px;flex:1;min-width:0;
                       text-align:left;padding:0">
          <div class="user-avatar">
            ${usuario.nombreCompleto.charAt(0).toUpperCase()}
          </div>
          <div class="user-info">
            <div class="user-name">${usuario.nombreCompleto}</div>
            <div class="user-email">${usuario.username}</div>
          </div>
        </button>
        <button class="btn-logout" id="btnLogout" title="Cerrar sesión">
          <i class="bi bi-box-arrow-right"></i>
        </button>
      </div>
    </aside>

    <!-- Modal Perfil -->
    <div class="modal-backdrop" id="modalPerfil">
      <div class="modal-box" style="max-width:420px">
        <div class="modal-header">
          <h3 class="modal-title">Mi perfil</h3>
          <button class="modal-close" id="cerrarPerfil"><i class="bi bi-x"></i></button>
        </div>
        <div class="modal-body" style="text-align:center">
          <div id="perfil-avatar"
               style="width:72px;height:72px;border-radius:50%;
                      background:linear-gradient(135deg,var(--indigo),var(--blue));
                      display:flex;align-items:center;justify-content:center;
                      font-size:28px;font-weight:700;color:white;
                      margin:0 auto 16px;
                      box-shadow:0 4px 16px rgba(99,102,241,.35)">
            ${usuario.nombreCompleto.charAt(0).toUpperCase()}
          </div>
          <div style="font-size:20px;font-weight:700;color:var(--text);margin-bottom:4px">
            ${usuario.nombreCompleto}
          </div>
          <div style="margin-bottom:20px">
            <span class="badge badge-confirmada">${cargo}</span>
          </div>
          <div style="display:grid;gap:10px;text-align:left">
            <div style="display:flex;align-items:center;gap:12px;padding:12px;
                        background:var(--bg-main);border-radius:10px">
              <i class="bi bi-envelope" style="color:var(--indigo);font-size:18px;width:24px"></i>
              <div>
                <div style="font-size:11px;color:var(--text-3);margin-bottom:1px">Correo</div>
                <div style="font-size:13px;font-weight:500;color:var(--text)">
                  ${usuario.username}
                </div>
              </div>
            </div>
            <div style="display:flex;align-items:center;gap:12px;padding:12px;
                        background:var(--bg-main);border-radius:10px">
              <i class="bi bi-shield-check" style="color:var(--green);font-size:18px;width:24px"></i>
              <div>
                <div style="font-size:11px;color:var(--text-3);margin-bottom:1px">Rol</div>
                <div style="font-size:13px;font-weight:500;color:var(--text)">${cargo}</div>
              </div>
            </div>
            <div style="display:flex;align-items:center;gap:12px;padding:12px;
                        background:var(--bg-main);border-radius:10px">
              <i class="bi bi-building" style="color:var(--blue);font-size:18px;width:24px"></i>
              <div>
                <div style="font-size:11px;color:var(--text-3);margin-bottom:1px">Clínica</div>
                <div style="font-size:13px;font-weight:500;color:var(--text)">Stella Maris</div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-red" onclick="AuthService.logout()">
            <i class="bi bi-box-arrow-right"></i> Cerrar sesión
          </button>
          <button class="btn btn-ghost" id="cerrarPerfil2">Cerrar</button>
        </div>
      </div>
    </div>
  `;

  // Eventos
  document.getElementById('btnLogout').addEventListener('click', () => AuthService.logout());

  document.getElementById('sidebar-user-btn').addEventListener('click', () =>
    document.getElementById('modalPerfil').classList.add('open'));

  document.getElementById('cerrarPerfil').addEventListener('click', () =>
    document.getElementById('modalPerfil').classList.remove('open'));

  document.getElementById('cerrarPerfil2').addEventListener('click', () =>
    document.getElementById('modalPerfil').classList.remove('open'));

  document.getElementById('modalPerfil').addEventListener('click', e => {
    if (e.target === document.getElementById('modalPerfil'))
      document.getElementById('modalPerfil').classList.remove('open');
  });
}