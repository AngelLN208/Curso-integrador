// js/services/medicoService.js

const MedicoService = {
    listar: async () => {
        const json = await apiFetch('/medicos');
        return json.data ?? json;
    },

    listarTodos: async () => {
        const json = await apiFetch('/medicos/todos');
        return json.data ?? json;
    },

    listarPorEspecialidad: async (especialidadId) => {
        const json = await apiFetch(`/medicos/especialidad/${especialidadId}`);
        return json.data ?? json;
    },

    getById: async (id) => {
        const json = await apiFetch(`/medicos/${id}`);
        return json.data ?? json;
    },

    activar: async (id) => {
        const json = await apiFetch(`/medicos/${id}/activar`, { method: 'PUT' });
        return json.data ?? json;
    }
};