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

const COLORES_METODO = { EFECTIVO: 'green', TARJETA: 'indigo', TRANSFERENCIA: 'blue' };

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

        // Métrica: total citas del mes
        document.getElementById('m-total-citas').textContent = citasMes.length;

        // Métrica: pacientes atendidos (únicos, estado distinto a pendiente)
        const pacientesAtendidosSet = new Set(
            citasMes.filter(c => c.estado === 'CONFIRMADA' || c.estado === 'ATENDIDA')
                .map(c => c.pacienteId || c.pacienteNombre)
        );
        document.getElementById('m-pacientes-atendidos').textContent = pacientesAtendidosSet.size;

        // Métrica: tasa de asistencia (confirmadas+atendidas / total - canceladas)
        const noCanceladas = citasMes.filter(c => c.estado !== 'CANCELADA');
        const asistieron = citasMes.filter(c => c.estado === 'CONFIRMADA' || c.estado === 'ATENDIDA');
        const tasa = noCanceladas.length ? Math.round((asistieron.length / noCanceladas.length) * 100) : 0;
        document.getElementById('m-tasa-asistencia').textContent = `${tasa}%`;

        // Ingresos del mes y desempeño por médico — requiere pagos por cada cita
        await calcularIngresosYDesempenoMedicos(citasMes, medicos);

    } catch (err) {
        console.error('Error cargando reportes:', err);
    }
}

async function calcularIngresosYDesempenoMedicos(citasMes, medicos) {
    // Trae los pagos de cada paciente involucrado en las citas del mes
    const pacientesIds = [...new Set(citasMes.map(c => c.pacienteId))];
    console.log('pacientesIds encontrados:', pacientesIds);

    let todosPagos = [];
    for (const pid of pacientesIds) {
        if (!pid) continue;
        try {
            const res = await PagoService.getPorPaciente(pid);
            const pagos = res.data ?? res;
            console.log(`Pagos del paciente ${pid}:`, pagos);
            todosPagos.push(...pagos);
        } catch (e) {
            console.error(`Error obteniendo pagos del paciente ${pid}:`, e);
        }
    }

    console.log('Total pagos recolectados:', todosPagos);

    const pagosDelMes = todosPagos.filter(p =>
        p.estado === 'PAGADO' && p.fechaPago && esDelMesActual(p.fechaPago));

    console.log('Pagos filtrados del mes:', pagosDelMes);

    const ingresoTotal = pagosDelMes.reduce((sum, p) => sum + parseFloat(p.montoFinal || p.monto || 0), 0);
    document.getElementById('m-ingresos').textContent = `S/ ${ingresoTotal.toFixed(2)}`;

    // Desempeño por médico
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

    // Distribución de métodos de pago
    renderChartMetodosPago(pagosDelMes);
}

function renderTablaMedicos(datos) {
    const tbody = document.getElementById('tabla-medicos');
    if (!datos.length) {
        tbody.innerHTML = `<tr><td colspan="4" class="empty-row">Sin citas registradas este mes</td></tr>`;
        return;
    }

    datos.sort((a, b) => b.citas - a.citas);

    tbody.innerHTML = datos.map(m => `
    <tr>
      <td style="font-weight:600;color:var(--text)">${m.nombre || '—'}</td>
      <td style="color:var(--text-2)">${m.especialidad || '—'}</td>
      <td>${m.citas}</td>
      <td style="color:var(--green);font-weight:600">S/ ${m.ingresos.toFixed(2)}</td>
    </tr>`).join('');
}

function renderChartMetodosPago(pagos) {
    const el = document.getElementById('chart-metodos-pago');

    if (!pagos.length) {
        el.innerHTML = `<div class="empty-row">Sin pagos registrados este mes</div>`;
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
    const ALTURA_MAX = 160; // px

    const barras = entradas.map(([metodo, cantidad]) => {
        const alturaPx = Math.max(8, Math.round((cantidad / maxValor) * ALTURA_MAX));
        const pct = Math.round((cantidad / total) * 100);
        const color = COLORES_METODO[metodo] || 'amber';

        return `
      <div style="display:flex;flex-direction:column;align-items:center;
                  flex:1;min-width:0;gap:8px">
        <div style="font-size:13px;font-weight:700;color:var(--text)">${cantidad}</div>
        <div style="width:100%;max-width:56px;height:${ALTURA_MAX}px;
                    display:flex;align-items:flex-end;justify-content:center">
          <div style="width:100%;height:${alturaPx}px;
                      background:linear-gradient(180deg,var(--${color}),var(--${color}));
                      border-radius:8px 8px 4px 4px;
                      box-shadow:0 2px 8px rgba(0,0,0,.08);
                      transition:height .6s ease, transform .15s;
                      cursor:default"
               onmouseover="this.style.transform='scaleY(1.03)'"
               onmouseout="this.style.transform='scaleY(1)'"
               title="${metodo}: ${cantidad} pagos (${pct}%)"></div>
        </div>
        <div style="font-size:11px;color:var(--text-3);text-align:center;line-height:1.3">
          ${metodo}
        </div>
        <div style="font-size:11px;color:var(--text-2);font-weight:600">
          ${pct}%
        </div>
      </div>`;
    }).join('');

    el.innerHTML = `
    <div style="display:flex;align-items:flex-end;gap:16px;
                padding:10px 4px 0;min-height:${ALTURA_MAX + 70}px">
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