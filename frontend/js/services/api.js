const BASE_URL = 'http://localhost:8080/api';

// Guarda el token después del login
const Auth = {
    getToken: () => localStorage.getItem('token'),
    setToken: (t) => localStorage.setItem('token', t),
    removeToken: () => localStorage.removeItem('token'),
    isLoggedIn: () => !!localStorage.getItem('token')
};

// Fetch con JWT automático
async function apiFetch(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    const token = Auth.getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`${BASE_URL}${endpoint}`, {
        ...options,
        headers
    });

    if (res.status === 401) {
        Auth.removeToken();
        window.location.href = '/auth/login.html';
        return;
    }

    if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error(error.message || `Error ${res.status}`);
    }

    // 204 No Content no tiene body
    if (res.status === 204) return null;
    return res.json();
}