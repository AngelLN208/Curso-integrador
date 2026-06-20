/**
 * perfil.js — Modal de perfil compartido entre todas las vistas
 * Requiere: api.js cargado antes, y que el HTML tenga el modal #modalPerfil
 * y el botón #sidebar-user-btn
 */
function iniciarPerfil() {
  const usuario = Auth.getUsuario();
  if (!usuario) return;

  const btn = document.getElementById('sidebar-user-btn');
  if (!btn) return;

  btn.addEventListener('click', () => {
    document.getElementById('perfil-avatar').textContent =
      usuario.nombreCompleto.charAt(0).toUpperCase();
    document.getElementById('perfil-nombre').textContent = usuario.nombreCompleto;
    document.getElementById('perfil-correo').textContent = usuario.username;
    document.getElementById('perfil-cargo').textContent  =
      usuario.rol === 'ROLE_ADMINISTRADOR' ? 'Administrador' :
      usuario.rol === 'ROLE_MEDICO'        ? 'Médico' : 'Recepcionista';
    document.getElementById('perfil-rol').textContent =
      usuario.rol.replace('ROLE_', '').charAt(0) +
      usuario.rol.replace('ROLE_', '').slice(1).toLowerCase();
    document.getElementById('modalPerfil').classList.add('open');
  });

  document.getElementById('cerrarPerfil').addEventListener('click', () =>
    document.getElementById('modalPerfil').classList.remove('open'));

  document.getElementById('modalPerfil').addEventListener('click', e => {
    if (e.target === document.getElementById('modalPerfil'))
      document.getElementById('modalPerfil').classList.remove('open');
  });
}