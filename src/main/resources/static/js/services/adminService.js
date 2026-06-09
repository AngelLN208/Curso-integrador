const AdminService = {
    registrarMedico: (data) =>
        apiFetch('/medicos', { method: 'POST', body: JSON.stringify(data) }),

    asignarHorario: (data) =>
        apiFetch('/horarios', { method: 'POST', body: JSON.stringify(data) }),

    registrarEspecialidad: (data) =>
        apiFetch('/especialidades', { method: 'POST', body: JSON.stringify(data) }),

    registrarSeguro: (data) =>
        apiFetch('/admin/seguros', { method: 'POST', body: JSON.stringify(data) }),

    getAuditoriaCita: (citaId) =>
        apiFetch(`/admin/auditoria/cita/${citaId}`)
};