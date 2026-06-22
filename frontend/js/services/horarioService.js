/**
 * horarioService.js — Horarios de médicos
 */
window.HorarioService = {
    listarPorMedico: async (medicoId) => {
        const json = await apiFetch(`/horarios/medico/${medicoId}`);
        return json.data ?? json;
    },

    asignar: async (data) => {
        const json = await apiFetch('/horarios', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },

    eliminar: async (id) => {
        const json = await apiFetch(`/horarios/${id}`, { method: 'DELETE' });
        return json.data ?? json;
    }
};