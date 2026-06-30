/**
 * chatbot.js — Asistente virtual del portal paciente (RF-55)
 */

PortalAuthService.requireAuth();
const pacienteActualChat = PortalAuth.getPaciente();
document.getElementById('saludo-usuario').textContent = pacienteActualChat?.nombreCompleto?.split(' ')[0] || '';

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
    burbuja.style.cssText = `display:flex;${esUsuario ? 'justify-content:flex-end' : 'justify-content:flex-start'}`;

    let botonEspecialidad = '';
    if (especialidadSugerida) {
        botonEspecialidad = `
      <button class="btn btn-primary btn-sm" style="margin-top:10px"
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
        <div style="margin-top:10px;background:white;border:1px solid var(--border);border-radius:10px;padding:12px">
          <div style="font-weight:700;font-size:13px">${cita.medicoNombre}</div>
          <div style="font-size:12px;color:var(--text-2)">${cita.especialidad}</div>
          <div style="font-size:12px;color:var(--text-2);margin-top:4px">
            <i class="bi bi-calendar"></i> ${fechaTexto} — ${horaTexto}
          </div>
          <button class="btn btn-primary btn-sm btn-block" style="margin-top:10px"
            onclick="confirmarCitaDesdeChat()">
            <i class="bi bi-check-circle"></i> Sí, confirmar cita
          </button>
        </div>`;
        } catch (e) {
            // Si el JSON viene mal formado, simplemente no mostramos la tarjeta
            console.warn('No se pudo parsear citaPropuesta:', e);
        }
    }

    burbuja.innerHTML = `
      <div style="max-width:78%;display:flex;flex-direction:column;gap:4px;${esUsuario ? 'align-items:flex-end' : 'align-items:flex-start'}">
        <div style="background:${esUsuario ? 'var(--primary)' : 'var(--bg-soft)'};
                    color:${esUsuario ? 'white' : 'var(--text)'};
                    padding:11px 15px;border-radius:14px;
                    border-bottom-${esUsuario ? 'right' : 'left'}-radius:4px;
                    font-size:13.5px;line-height:1.5;white-space:pre-wrap">
          ${escaparHtml(texto)}
          ${botonEspecialidad}
          ${tarjetaCita}
        </div>
        ${esFallback ? '<span style="font-size:10.5px;color:var(--text-3)">Respuesta automática</span>' : ''}
      </div>`;

    cont.appendChild(burbuja);
    cont.scrollTop = cont.scrollHeight;
}

function mostrarEscribiendo() {
    const cont = document.getElementById('chat-mensajes');
    const id = 'escribiendo-' + Date.now();
    const burbuja = document.createElement('div');
    burbuja.id = id;
    burbuja.style.cssText = 'display:flex;justify-content:flex-start';
    burbuja.innerHTML = `
      <div style="background:var(--bg-soft);padding:11px 15px;border-radius:14px;border-bottom-left-radius:4px">
        <i class="bi bi-three-dots" style="color:var(--text-3)"></i>
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