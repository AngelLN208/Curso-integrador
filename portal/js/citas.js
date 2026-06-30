/**
 * citas.js — Listado, filtros y acciones (pagar, cancelar, reprogramar)
 * de las citas del paciente (portal)
 */

PortalAuthService.requireAuth();
const pacienteActualCitas = PortalAuth.getPaciente();
document.getElementById('saludo-usuario').textContent = pacienteActualCitas?.nombreCompleto?.split(' ')[0] || '';

let todasLasCitas = [];
let estadoActivo = '';

// ── Modal: Ver comprobante ────────────────────────────────────────

async function abrirModalComprobante(citaId) {
    document.getElementById('comp-estado').innerHTML =
        '<span style="color:var(--text-3);font-size:13px">Cargando...</span>';
    document.getElementById('modalComprobante').style.display = 'flex';

    try {
        const pago = await PortalService.obtenerComprobante(citaId);

        const fechaPago = pago.fechaPago
            ? new Date(pago.fechaPago).toLocaleDateString('es-PE',
                { day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })
            : '—';

        const monto = parseFloat(pago.monto || 0);
        const montoFinal = parseFloat(pago.montoFinal || pago.monto || 0);
        const descuento = monto - montoFinal;

        document.getElementById('comp-estado').innerHTML =
            pago.estado === 'PAGADO'
                ? '<span style="color:var(--green);font-weight:700;font-size:13px"><i class="bi bi-check-circle"></i> Pago confirmado</span>'
                : '<span style="color:var(--amber);font-weight:700;font-size:13px"><i class="bi bi-hourglass-split"></i> Pago pendiente</span>';

        document.getElementById('comp-medico').textContent = pago.medicoNombre || '—';
        document.getElementById('comp-monto').textContent = `S/ ${monto.toFixed(2)}`;
        document.getElementById('comp-monto-final').textContent = `S/ ${montoFinal.toFixed(2)}`;
        document.getElementById('comp-metodo').textContent = pago.metodoPago || '—';
        document.getElementById('comp-fecha').textContent = fechaPago;

        const seguroEl = document.getElementById('comp-seguro');
        if (descuento > 0) {
            seguroEl.style.display = 'flex';
            document.getElementById('comp-descuento').textContent = `-S/ ${descuento.toFixed(2)}`;
        } else {
            seguroEl.style.display = 'none';
        }

    } catch (err) {
        document.getElementById('comp-estado').innerHTML =
            '<span style="color:var(--red);font-size:13px">No se pudo cargar el comprobante</span>';
    }
}

function cerrarModalComprobante() {
    document.getElementById('modalComprobante').style.display = 'none';
}

function imprimirComprobante() {
    window.print();
}

// ── Carga y filtros ─────────────────────────────────────────────

async function cargarMisCitas() {
    const cont = document.getElementById('lista-citas');
    try {
        todasLasCitas = await PortalService.misCitas();
        renderCitas(todasLasCitas);
    } catch (err) {
        cont.innerHTML = `
      <div class="card card-pad">
        <div class="empty-row" style="color:var(--red)">No se pudieron cargar tus citas</div>
      </div>`;
    }
}

function filtrarPorEstado(estado, btnClickeado) {
    document.querySelectorAll('#filtros-estado button').forEach(b => {
        b.className = 'btn btn-ghost btn-sm';
    });
    btnClickeado.className = 'btn btn-primary btn-sm';
    estadoActivo = estado;
    aplicarFiltrosCitas();
}

function aplicarFiltrosCitas() {
    const texto = document.getElementById('filtro-citas-texto').value.trim().toLowerCase();
    const fechaSeleccionada = document.getElementById('filtro-citas-fecha').value;

    let citas = todasLasCitas;

    if (estadoActivo) {
        citas = citas.filter(c => c.estado === estadoActivo);
    }

    if (texto) {
        citas = citas.filter(c =>
            c.medicoNombre.toLowerCase().includes(texto) ||
            c.especialidad.toLowerCase().includes(texto)
        );
    }

    if (fechaSeleccionada) {
        citas = citas.filter(c => c.fechaHora.startsWith(fechaSeleccionada));
    }

    renderCitas(citas);
}

