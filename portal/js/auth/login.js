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

    // Resaltar el footer con clases Tailwind (antes: estilos inline con variables CSS de portal.css)
    footer.classList.remove('text-neblina');
    footer.classList.add(
        'bg-guia/10', 'border-guia', 'text-tinta', 'dark:text-white',
        'rounded-xl', 'p-3', '!border'
    );
    footer.innerHTML = `
        <i class="bi bi-info-circle text-guia mr-1"></i>
        ¿No tienes cuenta aún? <a href="registro.html" class="text-guia font-bold hover:underline">Regístrate aquí</a>
        <span class="block text-[11px] text-neblina mt-1">
            Si ya tienes cuenta, verifica que tu correo y contraseña sean correctos
        </span>
    `;
}