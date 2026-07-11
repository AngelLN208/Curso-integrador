/**
 * registro.js — Lógica de registro de paciente
 */
if (PortalAuth.isLoggedIn()) window.location.href = PORTAL_CONFIG.ROUTES.DASHBOARD;

function validarRequisitosPassword() {
    const valor = document.getElementById('reg-password').value;

    const reglas = {
        length: valor.length >= 8 && valor.length <= 20,
        mixed: /[a-z]/.test(valor) && /[A-Z]/.test(valor),
        number: /\d/.test(valor),
        special: /[@$!%*?&]/.test(valor),
    };

    Object.entries(reglas).forEach(([key, cumple]) => {
        const el = document.querySelector(`[data-req="${key}"]`);
        const icon = el.querySelector('i');
        if (cumple) {
            el.classList.remove('text-neblina');
            el.classList.add('text-rumbo');
            icon.className = 'bi bi-check-circle-fill text-[10px]';
        } else {
            el.classList.remove('text-rumbo');
            el.classList.add('text-neblina');
            icon.className = 'bi bi-circle text-[10px]';
        }
    });
}

async function manejarRegistro(e) {
    e.preventDefault();
    const btn = document.getElementById('btn-registro');

    const password = document.getElementById('reg-password').value;
    const password2 = document.getElementById('reg-password2').value;

    if (password !== password2) {
        PortalNotify.error('Las contraseñas no coinciden');
        return;
    }

    const payload = {
        dni: document.getElementById('reg-dni').value.trim(),
        nombres: document.getElementById('reg-nombres').value.trim(),
        apellidos: document.getElementById('reg-apellidos').value.trim(),
        fechaNacimiento: document.getElementById('reg-fecha').value,
        celular: document.getElementById('reg-celular').value.trim(),
        correo: document.getElementById('reg-correo').value.trim(),
        contrasena: password,
        confirmarContrasena: password2,
        sexo: document.getElementById('reg-sexo').value,
    };

    btn.disabled = true;
    btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Creando cuenta...';

    try {
        await PortalAuthService.registrar(payload);
        PortalNotify.success('¡Cuenta creada correctamente!');
    } catch (err) {
        PortalNotify.error(err.message || 'No se pudo completar el registro');
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-person-plus"></i> Crear cuenta';
    }
}