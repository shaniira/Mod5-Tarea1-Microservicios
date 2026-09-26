import { defineStore } from 'pinia';
import api from '@/services/api';
import { facebookSessionUrl } from '@/services/facebookAuth';
import type { Role } from '@/types';

function parseJwt(token: string): Record<string, any> {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(decodeURIComponent(escape(atob(payload))));
  } catch {
    return {};
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    role: (localStorage.getItem('role') || '') as Role | '',
    username: localStorage.getItem('username') || '',
    nombres: localStorage.getItem('nombres') || '',
    apellidos: localStorage.getItem('apellidos') || '',
    mfaChallenge: sessionStorage.getItem('mfaChallenge') || ''
  }),
  getters: {
    isAuthenticated: state => Boolean(state.token),
    displayName: state => (state.nombres ? `${state.nombres} ${state.apellidos}`.trim() : state.username)
  },
  actions: {
    async login(username: string, password: string) {
      const { data } = await api.post('/auth/login', { username, password });
      if (data.requiresMfa) {
        this.mfaChallenge = data.challengeToken;
        sessionStorage.setItem('mfaChallenge', data.challengeToken);
        return false;
      }
      await this.setSession(data.token.token, username);
      return true;
    },
    async loginWithGoogle(idToken: string) {
      const { data } = await api.post('/auth/google', { idToken });
      await this.setSession(data.token, parseJwt(data.token).sub || '');
      return true;
    },
    // El backend ya validó code/state con Facebook; el ticket temporal solo se canjea una vez por el JWT interno.
    async exchangeFacebookTicket(ticket: string) {
      const { data } = await api.post(facebookSessionUrl(), null, { params: { ticket } });
      await this.setSession(data.token, parseJwt(data.token).sub || '');
      return true;
    },
    async verifyMfa(codigo: string) {
      const { data } = await api.post('/auth/mfa/verificar', { challengeToken: this.mfaChallenge, codigo });
      await this.setSession(data.token, parseJwt(data.token).sub || '');
      this.mfaChallenge = '';
      sessionStorage.removeItem('mfaChallenge');
    },
    async setSession(token: string, username: string) {
      const payload = parseJwt(token);

      this.token = token;
      this.role = (payload.rol || payload.role || payload.roles?.[0]?.replace('ROLE_', '') || '') as Role;
      this.username = username;

      localStorage.setItem('token', this.token);
      localStorage.setItem('role', this.role);
      localStorage.setItem('username', username);

      await this.fetchPerfil();
    },
    async fetchPerfil() {
      try {
        const { data } = await api.get('/auth/me');
        this.nombres = data.nombres || '';
        this.apellidos = data.apellidos || '';
        localStorage.setItem('nombres', this.nombres);
        localStorage.setItem('apellidos', this.apellidos);
      } catch {
        // El nombre es solo informativo: si falla, la sesión sigue funcionando con el username.
      }
    },
    logout() {
      this.token = '';
      this.role = '';
      this.username = '';
      this.nombres = '';
      this.apellidos = '';
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      localStorage.removeItem('username');
      localStorage.removeItem('nombres');
      localStorage.removeItem('apellidos');
      this.mfaChallenge = '';
      sessionStorage.removeItem('mfaChallenge');
    }
  }
});
