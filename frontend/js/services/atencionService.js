/**
 * atencionService.js — Triaje, consultas e historial médico
 */
window.AtencionService = {
    registrarTriaje: async (data) => {
        const json = await apiFetch('/atencion/triaje', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },

    registrarConsulta: async (data) => {
        const json = await apiFetch('/atencion/consulta', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },

    editarConsulta: async (citaId, data) => {
        const json = await apiFetch(`/atencion/consulta/${citaId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },
    editarTriaje: async (citaId, data) => {
        const json = await apiFetch(`/atencion/triaje/${citaId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },

    historial: async (pacienteId) => {
        const json = await apiFetch(`/atencion/historial/${pacienteId}`);
        return json.data ?? json;
    }

};