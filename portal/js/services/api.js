/**
 * api.js — Cliente HTTP del portal paciente
 */

const PortalAuth = {
  getToken: () => localStorage.getItem('portal_token'),
  setToken: (t) => localStorage.setItem('portal_token', t),
  removeToken: () => localStorage.removeItem('portal_token'),
  isLoggedIn: () => !!localStorage.getItem('portal_token'),
  getPaciente: () => JSON.parse(localStorage.getItem('portal_paciente') || 'null'),
  setPaciente: (p) => localStorage.setItem('portal_paciente', JSON.stringify(p)),
  removePaciente: () => localStorage.removeItem('portal_paciente'),
};

async function portalFetch(endpoint, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  const token = PortalAuth.getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${PORTAL_CONFIG.API_URL}${endpoint}`, { ...options, headers });

  if (res.status === 401) {
    // Solo redirigir al login si hay un token activo (sesión expirada).
    // Si NO hay token, es un intento de login fallido — no redirigir,
    // dejar que el catch del caller maneje el error y muestre el mensaje.
    if (PortalAuth.getToken()) {
      PortalAuth.removeToken();
      PortalAuth.removePaciente();
      window.location.href = PORTAL_CONFIG.ROUTES.LOGIN;
      return;
    }
    // Sin token: propagar el error normalmente para que el formulario lo muestre
    const json = await res.json().catch(() => ({}));
    throw new Error(json.message || 'Credenciales inválidas');
  }
  if (res.status === 204) return null;

  const json = await res.json().catch(() => ({}));

  if (!res.ok) {
    // Spring puede devolver errores de campo en distintas formas según
    // el GlobalExceptionHandler; intentamos extraer el mensaje más útil.
    let mensaje = json.message || `Error ${res.status}`;

    if (json.errors && Array.isArray(json.errors)) {
      mensaje = json.errors.map(e => e.defaultMessage || e.message || e).join('. ');
    } else if (json.data && typeof json.data === 'object') {
      const camposConError = Object.values(json.data).filter(v => typeof v === 'string');
      if (camposConError.length) mensaje = camposConError.join('. ');
    }

    throw new Error(mensaje);
  }

  return json;
}

const PortalAuthService = {
  login: async (username, password) => {
    const json = await portalFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    const data = json.data ?? json;

    if (data.rol !== 'ROLE_PACIENTE') {
      throw new Error('Esta cuenta no corresponde a un paciente. Usa el portal de empleados.');
    }

    PortalAuth.setToken(data.token);
    PortalAuth.setPaciente({
      username: data.username,
      nombreCompleto: data.nombreCompleto,
    });
    window.location.href = PORTAL_CONFIG.ROUTES.DASHBOARD;
  },

  registrar: async (datos) => {
    const json = await portalFetch('/auth/registro-paciente', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
    const data = json.data ?? json;

    PortalAuth.setToken(data.token);
    PortalAuth.setPaciente({
      username: data.correo,
      nombreCompleto: data.nombreCompleto,
    });
    window.location.href = PORTAL_CONFIG.ROUTES.DASHBOARD;
  },

  logout: () => {
    PortalAuth.removeToken();
    PortalAuth.removePaciente();
    window.location.href = PORTAL_CONFIG.ROUTES.LOGIN;
  },

  requireAuth: () => {
    if (!PortalAuth.isLoggedIn()) window.location.href = PORTAL_CONFIG.ROUTES.LOGIN;
  },
};