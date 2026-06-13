const PacienteService = {
    listar: async () => {
        const json = await apiFetch('/pacientes');
        return json.data ?? json;
    },

    buscar: async (criterio) => {
        const json = await apiFetch(`/pacientes/buscar?criterio=${encodeURIComponent(criterio)}`);
        return json.data ?? json;
    },

    getById: async (id) => {
        const json = await apiFetch(`/pacientes/${id}`);
        return json.data ?? json;
    },

    crear: async (data) => {
        const json = await apiFetch('/pacientes', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },

    actualizar: async (id, data) => {
        const json = await apiFetch(`/pacientes/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    }
};