/**
 * dashboard.js — Panel principal del paciente
 */

PortalAuthService.requireAuth();
const paciente = PortalAuth.getPaciente();

const nombreCorto = paciente?.nombreCompleto?.split(' ')[0] || '';
document.getElementById('saludo-usuario').textContent = nombreCorto;
document.getElementById('saludo-usuario-mobile').textContent = nombreCorto;

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
      <div class="text-center py-8 px-5">
        <i class="bi bi-calendar-x text-2xl text-neblina block mb-2"></i>
        <div class="font-semibold mb-1">No tienes citas próximas</div>
        <div class="text-[13px] text-neblina mb-4">Agenda una cita con el médico que necesites</div>
        <a href="directorio.html" class="inline-flex items-center gap-1.5 bg-guia hover:bg-guia/90 text-white text-xs font-semibold rounded-lg px-4 py-2 transition">
          <i class="bi bi-calendar-plus"></i> Agendar cita
        </a>
      </div>`;
    return;
  }

  const fecha = new Date(cita.fechaHora);
  const fechaStr = fecha.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
  const horaStr = fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

  const pagoBadge = cita.estadoPago === 'PAGADO'
    ? `<span class="inline-flex items-center gap-1 bg-rumbo/10 text-rumbo px-2.5 py-1 rounded-full text-[11.5px] font-semibold"><i class="bi bi-check-circle"></i> Pagado</span>`
    : `<span class="inline-flex items-center gap-1 bg-guia/10 text-guia px-2.5 py-1 rounded-full text-[11.5px] font-semibold"><i class="bi bi-clock"></i> Pago pendiente</span>`;

  cont.innerHTML = `
    <div class="p-5 flex items-center gap-4 flex-wrap">
      <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-tinta to-guia flex items-center justify-center text-white text-xl flex-shrink-0">
        <i class="bi bi-calendar-check"></i>
      </div>
      <div class="flex-1 min-w-[200px]">
        <div class="text-[11px] text-neblina uppercase tracking-wider mb-0.5">Tu próxima cita</div>
        <div class="font-display font-semibold text-base capitalize">${fechaStr} — ${horaStr}</div>
        <div class="text-[13.5px] text-neblina mt-0.5">${cita.medicoNombre} · ${cita.especialidad}</div>
      </div>
      <div>${pagoBadge}</div>
    </div>`;
}

function renderDiagnostico(diag) {
  const cont = document.getElementById('card-diagnostico');

  if (!diag) {
    cont.innerHTML = `<div class="text-[13px] text-neblina text-center py-3">Aún no tienes consultas registradas</div>`;
    return;
  }

  const fecha = new Date(diag.fechaCita).toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' });

  cont.innerHTML = `
    <div class="text-[12px] text-neblina mb-1.5">${fecha} · ${diag.medicoNombre}</div>
    <div class="font-semibold text-sm mb-1">${diag.diagnostico}</div>
    <div class="text-[13px] text-neblina">${diag.tratamiento}</div>`;
}

function renderPagoPendiente(pago) {
  const cont = document.getElementById('card-pago-pendiente');

  if (!pago) {
    cont.innerHTML = `<div class="text-[13px] text-neblina text-center py-3">No tienes pagos pendientes 🎉</div>`;
    return;
  }

  cont.innerHTML = `
    <div class="flex justify-between items-center">
      <div>
        <div class="text-[12px] text-neblina">Monto a pagar</div>
        <div class="font-display font-bold text-xl text-guia">S/ ${parseFloat(pago.montoFinal || pago.monto).toFixed(2)}</div>
      </div>
      <a href="citas.html" class="inline-flex items-center gap-1.5 bg-guia hover:bg-guia/90 text-white text-xs font-semibold rounded-lg px-4 py-2 transition">
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