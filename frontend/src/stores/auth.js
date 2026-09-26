import { defineStore } from 'pinia';
import api from '@/services/api';
import { facebookSessionUrl } from '@/services/facebookAuth';
function parseJwt(token) {
    try {
        const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(decodeURIComponent(escape(atob(payload))));
    }
    catch {
        return {};
    }
}
export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('token') || '',
        role: (localStorage.getItem('role') || ''),
        username: localStorage.getItem('username') || '',
        mfaChallenge: sessionStorage.getItem('mfaChallenge') || ''
    }),
    getters: {
        isAuthenticated: state => Boolean(state.token)
    },
    actions: {
        async login(username, password) {
            const { data } = await api.post('/auth/login', { username, password });
            if (data.requiresMfa) {
                this.mfaChallenge = data.challengeToken;
                sessionStorage.setItem('mfaChallenge', data.challengeToken);
                return false;
            }
            this.setSession(data.token.token, username);
            return true;
        },
        async loginWithGoogle(idToken) {
            const { data } = await api.post('/auth/google', { idToken });
            this.setSession(data.token, parseJwt(data.token).sub || '');
            return true;
        },
        // El backend ya validó code/state con Facebook; el ticket temporal solo se canjea una vez por el JWT interno.
        async exchangeFacebookTicket(ticket) {
            const { data } = await api.post(facebookSessionUrl(), null, { params: { ticket } });
            this.setSession(data.token, parseJwt(data.token).sub || '');
            return true;
        },
        async verifyMfa(codigo) {
            const { data } = await api.post('/auth/mfa/verificar', { challengeToken: this.mfaChallenge, codigo });
            this.setSession(data.token, parseJwt(data.token).sub || '');
            this.mfaChallenge = '';
            sessionStorage.removeItem('mfaChallenge');
        },
        setSession(token, username) {
            const payload = parseJwt(token);
            this.token = token;
            this.role = (payload.rol || payload.role || payload.roles?.[0]?.replace('ROLE_', '') || '');
            this.username = username;
            localStorage.setItem('token', this.token);
            localStorage.setItem('role', this.role);
            localStorage.setItem('username', username);
        },
        logout() {
            this.token = '';
            this.role = '';
            this.username = '';
            localStorage.removeItem('token');
            localStorage.removeItem('role');
            localStorage.removeItem('username');
            this.mfaChallenge = '';
            sessionStorage.removeItem('mfaChallenge');
        }
    }
});
