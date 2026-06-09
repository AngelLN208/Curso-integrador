// js/recepcionist/dashboard.js

// ─── Guard ───────────────────────────────────────────────────
const usuario = JSON.parse(localStorage.getItem('usuario'));
const token   = localStorage.getItem('token');

if (!token || !usuario) {
    window.location.href = '/views/auth/login.html';
}

// ─── Bienvenida ──────────────────────────────────────────────
document.getElementById('bienvenida').textContent =
    `Bienvenida, ${usuario.nombreCompleto}`;

// ─── Logout ──────────────────────────────────────────────────
document.getElementById('btnLogout').addEventListener('click', (e) => {
    e.preventDefault();
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    window.location.href = '/views/auth/login.html';
});

// ─── Cargar datos ────────────────────────────────────────────
async function cargarDashboard() {
    try {
        const [citas, pacientes] = await Promise.all([
            CitaService.listar(),
            PacienteService.listar()
        ]);

        // Filtra citas de hoy
        const hoy = new Date().toISOString().split('T')[0];
        const citasHoy = citas.filter(c => c.fechaHora.startsWith(hoy));

        // Tarjetas
        document.getElementById('total-citas-hoy').textContent  = citasHoy.length;
        document.getElementById('total-pacientes').textContent  = pacientes.length;

        // Tabla citas de hoy
        const tbody = document.getElementById('tabla-citas-hoy');
        tbody.innerHTML = '';

        if (citasHoy.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center text-muted">
                        No hay citas para hoy
                    </td>
                </tr>`;
            return;
        }

        citasHoy.forEach(cita => {
            const hora = cita.fechaHora.split('T')[1].substring(0, 5);
            tbody.insertAdjacentHTML('beforeend', `
                <tr>
                    <td>${hora}</td>
                    <td>${cita.pacienteNombre}</td>
                    <td>${cita.especialidad}</td>
                    <td>${UI.badgeEstado(cita.estado)}</td>
                </tr>`);
        });

    } catch (err) {
        UI.mostrarError(err);
    }
}

document.addEventListener('DOMContentLoaded', cargarDashboard);