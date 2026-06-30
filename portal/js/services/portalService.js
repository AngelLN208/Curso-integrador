/**
 * portalService.js — Endpoints del portal del paciente
 */
window.PortalService = {
    directorio: async () => {
        const json = await portalFetch('/portal/directorio');
        return json.data ?? json;
    },

    medicosPorEspecialidad: async (especialidadId) => {
        const json = await portalFetch(`/portal/directorio/especialidades/${especialidadId}/medicos`);
        return json.data ?? json;
    },

    agendarCita: async (data) => {
        const json = await portalFetch('/portal/citas', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },

    disponibilidadDias: async (medicoId) => {
        const json = await portalFetch(`/disponibilidad/medico/${medicoId}/dias`);
        return json.data ?? json;
    },

    disponibilidadSlots: async (medicoId, fecha) => {
        const json = await portalFetch(`/disponibilidad/medico/${medicoId}/slots?fecha=${fecha}`);
        return json.data ?? json;
    },
    misCitas: async () => {
        const json = await portalFetch('/portal/citas');
        return json.data ?? json;
    },
    pagarCita: async (data) => {
        const json = await portalFetch('/portal/pagos', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },

    cancelarCita: async (citaId) => {
        const json = await portalFetch(`/portal/citas/${citaId}/cancelar`, {
            method: 'PUT'
        });
        return json.data ?? json;
    },

    reprogramarCita: async (citaId, nuevaFechaHora) => {
        const json = await portalFetch(`/portal/citas/${citaId}/reprogramar`, {
            method: 'PUT',
            body: JSON.stringify({ nuevaFechaHora })
        });
        return json.data ?? json;
    },
    previsualizarPago: async (citaId) => {
        const json = await portalFetch(`/portal/pagos/cita/${citaId}/previsualizar`);
        return json.data ?? json;
    },
    obtenerComprobante: async (citaId) => {
        const json = await portalFetch(`/portal/pagos/cita/${citaId}/comprobante`);
        return json.data ?? json;
    },
    calificarMedico: async (citaId, puntuacion, comentario) => {
        const json = await portalFetch('/portal/valoraciones', {
            method: 'POST',
            body: JSON.stringify({ citaId, puntuacion, comentario: comentario || null })
        });
        return json.data ?? json;
    },
    chatbot: async (mensaje, historial) => {
        const json = await portalFetch('/portal/chatbot', {
            method: 'POST',
            body: JSON.stringify({ mensaje, historial: historial || [] })
        });
        return json.data ?? json;
    },

    obtenerPerfil: async () => {
        const json = await portalFetch('/portal/perfil');
        return json.data ?? json;
    },

    actualizarPerfil: async (data) => {
        const json = await portalFetch('/portal/perfil', {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },
};