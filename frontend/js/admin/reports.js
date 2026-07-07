/**
 * reports.js — Reportes administrativos
 */

AuthService.requireAuth();
iniciarSidebar('Reportes');

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

// Paleta nueva para el gráfico de métodos de pago
const COLORES_METODO = { EFECTIVO: '#2F9E6E', TARJETA: '#FF7A45', TRANSFERENCIA: '#3B82F6' };
const COLOR_DEFECTO = '#8A94A6';

// ── Periodo: mes actual ───────────────────────────────────────
const ahora = new Date();
const mesActual = ahora.getMonth();
const anioActual = ahora.getFullYear();
const nombreMes = ahora.toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });
document.getElementById('periodo-texto').textContent =
    `Resumen de ${nombreMes.charAt(0).toUpperCase() + nombreMes.slice(1)}`;

function esDelMesActual(fechaStr) {
    const f = new Date(fechaStr);
    return f.getMonth() === mesActual && f.getFullYear() === anioActual;
}

// ── Cargar reportes ───────────────────────────────────────────
async function cargarReportes() {
    try {
        const [citas, medicos] = await Promise.all([
            CitaService.listar(),
            MedicoService.listar(),
        ]);

        const citasMes = citas.filter(c => esDelMesActual(c.fechaHora));

        document.getElementById('m-total-citas').textContent = citasMes.length;

        const pacientesAtendidosSet = new Set(
            citasMes.filter(c => c.estado === 'CONFIRMADA' || c.estado === 'ATENDIDA')
                .map(c => c.pacienteId || c.pacienteNombre)
        );
        document.getElementById('m-pacientes-atendidos').textContent = pacientesAtendidosSet.size;

        const noCanceladas = citasMes.filter(c => c.estado !== 'CANCELADA');
        const asistieron = citasMes.filter(c => c.estado === 'CONFIRMADA' || c.estado === 'ATENDIDA');
        const tasa = noCanceladas.length ? Math.round((asistieron.length / noCanceladas.length) * 100) : 0;
        document.getElementById('m-tasa-asistencia').textContent = `${tasa}%`;

        await calcularIngresosYDesempenoMedicos(citasMes, medicos);

    } catch (err) {
        console.error('Error cargando reportes:', err);
    }
}

async function calcularIngresosYDesempenoMedicos(citasMes, medicos) {
    const pacientesIds = [...new Set(citasMes.map(c => c.pacienteId))];

    let todosPagos = [];
    for (const pid of pacientesIds) {
        if (!pid) continue;
        try {
            const res = await PagoService.getPorPaciente(pid);
            const pagos = res.data ?? res;
            todosPagos.push(...pagos);
        } catch (e) {
            console.error(`Error obteniendo pagos del paciente ${pid}:`, e);
        }
    }

    const pagosDelMes = todosPagos.filter(p =>
        p.estado === 'PAGADO' && p.fechaPago && esDelMesActual(p.fechaPago));

    const ingresoTotal = pagosDelMes.reduce((sum, p) => sum + parseFloat(p.montoFinal || p.monto || 0), 0);
    document.getElementById('m-ingresos').textContent = `S/ ${ingresoTotal.toFixed(2)}`;

    const porMedico = {};
    citasMes.forEach(c => {
        const key = c.medicoNombre || c.medicoId;
        if (!porMedico[key]) {
            porMedico[key] = { nombre: c.medicoNombre, especialidad: c.especialidad, citas: 0, ingresos: 0 };
        }
        porMedico[key].citas++;
    });

    pagosDelMes.forEach(p => {
        const medicoNombre = p.medicoNombre;
        if (medicoNombre && porMedico[medicoNombre]) {
            porMedico[medicoNombre].ingresos += parseFloat(p.montoFinal || p.monto || 0);
        }
    });

    renderTablaMedicos(Object.values(porMedico));
    renderChartMetodosPago(pagosDelMes);
}

function renderTablaMedicos(datos) {
    const tbody = document.getElementById('tabla-medicos');
    if (!datos.length) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center text-neblina py-10 text-[13px]">Sin citas registradas este mes</td></tr>`;
        return;
    }

    datos.sort((a, b) => b.citas - a.citas);

    tbody.innerHTML = datos.map(m => `
    <tr class="hover:bg-lienzo dark:hover:bg-tinta-dark transition-colors">
      <td class="px-4 py-3 font-semibold">${m.nombre || '—'}</td>
      <td class="px-4 py-3 text-neblina">${m.especialidad || '—'}</td>
      <td class="px-4 py-3">${m.citas}</td>
      <td class="px-4 py-3 font-mono font-semibold text-rumbo">S/ ${m.ingresos.toFixed(2)}</td>
    </tr>`).join('');
}

function renderChartMetodosPago(pagos) {
    const el = document.getElementById('chart-metodos-pago');

    if (!pagos.length) {
        el.innerHTML = `<div class="text-center text-neblina py-10 text-[13px]">Sin pagos registrados este mes</div>`;
        return;
    }

    const conteo = {};
    pagos.forEach(p => {
        const metodo = p.metodoPago || 'OTRO';
        conteo[metodo] = (conteo[metodo] || 0) + 1;
    });

    const total = pagos.length;
    const entradas = Object.entries(conteo).sort((a, b) => b[1] - a[1]);
    const maxValor = Math.max(...entradas.map(e => e[1]));
    const ALTURA_MAX = 160;

    const barras = entradas.map(([metodo, cantidad]) => {
        const alturaPx = Math.max(8, Math.round((cantidad / maxValor) * ALTURA_MAX));
        const pct = Math.round((cantidad / total) * 100);
        const color = COLORES_METODO[metodo] || COLOR_DEFECTO;

        return `
      <div class="flex flex-col items-center flex-1 min-w-0 gap-2">
        <div class="text-[13px] font-bold">${cantidad}</div>
        <div class="w-full max-w-[56px] flex items-end justify-center" style="height:${ALTURA_MAX}px">
          <div class="w-full rounded-t-lg rounded-b transition-all duration-500 hover:scale-y-105 cursor-default"
               style="height:${alturaPx}px;background:${color}"
               title="${metodo}: ${cantidad} pagos (${pct}%)"></div>
        </div>
        <div class="text-[11px] text-neblina text-center leading-tight">${metodo}</div>
        <div class="text-[11px] font-semibold">${pct}%</div>
      </div>`;
    }).join('');

    el.innerHTML = `
    <div class="flex items-end gap-4 pt-2.5" style="min-height:${ALTURA_MAX + 70}px">
      ${barras}
    </div>`;
}

// ── Descargar reportes (Excel o PDF) ─────────────────────────
async function descargarReporte(tipo, formato) {
    try {
        const token = Auth.getToken();
        const res = await fetch(`${CONFIG.API_URL}/reportes/${tipo}/${formato}`, {
            headers: { Authorization: `Bearer ${token}` }
        });

        if (!res.ok) throw new Error('No se pudo generar el reporte');

        const blob = await res.blob();
        const extension = formato === 'excel' ? 'xlsx' : 'pdf';
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${tipo}.${extension}`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);

    } catch (err) {
        UI.mostrarError(err);
    }
}

cargarReportes();