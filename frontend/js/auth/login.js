/**
 * login.js — Lógica del formulario de inicio de sesión
 * Requiere: config.js, api.js
 */

const form = document.getElementById('loginForm');
const btnLogin = document.getElementById('btnLogin');
const errorMsg = document.getElementById('error-msg');
const errorTxt = document.getElementById('error-text');
const btnText = btnLogin.querySelector('.btn-text');
const spinner = btnLogin.querySelector('.spinner');

// ── Submit ────────────────────────────────────────────────────
form.addEventListener('submit', async (e) => {
    e.preventDefault();
    ocultarError();

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    if (!email) return mostrarError('El correo es obligatorio');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
        return mostrarError('El correo no tiene un formato válido');
    if (!password) return mostrarError('La contraseña es obligatoria');

    setCargando(true);

    try {
        await AuthService.login(email, password);
        // AuthService redirige automáticamente según el rol
    } catch (err) {
        mostrarError(err.message || 'Credenciales incorrectas. Intenta de nuevo.');
    } finally {
        setCargando(false);
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
    errorMsg.classList.remove('hidden');
    errorMsg.classList.add('flex');
}
function ocultarError() {
    errorMsg.classList.add('hidden');
    errorMsg.classList.remove('flex');
}
function setCargando(cargando) {
    btnLogin.disabled = cargando;
    if (cargando) {
        btnText.textContent = 'Ingresando...';
        spinner.classList.remove('hidden');
    } else {
        btnText.textContent = 'Iniciar sesión';
        spinner.classList.add('hidden');
    }
}