const AtencionService = {
    registrarTriaje: (data) =>
        apiFetch('/atencion/triaje', {
            method: 'POST',
            body: JSON.stringify(data)
        }),

    registrarConsulta: (data) =>
        apiFetch('/atencion/consulta', {
            method: 'POST',
            body: JSON.stringify(data)
        }),

    getHistorial: (pacienteId) =>
        apiFetch(`/atencion/historial/${pacienteId}`)
};