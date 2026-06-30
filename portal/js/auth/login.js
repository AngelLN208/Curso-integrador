/**
 * login.js — Lógica de inicio de sesión
 */
if (PortalAuth.isLoggedIn()) window.location.href = PORTAL_CONFIG.ROUTES.DASHBOARD;

async function manejarLogin(e) {
    e.preventDefault();
    const btn = document.getElementById('btn-login');

    const correo = document.getElementById('login-correo').value.trim();
    const password = document.getElementById('login-password').value;

    btn.disabled = true;
    btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Ingresando...';

    try {
        await PortalAuthService.login(correo, password);

    } catch (err) {
        PortalNotify.error(err.message || 'Correo o contraseña incorrectos');
        resaltarSugerenciaRegistro();
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-box-arrow-in-right"></i> Iniciar sesión';
    }
}

function resaltarSugerenciaRegistro() {
    const footer = document.querySelector('.auth-footer');
    if (!footer) return;

    // Resaltar el footer con un mensaje más explícito
    footer.style.cssText = `
        margin-top:16px;padding:12px 14px;border-radius:10px;
        background:var(--primary-lt);border:1px solid var(--primary);
        text-align:center;font-size:13px;transition:all .3s ease
    `;
    footer.innerHTML = `
        <i class="bi bi-info-circle" style="color:var(--primary);margin-right:4px"></i>
        ¿No tienes cuenta aún? <a href="registro.html" style="color:var(--primary);font-weight:700">Regístrate aquí</a>
        <span style="display:block;font-size:11.5px;color:var(--text-3);margin-top:4px">
            Si ya tienes cuenta, verifica que tu correo y contraseña sean correctos
        </span>
    `;
}