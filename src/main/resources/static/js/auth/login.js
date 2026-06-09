document.getElementById('loginForm').addEventListener('submit', handleLogin);

async function handleLogin(event) {
    event.preventDefault();

    const username = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const btnLogin = document.querySelector('.btn-login');

    // Validaciones
    if (isEmpty(username)) {
        mostrarError('El correo es obligatorio');
        return;
    }
    if (!isValidEmail(username)) {
        mostrarError('El correo no tiene un formato válido');
        return;
    }
    if (isEmpty(password)) {
        mostrarError('La contraseña es obligatoria');
        return;
    }

    // Loading
    btnLogin.disabled = true;
    btnLogin.textContent = 'Iniciando sesión...';

    try {
        const data = await login(username, password);

        // Guarda token y datos del usuario
        localStorage.setItem('token', data.token);
        localStorage.setItem('usuario', JSON.stringify({
            username:       data.username,        // correo
            nombreCompleto: data.nombreCompleto,  // para mostrar en el dashboard "Bienvenida, María"
            rol:            data.rol              // ROLE_RECEPCIONISTA / ROLE_MEDICO / ROLE_ADMINISTRADOR
        }));

        // Redirige según rol
        redirigirPorRol(data.rol);

    } catch (err) {
        mostrarError(err.message);
    } finally {
        btnLogin.disabled = false;
        btnLogin.textContent = 'Iniciar sesión';
    }
}

function redirigirPorRol(rol) {
    const rutas = {
        'ROLE_RECEPCIONISTA': '/src/main/resources/static/views/recepcionist/dashboard.html',
        'ROLE_MEDICO':        '/src/main/resources/static/views/medico/dashboard.html',
        'ROLE_ADMINISTRADOR': '/src/main/resources/static/views/admin/dashboard.html'
    };
    const ruta = rutas[rol];
    if (ruta) {
        window.location.href = ruta;
    } else {
        mostrarError('Rol no reconocido: ' + rol);
    }
}

// ─── Helpers ─────────────────────────────────────────────────

function mostrarError(mensaje) {
    let alerta = document.getElementById('login-error');
    if (!alerta) {
        alerta = document.createElement('div');
        alerta.id = 'login-error';
        alerta.className = 'alert alert-danger mt-3';
        document.getElementById('loginForm').prepend(alerta);
    }
    alerta.textContent = mensaje;
    alerta.style.display = 'block';
}

function isEmpty(valor) {
    return !valor || valor.trim() === '';
}

function isValidEmail(valor) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valor);
}

// ─── Toggle password ─────────────────────────────────────────

document.getElementById('togglePassword').addEventListener('click', () => {
    const input = document.getElementById('password');
    const icon  = document.querySelector('#togglePassword i');
    const esPassword = input.type === 'password';
    input.type = esPassword ? 'text' : 'password';
    icon.classList.toggle('bi-eye',       !esPassword);
    icon.classList.toggle('bi-eye-slash',  esPassword);
});