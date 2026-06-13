const PagoService = {
    registrar: (data) =>
        apiFetch('/pagos', {
            method: 'POST',
            body: JSON.stringify(data)
        }),

    getPorPaciente: (pacienteId) =>
        apiFetch(`/pagos/paciente/${pacienteId}`)
};