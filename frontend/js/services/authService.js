const AuthService = {
    login: async (username, password) => {
        const data = await apiFetch('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
        Auth.setToken(data.token);
        return data;
    },

    logout: () => {
        Auth.removeToken();
        window.location.href = '/auth/login.html';
    }
};