/**
 * login.js — Lógica del formulario de inicio de sesión
 * Requiere: config.js, api.js
 */

const form = document.getElementById('loginForm');
const btnLogin = document.getElementById('btnLogin');
const errorMsg = document.getElementById('error-msg');
const errorTxt = document.getElementById('error-text');

// ── Submit ────────────────────────────────────────────────────
form.addEventListener('submit', async (e) => {
    e.preventDefault();
    ocultarError();

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    // Validaciones cliente
    if (!email) return mostrarError('El correo es obligatorio');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
        return mostrarError('El correo no tiene un formato válido');
    if (!password) return mostrarError('La contraseña es obligatoria');

    // Loading
    btnLogin.classList.add('loading');
    btnLogin.disabled = true;

    try {
        await AuthService.login(email, password);
        // AuthService redirige automáticamente según el rol
    } catch (err) {
        mostrarError(err.message || 'Credenciales incorrectas. Intenta de nuevo.');
    } finally {
        btnLogin.classList.remove('loading');
        btnLogin.disabled = false;
    }
});

// ── Toggle contraseña ─────────────────────────────────────────
document.getElementById('togglePassword').addEventListener('click', () => {
    const input = document.getElementById('password');
    const icon = document.getElementById('toggle-icon');
    const show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
});

// ── Helpers ───────────────────────────────────────────────────
function mostrarError(msg) {
    errorTxt.textContent = msg;
    errorMsg.classList.add('visible');
}
function ocultarError() {
    errorMsg.classList.remove('visible');
}