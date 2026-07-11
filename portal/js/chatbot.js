/**
 * chatbot.js — Asistente virtual del portal paciente (RF-55)
 */

PortalAuthService.requireAuth();
const pacienteActualChat = PortalAuth.getPaciente();
const nombreCortoChat = pacienteActualChat?.nombreCompleto?.split(' ')[0] || '';
document.getElementById('saludo-usuario').textContent = nombreCortoChat;
document.getElementById('saludo-usuario-mobile').textContent = nombreCortoChat;

// Historial en formato esperado por el backend: [{rol: 'user'|'assistant', contenido: '...'}]
let historialChat = [];

// Cache de especialidades para resolver nombre → id al sugerir especialidad
let especialidadesCache = null;

function mensajeBienvenida() {
    agregarMensaje('assistant',
        `¡Hola${pacienteActualChat?.nombreCompleto ? ', ' + pacienteActualChat.nombreCompleto.split(' ')[0] : ''}! 👋 Soy Stella, el asistente virtual de la Clínica Stella Maris. ` +
        `Puedo ayudarte a resolver dudas sobre la clínica o orientarte hacia la especialidad adecuada según tus síntomas. ¿En qué puedo ayudarte hoy?`,
        null, false);
}

function manejarTeclaChat(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        enviarMensajeChat();
    }
}

async function enviarMensajeChat(mensajeForzado) {
    const input = document.getElementById('chat-input');
    const mensaje = mensajeForzado || input.value.trim();
    if (!mensaje) return;

    agregarMensaje('user', mensaje);
    historialChat.push({ rol: 'user', contenido: mensaje });

    if (!mensajeForzado) {
        input.value = '';
        input.style.height = 'auto';
    }

    const btn = document.getElementById('btn-enviar-chat');
    btn.disabled = true;
    input.disabled = true;

    const idEscribiendo = mostrarEscribiendo();

    try {
        const respuesta = await PortalService.chatbot(mensaje, historialChat.slice(0, -1));

        quitarEscribiendo(idEscribiendo);
        agregarMensaje('assistant', respuesta.respuesta, respuesta.especialidadSugerida,
            !respuesta.generadoPorIA, respuesta.citaPropuesta);
        historialChat.push({ rol: 'assistant', contenido: respuesta.respuesta });

    } catch (err) {
        quitarEscribiendo(idEscribiendo);
        agregarMensaje('assistant',
            'Lo siento, tuve un problema para responder en este momento. Por favor intenta de nuevo en unos segundos.',
            null, true);
    } finally {
        btn.disabled = false;
        input.disabled = false;
        input.focus();
    }
}

// Botón de confirmación: envía un mensaje explícito de confirmación,
// reforzando que la acción del usuario (clic) es la confirmación real.
function confirmarCitaDesdeChat() {
    enviarMensajeChat('Sí, confirmo que quiero agendar esa cita.');
}

function agregarMensaje(rol, texto, especialidadSugerida, esFallback, citaPropuestaJson) {
    const cont = document.getElementById('chat-mensajes');
    const esUsuario = rol === 'user';

    const burbuja = document.createElement('div');
    burbuja.className = `flex ${esUsuario ? 'justify-end' : 'justify-start'}`;

    let botonEspecialidad = '';
    if (especialidadSugerida) {
        botonEspecialidad = `
      <button class="mt-2.5 flex items-center gap-1.5 bg-guia hover:bg-guia/90 text-white text-xs font-semibold rounded-lg px-3.5 py-2 transition"
        onclick="irADirectorioConEspecialidad('${especialidadSugerida.replace(/'/g, "\\'")}')">
        <i class="bi bi-search"></i> Ver médicos de ${especialidadSugerida}
      </button>`;
    }

    let tarjetaCita = '';
    if (citaPropuestaJson) {
        try {
            const cita = JSON.parse(citaPropuestaJson);
            const fecha = new Date(cita.fechaHora);
            const fechaTexto = fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' });
            const horaTexto = fecha.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

            tarjetaCita = `
        <div class="mt-2.5 bg-[#FFFDF9] border border-borde rounded-xl p-3">
          <div class="font-bold text-[13px] text-tinta">${cita.medicoNombre}</div>
          <div class="text-xs text-neblina">${cita.especialidad}</div>
          <div class="text-xs text-neblina mt-1">
            <i class="bi bi-calendar"></i> ${fechaTexto} — ${horaTexto}
          </div>
          <button class="w-full mt-2.5 bg-guia hover:bg-guia/90 text-white text-xs font-semibold rounded-lg py-2 transition flex items-center justify-center gap-1.5"
            onclick="confirmarCitaDesdeChat()">
            <i class="bi bi-check-circle"></i> Sí, confirmar cita
          </button>
        </div>`;
        } catch (e) {
            // Si el JSON viene mal formado, simplemente no mostramos la tarjeta
            console.warn('No se pudo parsear citaPropuesta:', e);
        }
    }

    const claseBurbuja = esUsuario
        ? 'bg-guia text-white rounded-2xl rounded-br-md'
        : 'bg-white/10 text-white rounded-2xl rounded-bl-md';

    burbuja.innerHTML = `
      <div class="max-w-[78%] flex flex-col gap-1 ${esUsuario ? 'items-end' : 'items-start'}">
        <div class="${claseBurbuja} px-4 py-2.5 text-[13.5px] leading-relaxed whitespace-pre-wrap">
          ${escaparHtml(texto)}
          ${botonEspecialidad}
          ${tarjetaCita}
        </div>
        ${esFallback ? '<span class="text-[10.5px] text-white/40">Respuesta automática</span>' : ''}
      </div>`;

    cont.appendChild(burbuja);
    cont.scrollTop = cont.scrollHeight;
}

function mostrarEscribiendo() {
    const cont = document.getElementById('chat-mensajes');
    const id = 'escribiendo-' + Date.now();
    const burbuja = document.createElement('div');
    burbuja.id = id;
    burbuja.className = 'flex justify-start';
    burbuja.innerHTML = `
      <div class="bg-white/10 px-4 py-2.5 rounded-2xl rounded-bl-md">
        <i class="bi bi-three-dots text-white/50"></i>
      </div>`;
    cont.appendChild(burbuja);
    cont.scrollTop = cont.scrollHeight;
    return id;
}

function quitarEscribiendo(id) {
    document.getElementById(id)?.remove();
}

function escaparHtml(texto) {
    const div = document.createElement('div');
    div.textContent = texto;
    return div.innerHTML;
}

// Lleva al directorio con la especialidad sugerida pre-seleccionada
async function irADirectorioConEspecialidad(nombreEspecialidad) {
    sessionStorage.setItem('especialidad_sugerida', nombreEspecialidad);
    window.location.href = 'directorio.html';
}

// Auto-resize del textarea
document.getElementById('chat-input').addEventListener('input', function () {
    this.style.height = 'auto';
    this.style.height = Math.min(this.scrollHeight, 120) + 'px';
});

mensajeBienvenida();