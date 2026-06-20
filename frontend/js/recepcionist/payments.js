/**
 * payments.js — Gestión de pagos
 */

AuthService.requireAuth();
const usuario = Auth.getUsuario();
iniciarSidebar('Pagos');

// ── Tema ──────────────────────────────────────────────────────
const themeToggle = document.getElementById('themeToggle');
const themeIcon   = document.getElementById('theme-icon');
function aplicarTema(t) {
  document.documentElement.setAttribute('data-theme', t);
  localStorage.setItem('tema', t);
  themeIcon.className = t === 'dark' ? 'bi bi-sun' : 'bi bi-moon-stars';
}
aplicarTema(localStorage.getItem('tema') || 'light');
themeToggle.addEventListener('click', () =>
  aplicarTema(document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark'));



// ── Modales ───────────────────────────────────────────────────
function abrirModal(id)  { document.getElementById(id).classList.add('open'); }
function cerrarModal(id) { document.getElementById(id).classList.remove('open'); }
document.querySelectorAll('.modal-backdrop').forEach(m =>
  m.addEventListener('click', e => { if (e.target === m) m.classList.remove('open'); }));

// ── Estado global ─────────────────────────────────────────────
let todosPagos   = [];
let citaActivaId = null;

// ── Cargar pagos ──────────────────────────────────────────────
async function cargarPagos() {
  try {
    // Obtener todos los pagos a través de las citas
    const resCitas = await apiFetch('/citas');
    const citas    = resCitas.data ?? resCitas;

    // Por cada paciente único, obtener sus pagos
    const pacienteIds = [...new Set(citas.map(c => c.pacienteId))];
    const pagosPromesas = pacienteIds.map(id =>
      apiFetch(`/pagos/paciente/${id}`)
        .then(r => r?.data ?? r ?? [])
        .catch(() => []));

    const resultados = await Promise.all(pagosPromesas);
    const mapaIds    = new Map();
    resultados.flat().forEach(p => { if (p?.id) mapaIds.set(p.id, p); });
    todosPagos = Array.from(mapaIds.values());

    calcularMetricas(todosPagos);
    renderTabla(todosPagos);

  } catch (err) { UI.mostrarError(err); }
}

function calcularMetricas(pagos) {
  const hoy = new Date().toLocaleDateString('en-CA'); // yyyy-MM-dd local
  const pagados  = pagos.filter(p => p.estado === 'PAGADO');
  const pendient = pagos.filter(p => p.estado === 'PENDIENTE');

  // Recaudado hoy: pagos PAGADO cuya fechaPago es de hoy (hora local)
  const hoyPagos = pagados.filter(p => {
    if (!p.fechaPago) return false;
    const fechaLocal = new Date(p.fechaPago).toLocaleDateString('en-CA');
    return fechaLocal === hoy;
  });

  const recaudado = hoyPagos.reduce((s, p) =>
    s + parseFloat(p.montoFinal || p.monto || 0), 0);

  document.getElementById('m-recaudado').textContent  = `S/ ${recaudado.toFixed(2)}`;
  document.getElementById('m-pagados').textContent    = pagados.length;
  document.getElementById('m-pendientes').textContent = pendient.length;
  document.getElementById('m-total').textContent      = pagos.length;
}

function renderTabla(pagos) {
  document.getElementById('total-mostrados').textContent = `${pagos.length} registros`;
  const tbody = document.getElementById('tabla-pagos');

  if (!pagos.length) {
    tbody.innerHTML = `<tr><td colspan="9" class="empty-row">
      <i class="bi bi-receipt" style="font-size:24px;display:block;margin-bottom:8px;color:var(--text-3)"></i>
      No se encontraron pagos
    </td></tr>`;
    return;
  }

  tbody.innerHTML = pagos
    .sort((a, b) => new Date(b.creadoEn) - new Date(a.creadoEn))
    .map(p => {
      const fechaPago = p.fechaPago
        ? new Date(p.fechaPago).toLocaleDateString('es-PE',
            { day:'2-digit', month:'short', year:'numeric' })
        : '—';

      const monto      = parseFloat(p.monto || 0).toFixed(2);
      const montoFinal = parseFloat(p.montoFinal || p.monto || 0).toFixed(2);
      const descuento  = parseFloat(p.monto || 0) - parseFloat(p.montoFinal || p.monto || 0);

      const badge = p.estado === 'PAGADO'
        ? '<span class="badge badge-confirmada">✓ Pagado</span>'
        : '<span class="badge badge-pendiente">⏳ Pendiente</span>';

      const metodoIcon = { EFECTIVO:'💵', TARJETA:'💳',
        TRANSFERENCIA:'🏦', YAPE:'📱', PLIN:'📱' }[p.metodoPago] || '';

      // Mostrar seguro si hay descuento aplicado
      const montoCell = descuento > 0
        ? `<td>
             <div>S/ ${monto}</div>
             <div style="font-size:11px;color:var(--green)">
               Seguro: -S/ ${descuento.toFixed(2)}
             </div>
           </td>`
        : `<td>S/ ${monto}</td>`;

      const accion = p.estado === 'PENDIENTE'
        ? `<button class="btn btn-sm btn-green"
                   onclick="abrirPago(${p.citaId},'${p.pacienteNombre}','${p.medicoNombre || ''}')">
             <i class="bi bi-cash"></i> Cobrar
           </button>`
        : `<button class="btn btn-sm btn-ghost" onclick="verComprobante(${p.id})">
             <i class="bi bi-receipt"></i> Ver
           </button>`;

      return `<tr>
        <td><span style="font-weight:600;color:var(--indigo)">#${p.citaId}</span></td>
        <td>${p.pacienteNombre}</td>
        <td style="color:var(--text-2);font-size:12px">${p.medicoNombre || '—'}</td>
        ${montoCell}
        <td style="font-weight:600;color:var(--teal)">S/ ${montoFinal}</td>
        <td>${metodoIcon} ${p.metodoPago || '—'}</td>
        <td style="color:var(--text-2);font-size:12px">${fechaPago}</td>
        <td>${badge}</td>
        <td>${accion}</td>
      </tr>`;
    }).join('');
}

// ── Filtros ───────────────────────────────────────────────────
function aplicarFiltros() {
  const buscar = document.getElementById('buscar-input').value.toLowerCase().trim();
  const estado = document.getElementById('filtro-estado').value;
  const fecha  = document.getElementById('filtro-fecha').value;

  renderTabla(todosPagos.filter(p => {
    const matchBuscar = !buscar || p.pacienteNombre?.toLowerCase().includes(buscar);
    const matchEstado = !estado || p.estado === estado;
    const matchFecha  = !fecha  || (p.fechaPago &&
      new Date(p.fechaPago).toLocaleDateString('en-CA') === fecha);
    return matchBuscar && matchEstado && matchFecha;
  }));
}

function limpiarFiltros() {
  ['buscar-input','filtro-estado','filtro-fecha'].forEach(id =>
    document.getElementById(id).value = '');
  renderTabla(todosPagos);
}

['buscar-input'].forEach(id =>
  document.getElementById(id).addEventListener('input', aplicarFiltros));
['filtro-estado','filtro-fecha'].forEach(id =>
  document.getElementById(id).addEventListener('change', aplicarFiltros));

// ── Registrar pago ────────────────────────────────────────────
async function abrirPago(citaId, pacienteNombre, medicoNombre) {
  citaActivaId = citaId;
  document.getElementById('pago-info-cita').textContent     = `Cita #${citaId}`;
  document.getElementById('pago-info-paciente').textContent = pacienteNombre;
  document.getElementById('pago-info-medico').textContent   = medicoNombre || '—';
  document.getElementById('pago-metodo').value               = '';

  // Mostrar estado de carga mientras se calcula el descuento
  document.getElementById('pago-monto-display').textContent = 'S/ —';
  document.getElementById('pago-seguro-info').innerHTML =
    '<span style="color:var(--text-3);font-size:12px">Calculando...</span>';

  abrirModal('modalPago');

  try {
    const res = await apiFetch(`/pagos/cita/${citaId}/previsualizar`);
    const calculo = res.data ?? res;

    document.getElementById('pago-monto-display').textContent =
      `S/ ${parseFloat(calculo.montoFinal).toFixed(2)}`;

    if (calculo.tieneSeguro) {
      document.getElementById('pago-seguro-info').innerHTML = `
        <div style="display:flex;align-items:center;gap:6px;justify-content:center;
                    color:var(--green);font-size:12px;font-weight:500">
          <i class="bi bi-shield-check"></i>
          ${calculo.nombreSeguro} aplica ${calculo.porcentajeCobertura}% de descuento
        </div>
        <div style="font-size:11px;color:var(--text-3);text-align:center;margin-top:2px">
          Precio base S/ ${parseFloat(calculo.monto).toFixed(2)} −
          descuento S/ ${parseFloat(calculo.descuento).toFixed(2)}
        </div>`;
    } else {
      document.getElementById('pago-seguro-info').innerHTML = `
        <div style="color:var(--text-3);font-size:12px;text-align:center">
          <i class="bi bi-shield-x"></i> Sin seguro vinculado — precio regular
        </div>`;
    }
  } catch (err) {
    document.getElementById('pago-monto-display').textContent = 'S/ 80.00';
    document.getElementById('pago-seguro-info').innerHTML =
      '<span style="color:var(--red);font-size:12px">No se pudo calcular el descuento</span>';
  }
}

async function confirmarPago() {
  const metodo = document.getElementById('pago-metodo').value;
  if (!metodo) return UI.mostrarError({ message: 'Selecciona el método de pago' });

  try {
    await PagoService.registrar({
      citaId:     citaActivaId,
      monto:      80.00,      // monto fijo — no editable por recepcionista
      metodoPago: metodo
    });
    cerrarModal('modalPago');
    UI.mostrarAlerta('Pago registrado correctamente');
    cargarPagos();
  } catch (err) { UI.mostrarError(err); }
}

// ── Ver comprobante ───────────────────────────────────────────
function verComprobante(pagoId) {
  const pago = todosPagos.find(p => p.id === pagoId);
  if (!pago) return;

  const fechaPago = pago.fechaPago
    ? new Date(pago.fechaPago).toLocaleDateString('es-PE',
        { day:'2-digit', month:'long', year:'numeric',
          hour:'2-digit', minute:'2-digit' })
    : '—';

  const descuento = parseFloat(pago.monto || 0) - parseFloat(pago.montoFinal || pago.monto || 0);

  document.getElementById('comp-estado').innerHTML =
    '<span class="badge badge-confirmada" style="font-size:13px">✓ Pago confirmado</span>';
  document.getElementById('comp-paciente').textContent    = pago.pacienteNombre;
  document.getElementById('comp-medico').textContent      = pago.medicoNombre || '—';
  document.getElementById('comp-monto').textContent       = `S/ ${parseFloat(pago.monto).toFixed(2)}`;
  document.getElementById('comp-monto-final').textContent = `S/ ${parseFloat(pago.montoFinal || pago.monto).toFixed(2)}`;
  document.getElementById('comp-metodo').textContent      = pago.metodoPago || '—';
  document.getElementById('comp-fecha').textContent       = fechaPago;

  // Mostrar descuento por seguro si aplica
  const seguroEl = document.getElementById('comp-seguro');
  if (descuento > 0) {
    seguroEl.style.display = 'flex';
    document.getElementById('comp-descuento').textContent = `-S/ ${descuento.toFixed(2)}`;
  } else {
    seguroEl.style.display = 'none';
  }

  abrirModal('modalComprobante');
}

cargarPagos();