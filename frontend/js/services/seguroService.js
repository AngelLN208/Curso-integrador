/**
 * seguroService.js — Gestión de seguros médicos y su vínculo con pacientes
 */
window.SeguroService = {
  listar: async () => {
    const json = await apiFetch('/admin/seguros');
    return json.data ?? json;
  },

  vincular: async (pacienteId, seguroId, numeroPoliza) => {
    const params = numeroPoliza
      ? `?numeroPoliza=${encodeURIComponent(numeroPoliza)}`
      : '';
    const json = await apiFetch(
      `/admin/pacientes/${pacienteId}/seguros/${seguroId}${params}`,
      { method: 'POST' }
    );
    return json.data ?? json;
  },

  desvincular: async (pacienteId, vinculoId) => {
    const json = await apiFetch(
      `/admin/pacientes/${pacienteId}/seguros/${vinculoId}`,
      { method: 'DELETE' }
    );
    return json.data ?? json;
  },
  activarSeguro: async (id) => {
    const json = await apiFetch(`/admin/seguros/${id}/activar`, { method: 'PUT' });
    return json.data ?? json;
  },
};