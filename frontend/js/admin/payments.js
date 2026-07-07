/**
 * payments.js (admin) — Gestión de pagos
 */

AuthService.requireAuth();
const usuario = Auth.getUsuario();
iniciarSidebar('Pagos');

// ── Tema ──────────────────────────────────────────────────────
const themeToggle = document.getElementById('themeToggle');
const themeIcon = document.getElementById('theme-icon');
function aplicarTema(t) {
    document.documentElement.setAttribute('data-theme', t);
    localStorage.setItem('tema', t);
    themeIcon.className = t === 'dark' ? 'bi bi-sun' : 'bi bi-moon-stars';
}
aplicarTema(localStorage.getItem('tema') || 'light');
themeToggle.addEventListener('click', () =>
    aplicarTema(document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark'));

// ── Modales (Tailwind: toggle hidden/flex) ─────────────────────
function abrirModal(id) {
    const m = document.getElementById(id);
    m.classList.remove('hidden');
    m.classList.add('flex');
}
function cerrarModal(id) {
    const m = document.getElementById(id);
    m.classList.add('hidden');
    m.classList.remove('flex');
}
document.querySelectorAll('[id^="modal"]').forEach(m =>
    m.addEventListener('click', e => { if (e.target === m) cerrarModal(m.id); }));

// ── Estado global ─────────────────────────────────────────────
let todosPagos = [];
let citaActivaId = null;

// ── Cargar pagos ──────────────────────────────────────────────
async function cargarPagos() {
    try {
        const resCitas = await apiFetch('/citas');
        const citas = resCitas.data ?? resCitas;

        const pacienteIds = [...new Set(citas.map(c => c.pacienteId))];
        const pagosPromesas = pacienteIds.map(id =>
            apiFetch(`/pagos/paciente/${id}`)
                .then(r => r?.data ?? r ?? [])
                .catch(() => []));

        const resultados = await Promise.all(pagosPromesas);
        const mapaIds = new Map();
        resultados.flat().forEach(p => { if (p?.id) mapaIds.set(p.id, p); });
        todosPagos = Array.from(mapaIds.values());

        calcularMetricas(todosPagos);
        renderTabla(todosPagos);

    } catch (err) { UI.mostrarError(err); }
}

function calcularMetricas(pagos) {
    const hoy = new Date().toLocaleDateString('en-CA');
    const pagados = pagos.filter(p => p.estado === 'PAGADO');
    const pendient = pagos.filter(p => p.estado === 'PENDIENTE');

    const hoyPagos = pagados.filter(p => {
        if (!p.fechaPago) return false;
        const fechaLocal = new Date(p.fechaPago).toLocaleDateString('en-CA');
        return fechaLocal === hoy;
    });

    const recaudado = hoyPagos.reduce((s, p) =>
        s + parseFloat(p.montoFinal || p.monto || 0), 0);

    document.getElementById('m-recaudado').textContent = `S/ ${recaudado.toFixed(2)}`;
    document.getElementById('m-pagados').textContent = pagados.length;
    document.getElementById('m-pendientes').textContent = pendient.length;
    document.getElementById('m-total').textContent = pagos.length;
}

function renderTabla(pagos) {
    document.getElementById('total-mostrados').textContent = `${pagos.length} registros`;
    const tbody = document.getElementById('tabla-pagos');

    if (!pagos.length) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-10 text-[13px]">
      <i class="bi bi-receipt text-2xl block mb-2 text-neblina"></i>
      <span class="text-neblina">No se encontraron pagos</span>
    </td></tr>`;
        return;
    }

    tbody.innerHTML = pagos
        .sort((a, b) => new Date(b.creadoEn) - new Date(a.creadoEn))
        .map(p => {
            const fechaPago = p.fechaPago
                ? new Date(p.fechaPago).toLocaleDateString('es-PE',
                    { day: '2-digit', month: 'short', year: 'numeric' })
                : '—';

            const monto = parseFloat(p.monto || 0).toFixed(2);
            const montoFinal = parseFloat(p.montoFinal || p.monto || 0).toFixed(2);
            const descuento = parseFloat(p.monto || 0) - parseFloat(p.montoFinal || p.monto || 0);

            const badge = p.estado === 'PAGADO'
                ? '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rumbo/10 text-rumbo"><i class="bi bi-check-circle"></i> Pagado</span>'
                : '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-guia/10 text-guia"><i class="bi bi-hourglass-split"></i> Pendiente</span>';

            const metodoIcon = {
                EFECTIVO: '💵', TARJETA: '💳',
                TRANSFERENCIA: '🏦', YAPE: '📱', PLIN: '📱'
            }[p.metodoPago] || '';

            const montoCell = descuento > 0
                ? `<td class="px-4 py-3 font-mono">
             <div>S/ ${monto}</div>
             <div class="text-[11px] text-rumbo">Seguro: -S/ ${descuento.toFixed(2)}</div>
           </td>`
                : `<td class="px-4 py-3 font-mono">S/ ${monto}</td>`;

            const accion = p.estado === 'PENDIENTE'
                ? `<button onclick="abrirPago(${p.citaId},'${p.pacienteNombre}','${p.medicoNombre || ''}')"
             class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-rumbo text-white hover:opacity-90 transition-opacity">
             <i class="bi bi-cash"></i> Cobrar
           </button>`
                : `<button onclick="verComprobante(${p.id})"
             class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-white dark:bg-tinta border border-borde dark:border-borde-dark text-neblina hover:text-tinta dark:hover:text-white transition-colors">
             <i class="bi bi-receipt"></i> Ver
           </button>`;

            return `<tr class="hover:bg-lienzo dark:hover:bg-tinta-dark transition-colors">
        <td class="px-4 py-3"><span class="font-semibold text-guia font-mono">#${p.citaId}</span></td>
        <td class="px-4 py-3">${p.pacienteNombre}</td>
        <td class="px-4 py-3 text-neblina text-xs">${p.medicoNombre || '—'}</td>
        ${montoCell}
        <td class="px-4 py-3 font-semibold text-teal-600 dark:text-teal-400 font-mono">S/ ${montoFinal}</td>
        <td class="px-4 py-3">${metodoIcon} ${p.metodoPago || '—'}</td>
        <td class="px-4 py-3 text-neblina text-xs">${fechaPago}</td>
        <td class="px-4 py-3">${badge}</td>
        <td class="px-4 py-3">${accion}</td>
      </tr>`;
        }).join('');
}

// ── Filtros ───────────────────────────────────────────────────
function aplicarFiltros() {
    const buscar = document.getElementById('buscar-input').value.toLowerCase().trim();
    const estado = document.getElementById('filtro-estado').value;
    const fecha = document.getElementById('filtro-fecha').value;

    renderTabla(todosPagos.filter(p => {
        const matchBuscar = !buscar || p.pacienteNombre?.toLowerCase().includes(buscar);
        const matchEstado = !estado || p.estado === estado;
        const matchFecha = !fecha || (p.fechaPago &&
            new Date(p.fechaPago).toLocaleDateString('en-CA') === fecha);
        return matchBuscar && matchEstado && matchFecha;
    }));
}

function limpiarFiltros() {
    ['buscar-input', 'filtro-estado', 'filtro-fecha'].forEach(id =>
        document.getElementById(id).value = '');
    renderTabla(todosPagos);
}

['buscar-input'].forEach(id =>
    document.getElementById(id).addEventListener('input', aplicarFiltros));
['filtro-estado', 'filtro-fecha'].forEach(id =>
    document.getElementById(id).addEventListener('change', aplicarFiltros));

// ── Registrar pago ────────────────────────────────────────────
async function abrirPago(citaId, pacienteNombre, medicoNombre) {
    citaActivaId = citaId;
    document.getElementById('pago-info-cita').textContent = `Cita #${citaId}`;
    document.getElementById('pago-info-paciente').textContent = pacienteNombre;
    document.getElementById('pago-info-medico').textContent = medicoNombre || '—';
    document.getElementById('pago-metodo').value = '';

    document.getElementById('pago-monto-display').textContent = 'S/ —';
    document.getElementById('pago-seguro-info').innerHTML =
        '<span class="text-neblina text-xs">Calculando...</span>';

    abrirModal('modalPago');

    try {
        const res = await apiFetch(`/pagos/cita/${citaId}/previsualizar`);
        const calculo = res.data ?? res;

        document.getElementById('pago-monto-display').textContent =
            `S/ ${parseFloat(calculo.montoFinal).toFixed(2)}`;

        if (calculo.tieneSeguro) {
            document.getElementById('pago-seguro-info').innerHTML = `
        <div class="flex items-center gap-1.5 justify-center text-rumbo text-xs font-medium">
          <i class="bi bi-shield-check"></i>
          ${calculo.nombreSeguro} aplica ${calculo.porcentajeCobertura}% de descuento
        </div>
        <div class="text-[11px] text-neblina text-center mt-0.5">
          Precio base S/ ${parseFloat(calculo.monto).toFixed(2)} −
          descuento S/ ${parseFloat(calculo.descuento).toFixed(2)}
        </div>`;
        } else {
            document.getElementById('pago-seguro-info').innerHTML = `
        <div class="text-neblina text-xs text-center">
          <i class="bi bi-shield-x"></i> Sin seguro vinculado — precio regular
        </div>`;
        }
    } catch (err) {
        document.getElementById('pago-monto-display').textContent = 'S/ 80.00';
        document.getElementById('pago-seguro-info').innerHTML =
            '<span class="text-alerta text-xs">No se pudo calcular el descuento</span>';
    }
}

async function confirmarPago() {
    const metodo = document.getElementById('pago-metodo').value;
    if (!metodo) return UI.mostrarError({ message: 'Selecciona el método de pago' });

    try {
        await PagoService.registrar({
            citaId: citaActivaId,
            monto: 80.00,
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
            {
                day: '2-digit', month: 'long', year: 'numeric',
                hour: '2-digit', minute: '2-digit'
            })
        : '—';

    const descuento = parseFloat(pago.monto || 0) - parseFloat(pago.montoFinal || pago.monto || 0);

    document.getElementById('comp-estado').innerHTML =
        '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[13px] font-semibold bg-rumbo/10 text-rumbo">✓ Pago confirmado</span>';
    document.getElementById('comp-paciente').textContent = pago.pacienteNombre;
    document.getElementById('comp-medico').textContent = pago.medicoNombre || '—';
    document.getElementById('comp-monto').textContent = `S/ ${parseFloat(pago.monto).toFixed(2)}`;
    document.getElementById('comp-monto-final').textContent = `S/ ${parseFloat(pago.montoFinal || pago.monto).toFixed(2)}`;
    document.getElementById('comp-metodo').textContent = pago.metodoPago || '—';
    document.getElementById('comp-fecha').textContent = fechaPago;

    const seguroEl = document.getElementById('comp-seguro');
    if (descuento > 0) {
        seguroEl.classList.remove('hidden');
        seguroEl.classList.add('flex');
        document.getElementById('comp-descuento').textContent = `-S/ ${descuento.toFixed(2)}`;
    } else {
        seguroEl.classList.add('hidden');
        seguroEl.classList.remove('flex');
    }

    abrirModal('modalComprobante');
}

cargarPagos();