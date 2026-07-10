/**
 * horario.js (médico) — Mi horario semanal
 */

AuthService.requireAuth();
const usuario = Auth.getUsuario();
iniciarSidebar('Mi horario');

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

// ── Orden y etiquetas de días ──────────────────────────────────
const DIAS_ORDEN = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const DIAS_LABEL = {
  MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves', FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo'
};

// ── Cargar horario ──────────────────────────────────────────────
async function cargarHorario() {
  const cont = document.getElementById('grid-horario');

  if (!usuario.medicoId) {
    cont.innerHTML = `<div class="text-center text-neblina py-10 text-[13px]">No se encontró tu perfil de médico</div>`;
    return;
  }

  try {
    const horarios = await HorarioService.listarPorMedico(usuario.medicoId);
    calcularMetricas(horarios);
    renderGrid(horarios);
  } catch (err) {
    cont.innerHTML = `<div class="text-center text-neblina py-10 text-[13px]">No se pudo cargar tu horario</div>`;
  }
}

function calcularMetricas(horarios) {
  const diasUnicos = new Set(horarios.map(h => h.dia));
  document.getElementById('m-dias-activos').textContent = diasUnicos.size;

  let totalMinutos = 0;
  horarios.forEach(h => {
    const inicio = parseHora(h.horaInicio);
    const fin = parseHora(h.horaFin);
    totalMinutos += (fin - inicio);
  });

  const horas = Math.round(totalMinutos / 60 * 10) / 10;
  document.getElementById('m-horas-semana').textContent = `${horas}h`;

  const slots = Math.floor(totalMinutos / 45);
  document.getElementById('m-slots-disponibles').textContent = slots;
}

function parseHora(horaStr) {
  // horaStr viene como "08:00:00" o "08:00"
  const [h, m] = horaStr.split(':').map(Number);
  return h * 60 + m;
}

function formatHora(horaStr) {
  const [h, m] = horaStr.split(':').map(Number);
  const periodo = h < 12 ? 'a.m.' : 'p.m.';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${m.toString().padStart(2, '0')} ${periodo}`;
}

function renderGrid(horarios) {
  const cont = document.getElementById('grid-horario');

  if (!horarios.length) {
    cont.innerHTML = `<div class="text-center text-neblina py-10 text-[13px]">
      <i class="bi bi-calendar-x block text-2xl mb-2"></i>
      Aún no tienes un horario asignado. Contacta al administrador.
    </div>`;
    return;
  }

  // Agrupar por día
  const porDia = {};
  horarios.forEach(h => {
    if (!porDia[h.dia]) porDia[h.dia] = [];
    porDia[h.dia].push(h);
  });

  const diasConHorario = DIAS_ORDEN.filter(d => porDia[d]);

  cont.innerHTML = `
    <div class="grid gap-3" style="grid-template-columns:repeat(${diasConHorario.length}, 1fr)">
      ${diasConHorario.map(dia => `
        <div class="border border-borde dark:border-borde-dark rounded-lg overflow-hidden">
          <div class="bg-guia text-white py-2.5 text-center font-semibold text-[13px]">
            ${DIAS_LABEL[dia]}
          </div>
          <div class="p-3.5 space-y-2.5">
            ${porDia[dia].map(h => `
              <div class="bg-guia/10 rounded-lg p-2.5 text-center">
                <div class="text-xs text-neblina mb-0.5">
                  <i class="bi bi-clock"></i>
                </div>
                <div class="text-[13px] font-semibold text-guia">
                  ${formatHora(h.horaInicio)}
                </div>
                <div class="text-[11px] text-neblina my-0.5">a</div>
                <div class="text-[13px] font-semibold text-guia">
                  ${formatHora(h.horaFin)}
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      `).join('')}
    </div>`;
}

cargarHorario();