function limpiarFiltrosCitas() {
    document.getElementById('filtro-citas-texto').value = '';
    document.getElementById('filtro-citas-fecha').value = '';
    estadoActivo = '';
    document.querySelectorAll('#filtros-estado button').forEach(b => b.className = 'btn btn-ghost btn-sm');
    document.querySelector('#filtros-estado button[data-estado=""]').className = 'btn btn-primary btn-sm';
    renderCitas(todasLasCitas);
}

// ── Render de la lista ──────────────────────────────────────────

function renderCitas(citas) {
    const cont = document.getElementById('lista-citas');

    if (!citas.length) {
        cont.innerHTML = `
      <div class="card card-pad">
        <div class="empty-row">No tienes citas en esta categoría</div>
      </div>`;
        return;
    }

    cont.innerHTML = citas.map(cita => {
        const { etiqueta, color, fondo, icono } = estiloEstado(cita.estado);
        const fecha = new Date(cita.fechaHora);
        const fechaTexto = fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' });
        const horaTexto = fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

        return `
      <div class="card card-pad" style="display:flex;flex-direction:column;gap:14px">
        <div style="display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap">
          <div style="display:flex;align-items:center;gap:14px">
            <div style="width:46px;height:46px;border-radius:12px;background:${fondo};
                        display:flex;align-items:center;justify-content:center;font-size:18px;color:${color}">
              <i class="bi ${icono}"></i>
            </div>
            <div>
              <div style="font-weight:700;font-size:14.5px">${cita.medicoNombre}</div>
              <div style="font-size:12.5px;color:var(--text-2)">${cita.especialidad}</div>
              ${cita.motivo ? `<div style="font-size:11.5px;color:var(--text-3);margin-top:2px">${cita.motivo}</div>` : ''}
            </div>
          </div>
          <div style="display:flex;align-items:center;gap:18px">
            <div style="text-align:right">
              <div style="font-size:13px;font-weight:600">${fechaTexto}</div>
              <div style="font-size:12px;color:var(--text-2)">${horaTexto}</div>
            </div>
            <span style="font-size:11.5px;font-weight:700;padding:5px 12px;border-radius:20px;
                         background:${fondo};color:${color};white-space:nowrap">
              ${etiqueta}
            </span>
          </div>
        </div>
        ${renderAcciones(cita)}
      </div>`;
    }).join('');
}

function estiloEstado(estado) {
    const mapa = {
        PENDIENTE: { etiqueta: 'Pendiente', color: 'var(--amber)', fondo: 'var(--amber-lt)', icono: 'bi-hourglass-split' },
        CONFIRMADA: { etiqueta: 'Confirmada', color: 'var(--green)', fondo: '#DCFCE7', icono: 'bi-check-circle' },
        ATENDIDA: { etiqueta: 'Atendida', color: 'var(--accent)', fondo: 'var(--accent-lt)', icono: 'bi-clipboard2-pulse' },
        REPROGRAMADA: { etiqueta: 'Reprogramada', color: 'var(--primary)', fondo: 'var(--primary-lt)', icono: 'bi-arrow-repeat' },
        CANCELADA: { etiqueta: 'Cancelada', color: 'var(--red)', fondo: '#FEE2E2', icono: 'bi-x-circle' },
    };
    return mapa[estado] || { etiqueta: estado, color: 'var(--text-2)', fondo: 'var(--bg-soft)', icono: 'bi-calendar' };
}

