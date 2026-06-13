const BASE_URL = 'http://localhost:8080/api';

async function login(username, password) {
    const res = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });

    const json = await res.json();

    if (!res.ok || !json.success) {
        throw new Error(json.message || 'Credenciales incorrectas');
    }

    return json.data; // ← aquí está el LoginResponse real
}