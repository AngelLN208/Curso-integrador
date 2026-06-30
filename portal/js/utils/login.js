/**
 * login.js — Lógica de inicio de sesión
 */
if (PortalAuth.isLoggedIn()) window.location.href = PORTAL_CONFIG.ROUTES.DASHBOARD;

async function manejarLogin(e) {
  e.preventDefault();
  const btn = document.getElementById('btn-login');

  const correo   = document.getElementById('login-correo').value.trim();
  const password = document.getElementById('login-password').value;

  btn.disabled = true;
  btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Ingresando...';

  try {
    await PortalAuthService.login(correo, password);
  } catch (err) {
    PortalNotify.error(err.message || 'Correo o contraseña incorrectos');
    btn.disabled = false;
    btn.innerHTML = '<i class="bi bi-box-arrow-in-right"></i> Iniciar sesión';
  }
}