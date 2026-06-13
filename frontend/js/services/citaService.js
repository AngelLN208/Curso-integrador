const CitaService = {
    listar: async () => {
        const json = await apiFetch('/citas');
        return json.data ?? json; // si viene envuelto en ApiResponse saca .data
    },

    buscar: async (filtros) => {
        const params = new URLSearchParams(filtros).toString();
        const json = await apiFetch(`/citas/buscar?${params}`);
        return json.data ?? json;
    },

    getById: async (id) => {
        const json = await apiFetch(`/citas/${id}`);
        return json.data ?? json;
    },

    crear: async (data) => {
        const json = await apiFetch('/citas', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },

    reprogramar: async (id, data) => {
        const json = await apiFetch(`/citas/${id}/reprogramar`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },

    cancelar: async (id, motivo) => {
        const json = await apiFetch(`/citas/${id}/cancelar`, {
            method: 'PUT',
            body: JSON.stringify({ motivo })
        });
        return json.data ?? json;
    }
};