/**
 * perfil.js — Edición de perfil del paciente (portal)
 */

PortalAuthService.requireAuth();
const pacienteActualPerfil = PortalAuth.getPaciente();
document.getElementById('saludo-usuario').textContent = pacienteActualPerfil?.nombreCompleto?.split(' ')[0] || '';

let correoOriginal = '';

async function cargarPerfil() {
    try {
        const perfil = await PortalService.obtenerPerfil();

        document.getElementById('perfil-dni').value = perfil.dni;
        document.getElementById('perfil-nombres').value = perfil.nombres;
        document.getElementById('perfil-apellidos').value = perfil.apellidos;
        document.getElementById('perfil-fecha-nacimiento').value = perfil.fechaNacimiento;
        document.getElementById('perfil-sexo').value = perfil.sexo;
        document.getElementById('perfil-celular').value = perfil.celular;
        document.getElementById('perfil-correo').value = perfil.correo;

        correoOriginal = perfil.correo;

        document.getElementById('perfil-loading').style.display = 'none';
        document.getElementById('form-perfil').style.display = 'block';

        renderSeguros(perfil.seguros || []);

    } catch (err) {
        document.getElementById('perfil-loading').innerHTML =
            '<span style="color:var(--red)">No se pudo cargar tu perfil</span>';
    }
}

function renderSeguros(seguros) {
    const cardSeguros = document.getElementById('card-seguros');
    const cont = document.getElementById('lista-seguros');

    if (!seguros.length) {
        cardSeguros.style.display = 'none';
        return;
    }

    cardSeguros.style.display = 'block';
    cont.innerHTML = seguros.map(s => `
      <div style="display:flex;justify-content:space-between;align-items:center;padding:10px 0;
                  border-bottom:1px solid var(--border)">
        <div>
          <div style="font-weight:600;font-size:13.5px">${s.nombre}</div>
          <div style="font-size:11.5px;color:var(--text-3)">${s.tipo} ${s.numeroPoliza ? '— Póliza: ' + s.numeroPoliza : ''}</div>
        </div>
        <span style="font-size:12px;font-weight:700;color:var(--green)">
          ${s.porcentajeCobertura}% cobertura
        </span>
      </div>`).join('');
}

document.getElementById('form-perfil').addEventListener('submit', async function (e) {
    e.preventDefault();

    const correoNuevo = document.getElementById('perfil-correo').value.trim();
    const cambioCorreo = correoNuevo.toLowerCase() !== correoOriginal.toLowerCase();

    const datos = {
        nombres: document.getElementById('perfil-nombres').value.trim(),
        apellidos: document.getElementById('perfil-apellidos').value.trim(),
        fechaNacimiento: document.getElementById('perfil-fecha-nacimiento').value,
        sexo: document.getElementById('perfil-sexo').value,
        celular: document.getElementById('perfil-celular').value.trim(),
        correo: correoNuevo
    };

    const btn = document.getElementById('btn-guardar-perfil');
    btn.disabled = true;
    btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Guardando...';

    try {
        await PortalService.actualizarPerfil(datos);

        if (cambioCorreo) {
            // El correo es el username del login — el token actual queda
            // inválido para futuras peticiones, así que forzamos un nuevo login.
            PortalNotify.success('Perfil actualizado. Por seguridad, vuelve a iniciar sesión con tu nuevo correo.');
            setTimeout(() => {
                PortalAuthService.logout();
            }, 2000);
        } else {
            // Sincronizamos el nombre cacheado en localStorage para que el
            // saludo del header refleje el cambio sin necesidad de re-login.
            PortalAuth.setPaciente({
                username: pacienteActualPerfil.username,
                nombreCompleto: datos.nombres + ' ' + datos.apellidos
            });
            document.getElementById('saludo-usuario').textContent = datos.nombres.split(' ')[0];

            PortalNotify.success('Perfil actualizado correctamente');
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-check-circle"></i> Guardar cambios';
        }

    } catch (err) {
        PortalNotify.error(err.message || 'No se pudo actualizar el perfil');
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-circle"></i> Guardar cambios';
    }
});

async function cambiarPassword() {
    const actual = document.getElementById('pwd-actual').value;
    const nueva = document.getElementById('pwd-nueva').value;
    const confirmacion = document.getElementById('pwd-confirmacion').value;

    if (!actual || !nueva || !confirmacion) {
        return PortalNotify.error('Completa todos los campos de contraseña');
    }

    if (nueva.length < 6) {
        return PortalNotify.error('La nueva contraseña debe tener al menos 6 caracteres');
    }

    if (nueva !== confirmacion) {
        return PortalNotify.error('La nueva contraseña y su confirmación no coinciden');
    }

    const btn = document.getElementById('btn-cambiar-pwd');
    btn.disabled = true;
    btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Cambiando...';

    try {
        await PortalService.cambiarPassword({
            passwordActual: actual,
            passwordNueva: nueva,
            passwordNuevaConfirmacion: confirmacion
        });

        PortalNotify.success('Contraseña actualizada correctamente');
        document.getElementById('pwd-actual').value = '';
        document.getElementById('pwd-nueva').value = '';
        document.getElementById('pwd-confirmacion').value = '';

    } catch (err) {
        PortalNotify.error(err.message || 'No se pudo cambiar la contraseña');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-shield-check"></i> Cambiar contraseña';
    }
}

function togglePwd(inputId, btn) {
    const input = document.getElementById(inputId);
    const icon = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        icon.className = 'bi bi-eye-slash';
    } else {
        input.type = 'password';
        icon.className = 'bi bi-eye';
    }
}
cargarPerfil();