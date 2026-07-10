/**
 * forgot-password.js — Solicitud de recuperación de contraseña (staff)
 * Requiere: config.js, api.js
 */

const form = document.getElementById('forgotForm');
const btnEnviar = document.getElementById('btnEnviar');
const errorMsg = document.getElementById('error-msg');
const errorTxt = document.getElementById('error-text');
const btnText = btnEnviar.querySelector('.btn-text');
const spinner = btnEnviar.querySelector('.spinner');

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    ocultarError();

    const correo = document.getElementById('correo').value.trim();

    if (!correo) return mostrarError('El correo es obligatorio');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo))
        return mostrarError('El correo no tiene un formato válido');

    setCargando(true);

    try {
        await apiFetch('/auth/recuperar-password', {
            method: 'POST',
            body: JSON.stringify({ correo })
        });

        document.getElementById('correo-enviado').textContent = correo;
        document.getElementById('vista-solicitar').classList.add('hidden');
        document.getElementById('vista-enviado').classList.remove('hidden');
    } catch (err) {
        mostrarError(err.message || 'No se pudo procesar la solicitud. Intenta de nuevo.');
    } finally {
        setCargando(false);
    }
});

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
    btnEnviar.disabled = cargando;
    if (cargando) {
        btnText.textContent = 'Enviando...';
        spinner.classList.remove('hidden');
    } else {
        btnText.textContent = 'Enviar enlace';
        spinner.classList.add('hidden');
    }
}