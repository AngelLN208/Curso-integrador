/**
 * reset-password.js — Restablecer contraseña con token (staff)
 * Requiere: config.js, api.js
 */

const params = new URLSearchParams(window.location.search);
const token = params.get('token');

const vistaFormulario = document.getElementById('vista-formulario');
const vistaExito = document.getElementById('vista-exito');
const vistaInvalido = document.getElementById('vista-invalido');

// Si no hay token en la URL, mostrar directamente la vista de enlace inválido
if (!token) {
    vistaFormulario.classList.add('hidden');
    vistaInvalido.classList.remove('hidden');
}

const form = document.getElementById('resetForm');
const btnRestablecer = document.getElementById('btnRestablecer');
const errorMsg = document.getElementById('error-msg');
const errorTxt = document.getElementById('error-text');
const btnText = btnRestablecer.querySelector('.btn-text');
const spinner = btnRestablecer.querySelector('.spinner');

form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    ocultarError();

    const nuevaPassword = document.getElementById('nuevaPassword').value;
    const confirmarPassword = document.getElementById('confirmarPassword').value;

    if (!nuevaPassword || nuevaPassword.length < 6)
        return mostrarError('La contraseña debe tener al menos 6 caracteres');
    if (nuevaPassword !== confirmarPassword)
        return mostrarError('Las contraseñas no coinciden');

    setCargando(true);

    try {
        await apiFetch('/auth/reset-password', {
            method: 'POST',
            body: JSON.stringify({ token, nuevaPassword, confirmarPassword })
        });

        vistaFormulario.classList.add('hidden');
        vistaExito.classList.remove('hidden');
    } catch (err) {
        const msg = err.message || '';
        if (msg.toLowerCase().includes('no es válido') || msg.toLowerCase().includes('expirado')) {
            vistaFormulario.classList.add('hidden');
            vistaInvalido.classList.remove('hidden');
        } else {
            mostrarError(msg || 'No se pudo restablecer la contraseña. Intenta de nuevo.');
        }
    } finally {
        setCargando(false);
    }
});

// Toggle mostrar/ocultar contraseñas
function togglePasswordVisibility(inputId, iconId) {
    const input = document.getElementById(inputId);
    const icon = document.getElementById(iconId);
    const show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
}
document.getElementById('toggle1')?.addEventListener('click', () =>
    togglePasswordVisibility('nuevaPassword', 'toggle-icon1'));
document.getElementById('toggle2')?.addEventListener('click', () =>
    togglePasswordVisibility('confirmarPassword', 'toggle-icon2'));

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
    btnRestablecer.disabled = cargando;
    if (cargando) {
        btnText.textContent = 'Restableciendo...';
        spinner.classList.remove('hidden');
    } else {
        btnText.textContent = 'Restablecer contraseña';
        spinner.classList.add('hidden');
    }
}