// Botones de acción según el estado de la cita.
// Solo PENDIENTE/CONFIRMADA permiten pagar, cancelar o reprogramar.
function renderAcciones(cita) {
    const puedeAccionarCita = cita.estado === 'PENDIENTE'
        || cita.estado === 'CONFIRMADA'
        || cita.estado === 'REPROGRAMADA';
    const tienePagoConfirmado = cita.estado === 'CONFIRMADA' || cita.estado === 'ATENDIDA';

    if (!puedeAccionarCita && cita.estado !== 'ATENDIDA') return '';

    const botones = [];

    // Pagar: aplica si está PENDIENTE o si fue REPROGRAMADA
    // (una reprogramación no cambia el estado del pago, sigue pendiente).
    if (cita.estado === 'PENDIENTE' || cita.estado === 'REPROGRAMADA') {
        botones.push(`
      <button class="btn btn-primary btn-sm" onclick='abrirModalPagar(${JSON.stringify(cita)})'>
        <i class="bi bi-credit-card"></i> Pagar
      </button>`);
    }

    if (puedeAccionarCita) {
        botones.push(`
      <button class="btn btn-ghost btn-sm" onclick='abrirModalReprogramar(${JSON.stringify(cita)})'>
        <i class="bi bi-arrow-repeat"></i> Reprogramar
      </button>`);

        botones.push(`
      <button class="btn btn-ghost btn-sm" style="color:var(--red)" onclick="abrirModalCancelar(${cita.id})">
        <i class="bi bi-x-circle"></i> Cancelar
      </button>`);
    }

    // Ver comprobante: aplica si la cita ya fue confirmada (pago hecho) o atendida
    if (tienePagoConfirmado) {
        botones.push(`
      <button class="btn btn-ghost btn-sm" onclick="abrirModalComprobante(${cita.id})">
        <i class="bi bi-receipt"></i> Ver comprobante
      </button>`);
    }

    // Calificar: solo en citas ATENDIDA que aún no han sido valoradas
    if (cita.estado === 'ATENDIDA' && !cita.yaValorada) {
        botones.push(`
      <button class="btn btn-ghost btn-sm" style="color:var(--amber)" onclick='abrirModalCalificar(${JSON.stringify(cita)})'>
        <i class="bi bi-star"></i> Calificar
      </button>`);
    } else if (cita.estado === 'ATENDIDA' && cita.yaValorada) {
        botones.push(`
      <span style="font-size:12px;color:var(--text-3);display:flex;align-items:center;gap:4px">
        <i class="bi bi-star-fill" style="color:var(--amber)"></i> Ya calificaste esta consulta
      </span>`);
    }

    if (!botones.length) return '';

    return `<div style="display:flex;gap:10px;flex-wrap:wrap;align-items:center;border-top:1px solid var(--border);padding-top:12px">
      ${botones.join('')}
    </div>`;
}


// ── Modal: Pagar ─────────────────────────────────────────────────

