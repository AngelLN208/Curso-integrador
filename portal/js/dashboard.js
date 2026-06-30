/**
 * dashboard.js — Panel principal del paciente
 */

PortalAuthService.requireAuth();
const paciente = PortalAuth.getPaciente();

document.getElementById('saludo-usuario').textContent = paciente?.nombreCompleto?.split(' ')[0] || '';

async function cargarDashboard() {
    try {
        const json = await portalFetch('/portal/dashboard');
        const data = json.data ?? json;

        document.getElementById('bienvenida').textContent = `Hola, ${data.nombrePaciente?.split(' ')[0] || ''} 👋`;

        renderProximaCita(data.proximaCita);
        renderDiagnostico(data.ultimoDiagnostico);
        renderPagoPendiente(data.pagoPendiente);

        document.getElementById('m-total').textContent = data.totalCitas ?? 0;
        document.getElementById('m-pendientes').textContent = data.citasPendientes ?? 0;
        document.getElementById('m-confirmadas').textContent = data.citasConfirmadas ?? 0;
        document.getElementById('m-atendidas').textContent = data.citasAtendidas ?? 0;

    } catch (err) {
        PortalNotify.error('No se pudo cargar tu panel');
    }
}

function renderProximaCita(cita) {
    const cont = document.getElementById('card-proxima-cita');

    if (!cita) {
        cont.innerHTML = `
      <div class="card-pad" style="text-align:center;padding:32px 20px">
        <i class="bi bi-calendar-x" style="font-size:28px;color:var(--text-3);display:block;margin-bottom:10px"></i>
        <div style="font-weight:600;margin-bottom:4px">No tienes citas próximas</div>
        <div style="font-size:13px;color:var(--text-2);margin-bottom:16px">Agenda una cita con el médico que necesites</div>
        <a href="directorio.html" class="btn btn-primary btn-sm">
          <i class="bi bi-calendar-plus"></i> Agendar cita
        </a>
      </div>`;
        return;
    }

    const fecha = new Date(cita.fechaHora);
    const fechaStr = fecha.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
    const horaStr = fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

    const pagoBadge = cita.estadoPago === 'PAGADO'
        ? '<span style="background:var(--accent-lt);color:var(--accent);padding:4px 10px;border-radius:20px;font-size:11.5px;font-weight:600"><i class="bi bi-check-circle"></i> Pagado</span>'
        : '<span style="background:#FEF3C7;color:var(--amber);padding:4px 10px;border-radius:20px;font-size:11.5px;font-weight:600"><i class="bi bi-clock"></i> Pago pendiente</span>';

    cont.innerHTML = `
    <div class="card-pad" style="display:flex;align-items:center;gap:20px;flex-wrap:wrap">
      <div style="width:56px;height:56px;background:linear-gradient(135deg,var(--primary),var(--accent));
                  border-radius:16px;display:flex;align-items:center;justify-content:center;
                  color:white;font-size:24px;flex-shrink:0">
        <i class="bi bi-calendar-check"></i>
      </div>
      <div style="flex:1;min-width:200px">
        <div style="font-size:11px;color:var(--text-3);text-transform:uppercase;letter-spacing:.05em;margin-bottom:2px">
          Tu próxima cita
        </div>
        <div style="font-weight:700;font-size:16px;text-transform:capitalize">${fechaStr} — ${horaStr}</div>
        <div style="font-size:13.5px;color:var(--text-2);margin-top:2px">
          ${cita.medicoNombre} · ${cita.especialidad}
        </div>
      </div>
      <div>${pagoBadge}</div>
    </div>`;
}

function renderDiagnostico(diag) {
    const cont = document.getElementById('card-diagnostico');

    if (!diag) {
        cont.innerHTML = `<div class="empty-row">Aún no tienes consultas registradas</div>`;
        return;
    }

    const fecha = new Date(diag.fechaCita).toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' });

    cont.innerHTML = `
    <div style="font-size:12px;color:var(--text-3);margin-bottom:6px">${fecha} · ${diag.medicoNombre}</div>
    <div style="font-weight:600;font-size:14px;margin-bottom:4px">${diag.diagnostico}</div>
    <div style="font-size:13px;color:var(--text-2)">${diag.tratamiento}</div>`;
}

function renderPagoPendiente(pago) {
    const cont = document.getElementById('card-pago-pendiente');

    if (!pago) {
        cont.innerHTML = `<div class="empty-row">No tienes pagos pendientes 🎉</div>`;
        return;
    }

    cont.innerHTML = `
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div style="font-size:12px;color:var(--text-3)">Monto a pagar</div>
        <div style="font-weight:800;font-size:20px;color:var(--amber)">S/ ${parseFloat(pago.montoFinal || pago.monto).toFixed(2)}</div>
      </div>
      <a href="citas.html" class="btn btn-primary btn-sm">
        <i class="bi bi-credit-card"></i> Pagar ahora
      </a>
    </div>`;
}

async function descargarHistorial(e) {
    e.preventDefault();
    try {
        const token = PortalAuth.getToken();
        const res = await fetch(`${PORTAL_CONFIG.API_URL}/portal/historial/pdf`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        if (!res.ok) throw new Error('No se pudo generar el historial');

        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'historial-medico.pdf';
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);

    } catch (err) {
        PortalNotify.error('No se pudo descargar tu historial médico');
    }
}

cargarDashboard();