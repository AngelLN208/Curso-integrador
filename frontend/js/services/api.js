/**
 * api.js — Cliente HTTP centralizado
 * Reemplaza: api.js + auth.service.js + authService.js (estaban duplicados)
 * Requiere: config.js cargado antes en el HTML.
 */

const Auth = {
  getToken: () => localStorage.getItem('token'),
  setToken: (t) => localStorage.setItem('token', t),
  removeToken: () => localStorage.removeItem('token'),
  isLoggedIn: () => !!localStorage.getItem('token'),
  getUsuario: () => JSON.parse(localStorage.getItem('usuario') || 'null'),
  setUsuario: (u) => localStorage.setItem('usuario', JSON.stringify(u)),
  removeUsuario: () => localStorage.removeItem('usuario'),
};

async function apiFetch(endpoint, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  const token = Auth.getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${CONFIG.API_URL}${endpoint}`, { ...options, headers });

  if (res.status === 401) {
    Auth.removeToken();
    Auth.removeUsuario();
    window.location.href = CONFIG.ROUTES.LOGIN;
    return;
  }
  if (res.status === 204) return null;

  const json = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(json.message || `Error ${res.status}`);
  return json;
}

const AuthService = {
  login: async (username, password) => {
    const json = await apiFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    const data = json.data ?? json;
    Auth.setToken(data.token);
    Auth.setUsuario({
      username: data.username,
      nombreCompleto: data.nombreCompleto,
      rol: data.rol,
      medicoId: data.medicoId ?? null
    });
    AuthService._redirigir(data.rol);
  },

  /**
   * Cierra sesión: invalida el token en el backend (RNF-02) y luego
   * limpia el almacenamiento local y redirige al login.
   * Si la llamada al backend falla (sin conexión, servidor caído),
   * el logout local se completa igual — no queremos bloquear al
   * usuario de salir de su cuenta por un problema de red.
   */
  logout: async () => {
    const token = Auth.getToken();
    if (token) {
      try {
        await fetch(`${CONFIG.API_URL}/auth/logout`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` },
        });
      } catch (err) {
        // Falla silenciosa: el logout local continúa de todas formas
      }
    }
    Auth.removeToken();
    Auth.removeUsuario();
    window.location.href = CONFIG.ROUTES.LOGIN;
  },

  requireAuth: () => {
    if (!Auth.isLoggedIn()) window.location.href = CONFIG.ROUTES.LOGIN;
  },

  _redirigir: (rol) => {
    const destinos = {
      ROLE_RECEPCIONISTA: CONFIG.ROUTES.RECEP_DASHBOARD,
      ROLE_MEDICO: CONFIG.ROUTES.MEDICO_DASHBOARD,
      ROLE_ADMINISTRADOR: CONFIG.ROUTES.ADMIN_DASHBOARD,
    };
    if (destinos[rol]) window.location.href = destinos[rol];
    else throw new Error('Rol no reconocido: ' + rol);
  },
};