async function abrirModalPagar(cita) {
    document.getElementById('pagar-cita-id').value = cita.id;
    document.getElementById('pagar-medico-nombre').textContent = cita.medicoNombre;

    const fecha = new Date(cita.fechaHora);
    document.getElementById('pagar-fecha-info').textContent =
        fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }) +
        ' — ' + fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

    document.getElementById('pagar-metodo').value = 'EFECTIVO';
    document.getElementById('pagar-numero-tarjeta').value = '';
    document.getElementById('pagar-titular-tarjeta').value = '';
    toggleCamposTarjeta();

    document.getElementById('pagar-monto-display').textContent = 'S/ —';
    document.getElementById('pagar-seguro-info').innerHTML =
        '<span style="color:var(--text-3);font-size:12px">Calculando...</span>';

    document.getElementById('modalPagar').style.display = 'flex';

    try {
        const calculo = await PortalService.previsualizarPago(cita.id);

        document.getElementById('pagar-monto-display').textContent =
            `S/ ${parseFloat(calculo.montoFinal).toFixed(2)}`;

        if (calculo.tieneSeguro) {
            document.getElementById('pagar-seguro-info').innerHTML = `
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
            document.getElementById('pagar-seguro-info').innerHTML = `
        <div style="color:var(--text-3);font-size:12px;text-align:center">
          <i class="bi bi-shield-x"></i> Sin seguro vinculado — precio regular
        </div>`;
        }
    } catch (err) {
        document.getElementById('pagar-monto-display').textContent = 'S/ 80.00';
        document.getElementById('pagar-seguro-info').innerHTML =
            '<span style="color:var(--red);font-size:12px">No se pudo calcular el descuento</span>';
    }
}

function cerrarModalPagar() {
    document.getElementById('modalPagar').style.display = 'none';
}

function toggleCamposTarjeta() {
    const esTarjeta = document.getElementById('pagar-metodo').value === 'TARJETA';
    document.getElementById('pagar-campos-tarjeta').style.display = esTarjeta ? 'block' : 'none';
}

async function confirmarPago() {
    const citaId = document.getElementById('pagar-cita-id').value;
    const metodoPago = document.getElementById('pagar-metodo').value;
    const numeroTarjeta = document.getElementById('pagar-numero-tarjeta').value.trim();
    const titularTarjeta = document.getElementById('pagar-titular-tarjeta').value.trim();

    if (metodoPago === 'TARJETA') {
        if (!/^\d{16}$/.test(numeroTarjeta)) {
            return PortalNotify.error('El número de tarjeta debe tener exactamente 16 dígitos');
        }
        if (!titularTarjeta) {
            return PortalNotify.error('Ingresa el nombre del titular de la tarjeta');
        }
    }

    const btn = document.getElementById('btn-confirmar-pago');
    btn.disabled = true;
    btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Procesando...';

    try {
        const data = { citaId: Number(citaId), metodoPago };
        if (metodoPago === 'TARJETA') {
            data.numeroTarjeta = numeroTarjeta;
            data.titularTarjeta = titularTarjeta;
        }

        await PortalService.pagarCita(data);
        PortalNotify.success('¡Pago registrado correctamente!');
        cerrarModalPagar();
        cargarMisCitas();

    } catch (err) {
        PortalNotify.error(err.message || 'No se pudo procesar el pago');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-credit-card"></i> Confirmar pago';
    }
}

// ── Modal: Reprogramar ──────────────────────────────────────────

function abrirModalReprogramar(cita) {
    document.getElementById('reprogramar-cita-id').value = cita.id;
    document.getElementById('reprogramar-medico-id').value = cita.medicoId;
    document.getElementById('reprogramar-medico-nombre').textContent = cita.medicoNombre;

    const fecha = new Date(cita.fechaHora);
    document.getElementById('reprogramar-fecha-actual').textContent =
        'Fecha actual: ' + fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }) +
        ' — ' + fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

    document.getElementById('reprogramar-fecha').value = '';
    document.getElementById('reprogramar-fecha').min = new Date().toISOString().split('T')[0];
    document.getElementById('reprogramar-fechahora-elegida').value = '';
    document.getElementById('reprogramar-slots').innerHTML =
        '<span style="font-size:13px;color:var(--text-3)">Selecciona una fecha primero</span>';

    document.getElementById('modalReprogramar').style.display = 'flex';
}

function cerrarModalReprogramar() {
    document.getElementById('modalReprogramar').style.display = 'none';
}

let slotsReprogramarActuales = [];

async function cargarSlotsReprogramar() {
    const fecha = document.getElementById('reprogramar-fecha').value;
    const medicoId = document.getElementById('reprogramar-medico-id').value;
    const cont = document.getElementById('reprogramar-slots');

    if (!fecha) return;

    cont.innerHTML = '<span style="font-size:13px;color:var(--text-3)">Cargando horarios...</span>';
    document.getElementById('reprogramar-fechahora-elegida').value = '';

    try {
        const slots = await PortalService.disponibilidadSlots(medicoId, fecha);
        slotsReprogramarActuales = slots;

        if (!slots.length) {
            cont.innerHTML = '<span style="font-size:13px;color:var(--text-3)">No hay horarios disponibles este día</span>';
            return;
        }

        cont.innerHTML = slots.map((s, idx) => `
      <button type="button" class="btn btn-ghost btn-sm" onclick="elegirSlotReprogramar(${idx}, this)">
        ${s.hora}
      </button>`).join('');

    } catch (err) {
        cont.innerHTML = '<span style="font-size:13px;color:var(--red)">No se pudo cargar la disponibilidad</span>';
    }
}

function elegirSlotReprogramar(idx, btnClickeado) {
    document.querySelectorAll('#reprogramar-slots button').forEach(b => b.className = 'btn btn-ghost btn-sm');
    btnClickeado.className = 'btn btn-primary btn-sm';
    document.getElementById('reprogramar-fechahora-elegida').value = slotsReprogramarActuales[idx].fechaHora;
}

async function confirmarReprogramacion() {
    const citaId = document.getElementById('reprogramar-cita-id').value;
    const nuevaFechaHora = document.getElementById('reprogramar-fechahora-elegida').value;

    if (!nuevaFechaHora) return PortalNotify.error('Selecciona un nuevo horario disponible');

    const btn = document.getElementById('btn-confirmar-reprogramacion');
    btn.disabled = true;
    btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Reprogramando...';

    try {
        await PortalService.reprogramarCita(citaId, nuevaFechaHora);
        PortalNotify.success('¡Cita reprogramada correctamente!');
        cerrarModalReprogramar();
        cargarMisCitas();

    } catch (err) {
        PortalNotify.error(err.message || 'No se pudo reprogramar la cita');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-arrow-repeat"></i> Confirmar nuevo horario';
    }
}

// ── Modal: Cancelar ──────────────────────────────────────────────

function abrirModalCancelar(citaId) {
    document.getElementById('cancelar-cita-id').value = citaId;
    document.getElementById('modalCancelar').style.display = 'flex';
}

function cerrarModalCancelar() {
    document.getElementById('modalCancelar').style.display = 'none';
}

async function confirmarCancelacion() {
    const citaId = document.getElementById('cancelar-cita-id').value;

    const btn = document.getElementById('btn-confirmar-cancelacion');
    btn.disabled = true;
    btn.innerHTML = 'Cancelando...';

    try {
        await PortalService.cancelarCita(citaId);
        PortalNotify.success('Cita cancelada correctamente');
        cerrarModalCancelar();
        cargarMisCitas();

    } catch (err) {
        PortalNotify.error(err.message || 'No se pudo cancelar la cita');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Sí, cancelar';
    }
}

// ── Modal: Calificar médico ───────────────────────────────────────

let puntuacionSeleccionada = 0;

function abrirModalCalificar(cita) {
    document.getElementById('calificar-cita-id').value = cita.id;
    document.getElementById('calificar-medico-nombre').textContent = cita.medicoNombre;
    document.getElementById('calificar-comentario').value = '';
    puntuacionSeleccionada = 0;
    renderEstrellasCalificar();
    document.getElementById('modalCalificar').style.display = 'flex';
}

function cerrarModalCalificar() {
    document.getElementById('modalCalificar').style.display = 'none';
}

function seleccionarPuntuacion(valor) {
    puntuacionSeleccionada = valor;
    renderEstrellasCalificar();
}

function renderEstrellasCalificar() {
    const cont = document.getElementById('calificar-estrellas');
    cont.innerHTML = [1, 2, 3, 4, 5].map(n => `
      <i class="bi ${n <= puntuacionSeleccionada ? 'bi-star-fill' : 'bi-star'}"
         style="font-size:28px;color:var(--amber);cursor:pointer"
         onclick="seleccionarPuntuacion(${n})"></i>
    `).join('');
}

async function confirmarCalificacion() {
    const citaId = document.getElementById('calificar-cita-id').value;
    const comentario = document.getElementById('calificar-comentario').value.trim();

    if (!puntuacionSeleccionada) return PortalNotify.error('Selecciona una puntuación de 1 a 5 estrellas');

    const btn = document.getElementById('btn-confirmar-calificacion');
    btn.disabled = true;
    btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Enviando...';

    try {
        await PortalService.calificarMedico(citaId, puntuacionSeleccionada, comentario);
        PortalNotify.success('¡Gracias por tu calificación!');
        cerrarModalCalificar();
        cargarMisCitas();

    } catch (err) {
        PortalNotify.error(err.message || 'No se pudo registrar la calificación');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-star"></i> Enviar calificación';
    }
}

cargarMisCitas();