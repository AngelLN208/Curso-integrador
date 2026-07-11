/**
 * citas.js — Listado, filtros y acciones (pagar, cancelar, reprogramar)
 * de las citas del paciente (portal)
 */

PortalAuthService.requireAuth();
const pacienteActualCitas = PortalAuth.getPaciente();
const nombreCortoCitas = pacienteActualCitas?.nombreCompleto?.split(' ')[0] || '';
document.getElementById('saludo-usuario').textContent = nombreCortoCitas;
document.getElementById('saludo-usuario-mobile').textContent = nombreCortoCitas;

let todasLasCitas = [];
let estadoActivo = '';

const CLASE_CHIP_INACTIVO = 'flex items-center gap-1.5 bg-white/10 border border-white/15 text-white/80 text-xs font-medium rounded-lg px-3.5 py-2 transition hover:bg-white/[0.16]';
const CLASE_CHIP_ACTIVO = 'flex items-center gap-1.5 bg-guia text-white text-xs font-semibold rounded-lg px-3.5 py-2 transition';

function abrirModal(id) {
    const modal = document.getElementById(id);
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function cerrarModal(id) {
    const modal = document.getElementById(id);
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

// ── Modal: Ver comprobante ────────────────────────────────────────

async function abrirModalComprobante(citaId) {
    document.getElementById('comp-estado').innerHTML =
        '<span class="text-neblina text-[13px]">Cargando...</span>';
    abrirModal('modalComprobante');

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
                ? '<span class="text-rumbo font-bold text-[13px]"><i class="bi bi-check-circle"></i> Pago confirmado</span>'
                : '<span class="text-guia font-bold text-[13px]"><i class="bi bi-hourglass-split"></i> Pago pendiente</span>';

        document.getElementById('comp-medico').textContent = pago.medicoNombre || '—';
        document.getElementById('comp-monto').textContent = `S/ ${monto.toFixed(2)}`;
        document.getElementById('comp-monto-final').textContent = `S/ ${montoFinal.toFixed(2)}`;
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

    } catch (err) {
        document.getElementById('comp-estado').innerHTML =
            '<span class="text-alerta text-[13px]">No se pudo cargar el comprobante</span>';
    }
}

function cerrarModalComprobante() {
    cerrarModal('modalComprobante');
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
      <div class="bg-white/[0.07] backdrop-blur-md border border-white/15 rounded-card text-center text-sm text-alerta py-8">
        No se pudieron cargar tus citas
      </div>`;
    }
}

function filtrarPorEstado(estado, btnClickeado) {
    document.querySelectorAll('#filtros-estado button').forEach(b => {
        b.className = CLASE_CHIP_INACTIVO;
    });
    btnClickeado.className = CLASE_CHIP_ACTIVO;
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
    document.querySelectorAll('#filtros-estado button').forEach(b => b.className = CLASE_CHIP_INACTIVO);
    document.querySelector('#filtros-estado button[data-estado=""]').className = CLASE_CHIP_ACTIVO;
    renderCitas(todasLasCitas);
}

// ── Render de la lista ──────────────────────────────────────────

function renderCitas(citas) {
    const cont = document.getElementById('lista-citas');

    if (!citas.length) {
        cont.innerHTML = `
      <div class="bg-white/[0.07] backdrop-blur-md border border-white/15 rounded-card text-center text-sm text-white/60 py-8">
        No tienes citas en esta categoría
      </div>`;
        return;
    }

    cont.innerHTML = citas.map(cita => {
        const { etiqueta, badge, icono } = estiloEstado(cita.estado);
        const fecha = new Date(cita.fechaHora);
        const fechaTexto = fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' });
        const horaTexto = fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

        return `
      <div class="bg-white/[0.07] backdrop-blur-md border border-white/15 rounded-card p-5 flex flex-col gap-3.5">
        <div class="flex items-center justify-between gap-4 flex-wrap">
          <div class="flex items-center gap-3.5">
            <div class="w-[46px] h-[46px] rounded-xl ${badge} flex items-center justify-center text-lg flex-shrink-0">
              <i class="bi ${icono}"></i>
            </div>
            <div>
              <div class="font-semibold text-[14.5px] text-white">${cita.medicoNombre}</div>
              <div class="text-[12.5px] text-white/60">${cita.especialidad}</div>
              ${cita.motivo ? `<div class="text-[11.5px] text-white/40 mt-0.5">${cita.motivo}</div>` : ''}
            </div>
          </div>
          <div class="flex items-center gap-4">
            <div class="text-right">
              <div class="text-[13px] font-semibold text-white">${fechaTexto}</div>
              <div class="text-[12px] text-white/60">${horaTexto}</div>
            </div>
            <span class="text-[11.5px] font-bold px-3 py-1.5 rounded-full whitespace-nowrap ${badge}">
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
        PENDIENTE: { etiqueta: 'Pendiente', badge: 'bg-guia/15 text-guia', icono: 'bi-hourglass-split' },
        CONFIRMADA: { etiqueta: 'Confirmada', badge: 'bg-rumbo/15 text-rumbo', icono: 'bi-check-circle' },
        ATENDIDA: { etiqueta: 'Atendida', badge: 'bg-[#7DA6FF]/15 text-[#7DA6FF]', icono: 'bi-clipboard2-pulse' },
        REPROGRAMADA: { etiqueta: 'Reprogramada', badge: 'bg-[#C9A6FF]/15 text-[#C9A6FF]', icono: 'bi-arrow-repeat' },
        CANCELADA: { etiqueta: 'Cancelada', badge: 'bg-alerta/15 text-alerta', icono: 'bi-x-circle' },
    };
    return mapa[estado] || { etiqueta: estado, badge: 'bg-white/10 text-white/60', icono: 'bi-calendar' };
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
    const claseBtn = 'flex items-center gap-1.5 border border-white/15 text-white/80 hover:bg-white/10 text-xs font-medium rounded-lg px-3 py-1.5 transition';
    const claseBtnPrimario = 'flex items-center gap-1.5 bg-guia hover:bg-guia/90 text-white text-xs font-semibold rounded-lg px-3 py-1.5 transition';
    const claseBtnPeligro = 'flex items-center gap-1.5 border border-alerta/30 text-alerta hover:bg-alerta/10 text-xs font-medium rounded-lg px-3 py-1.5 transition';

    // Pagar: aplica si está PENDIENTE o si fue REPROGRAMADA
    // (una reprogramación no cambia el estado del pago, sigue pendiente).
    if (cita.estado === 'PENDIENTE' || cita.estado === 'REPROGRAMADA') {
        botones.push(`
      <button class="${claseBtnPrimario}" onclick='abrirModalPagar(${JSON.stringify(cita)})'>
        <i class="bi bi-credit-card"></i> Pagar
      </button>`);
    }

    if (puedeAccionarCita) {
        botones.push(`
      <button class="${claseBtn}" onclick='abrirModalReprogramar(${JSON.stringify(cita)})'>
        <i class="bi bi-arrow-repeat"></i> Reprogramar
      </button>`);

        botones.push(`
      <button class="${claseBtnPeligro}" onclick="abrirModalCancelar(${cita.id})">
        <i class="bi bi-x-circle"></i> Cancelar
      </button>`);
    }

    // Ver comprobante: aplica si la cita ya fue confirmada (pago hecho) o atendida
    if (tienePagoConfirmado) {
        botones.push(`
      <button class="${claseBtn}" onclick="abrirModalComprobante(${cita.id})">
        <i class="bi bi-receipt"></i> Ver comprobante
      </button>`);
    }

    // Calificar: solo en citas ATENDIDA que aún no han sido valoradas
    if (cita.estado === 'ATENDIDA' && !cita.yaValorada) {
        botones.push(`
      <button class="flex items-center gap-1.5 border border-guia/30 text-guia hover:bg-guia/10 text-xs font-medium rounded-lg px-3 py-1.5 transition" onclick='abrirModalCalificar(${JSON.stringify(cita)})'>
        <i class="bi bi-star"></i> Calificar
      </button>`);
    } else if (cita.estado === 'ATENDIDA' && cita.yaValorada) {
        botones.push(`
      <span class="text-xs text-white/50 flex items-center gap-1.5">
        <i class="bi bi-star-fill text-guia"></i> Ya calificaste esta consulta
      </span>`);
    }

    if (!botones.length) return '';

    return `<div class="flex gap-2.5 flex-wrap items-center border-t border-white/10 pt-3">
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
        '<span class="text-neblina text-xs">Calculando...</span>';

    abrirModal('modalPagar');

    try {
        const calculo = await PortalService.previsualizarPago(cita.id);

        document.getElementById('pagar-monto-display').textContent =
            `S/ ${parseFloat(calculo.montoFinal).toFixed(2)}`;

        if (calculo.tieneSeguro) {
            document.getElementById('pagar-seguro-info').innerHTML = `
        <div class="flex items-center gap-1.5 justify-center text-rumbo text-xs font-medium">
          <i class="bi bi-shield-check"></i>
          ${calculo.nombreSeguro} aplica ${calculo.porcentajeCobertura}% de descuento
        </div>
        <div class="text-[11px] text-neblina text-center mt-0.5">
          Precio base S/ ${parseFloat(calculo.monto).toFixed(2)} −
          descuento S/ ${parseFloat(calculo.descuento).toFixed(2)}
        </div>`;
        } else {
            document.getElementById('pagar-seguro-info').innerHTML = `
        <div class="text-neblina text-xs text-center">
          <i class="bi bi-shield-x"></i> Sin seguro vinculado — precio regular
        </div>`;
        }
    } catch (err) {
        document.getElementById('pagar-monto-display').textContent = 'S/ 80.00';
        document.getElementById('pagar-seguro-info').innerHTML =
            '<span class="text-alerta text-xs">No se pudo calcular el descuento</span>';
    }
}

function cerrarModalPagar() {
    cerrarModal('modalPagar');
}

function toggleCamposTarjeta() {
    const esTarjeta = document.getElementById('pagar-metodo').value === 'TARJETA';
    const cont = document.getElementById('pagar-campos-tarjeta');
    cont.classList.toggle('hidden', !esTarjeta);
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
        '<span class="text-[13px] text-neblina">Selecciona una fecha primero</span>';

    abrirModal('modalReprogramar');
}

function cerrarModalReprogramar() {
    cerrarModal('modalReprogramar');
}

let slotsReprogramarActuales = [];

const CLASE_SLOT_INACTIVO = 'border border-borde text-tinta text-xs font-medium rounded-lg px-3 py-1.5 transition hover:border-guia';
const CLASE_SLOT_ACTIVO = 'bg-guia border border-guia text-white text-xs font-semibold rounded-lg px-3 py-1.5 transition';

async function cargarSlotsReprogramar() {
    const fecha = document.getElementById('reprogramar-fecha').value;
    const medicoId = document.getElementById('reprogramar-medico-id').value;
    const cont = document.getElementById('reprogramar-slots');

    if (!fecha) return;

    cont.innerHTML = '<span class="text-[13px] text-neblina">Cargando horarios...</span>';
    document.getElementById('reprogramar-fechahora-elegida').value = '';

    try {
        const slots = await PortalService.disponibilidadSlots(medicoId, fecha);
        slotsReprogramarActuales = slots;

        if (!slots.length) {
            cont.innerHTML = '<span class="text-[13px] text-neblina">No hay horarios disponibles este día</span>';
            return;
        }

        cont.innerHTML = slots.map((s, idx) => `
      <button type="button" class="${CLASE_SLOT_INACTIVO}" onclick="elegirSlotReprogramar(${idx}, this)">
        ${s.hora}
      </button>`).join('');

    } catch (err) {
        cont.innerHTML = '<span class="text-[13px] text-alerta">No se pudo cargar la disponibilidad</span>';
    }
}

function elegirSlotReprogramar(idx, btnClickeado) {
    document.querySelectorAll('#reprogramar-slots button').forEach(b => b.className = CLASE_SLOT_INACTIVO);
    btnClickeado.className = CLASE_SLOT_ACTIVO;
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
    abrirModal('modalCancelar');
}

function cerrarModalCancelar() {
    cerrarModal('modalCancelar');
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
    abrirModal('modalCalificar');
}

function cerrarModalCalificar() {
    cerrarModal('modalCalificar');
}

function seleccionarPuntuacion(valor) {
    puntuacionSeleccionada = valor;
    renderEstrellasCalificar();
}

function renderEstrellasCalificar() {
    const cont = document.getElementById('calificar-estrellas');
    cont.innerHTML = [1, 2, 3, 4, 5].map(n => `
      <i class="bi ${n <= puntuacionSeleccionada ? 'bi-star-fill' : 'bi-star'} text-guia text-[28px] cursor-pointer"
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