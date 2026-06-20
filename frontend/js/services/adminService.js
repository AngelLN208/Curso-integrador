/**
 * adminService.js — Servicios del panel de administración
 */
const AdminService = {
    // ── Médicos / Horarios / Especialidades / Seguros (ya existían) ──
    registrarMedico: (data) =>
        apiFetch('/medicos', { method: 'POST', body: JSON.stringify(data) }),

    asignarHorario: (data) =>
        apiFetch('/horarios', { method: 'POST', body: JSON.stringify(data) }),

    registrarEspecialidad: (data) =>
        apiFetch('/especialidades', { method: 'POST', body: JSON.stringify(data) }),

    registrarSeguro: (data) =>
        apiFetch('/admin/seguros', { method: 'POST', body: JSON.stringify(data) }),

    getAuditoriaCita: (citaId) =>
        apiFetch(`/admin/auditoria/cita/${citaId}`),

    // ── Usuarios del sistema ──────────────────────────────────────
    listarUsuarios: async () => {
        const json = await apiFetch('/admin/usuarios');
        return json.data ?? json;
    },

    // ── Usuarios del sistema ──────────────────────────────────────
    listarUsuarios: async () => {
        const json = await apiFetch('/admin/usuarios');
        return json.data ?? json;
    },

    crearUsuario: async (data) => {
        const json = await apiFetch('/admin/usuarios', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return json.data ?? json;
    },

    cambiarEstadoUsuario: async (id, activo) => {
        const json = await apiFetch(`/admin/usuarios/${id}/estado?activo=${activo}`, {
            method: 'PUT'
        });
        return json.data ?? json;
    },
    cambiarEstadoUsuario: async (id, activo) => {
        const json = await apiFetch(`/admin/usuarios/${id}/estado?activo=${activo}`, {
            method: 'PUT'
        });
        return json.data ?? json;
    },



    // ── Auditoría ─────────────────────────────────────────────────
    filtrarAuditoria: async (usuarioId, tipoAccion) => {
        const params = new URLSearchParams();
        if (usuarioId) params.append('usuarioId', usuarioId);
        if (tipoAccion) params.append('tipoAccion', tipoAccion);
        const json = await apiFetch(`/admin/auditoria/filtrar?${params.toString()}`);
        return json.data ?? json;
    },

    // ── Seguros (catálogo completo, incluye listar) ──────────────
    listarSeguros: async () => {
        const json = await apiFetch('/admin/seguros');
        return json.data ?? json;
    },

    desactivarSeguro: async (id) => {
        const json = await apiFetch(`/admin/seguros/${id}`, { method: 'DELETE' });
        return json.data ?? json;
    }
};