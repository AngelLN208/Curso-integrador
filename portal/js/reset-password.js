// Extraer el token de la URL
const params = new URLSearchParams(window.location.search);
const token = params.get('token');

if (!token) {
    document.getElementById('form-reset').classList.add('hidden');
    const tokenInvalido = document.getElementById('token-invalido');
    tokenInvalido.classList.remove('hidden');
    tokenInvalido.classList.add('flex');
}

async function resetearPassword() {
    const nuevaPassword = document.getElementById('reset-password').value;
    const confirmar = document.getElementById('reset-password-confirm').value;

    if (nuevaPassword.length < 6) {
        return PortalNotify.error('La contraseña debe tener al menos 6 caracteres');
    }
    if (nuevaPassword !== confirmar) {
        return PortalNotify.error('Las contraseñas no coinciden');
    }

    const btn = document.getElementById('btn-reset');
    btn.disabled = true;
    btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Procesando...';

    try {
        await portalFetch('/portal/auth/reset-password', {
            method: 'POST',
            body: JSON.stringify({
                token,
                nuevaPassword,
                confirmarPassword: confirmar
            })
        });

        document.getElementById('form-reset').classList.add('hidden');
        const exitoso = document.getElementById('reset-exitoso');
        exitoso.classList.remove('hidden');
        exitoso.classList.add('flex');

    } catch (err) {
        if (err.message?.includes('expirado') || err.message?.includes('válido')) {
            document.getElementById('form-reset').classList.add('hidden');
            const tokenInvalido = document.getElementById('token-invalido');
            tokenInvalido.classList.remove('hidden');
            tokenInvalido.classList.add('flex');
        } else {
            PortalNotify.error(err.message || 'No se pudo restablecer la contraseña');
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-shield-check"></i> Establecer nueva contraseña';
        }
    }
}