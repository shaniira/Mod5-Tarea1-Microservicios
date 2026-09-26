import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter } from 'vue-router';
import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/services/api', async () => {
  const actual = await vi.importActual<typeof import('@/services/api')>('@/services/api');
  return { ...actual, default: { post: vi.fn() } };
});

import api from '@/services/api';
import { facebookLoginUrl } from '@/services/facebookAuth';
import LoginView from './LoginView.vue';

function fakeJwt(payload: Record<string, unknown>): string {
  const part = (obj: unknown) => Buffer.from(JSON.stringify(obj)).toString('base64');
  return `${part({ alg: 'none' })}.${part(payload)}.sig`;
}

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: LoginView },
      { path: '/dashboard', component: { template: '<div>dashboard</div>' } },
      { path: '/mi-cuenta', component: { template: '<div>mi-cuenta</div>' } }
    ]
  });
}

beforeEach(() => {
  localStorage.clear();
  sessionStorage.clear();
  vi.mocked(api.post).mockReset();
  vi.stubGlobal('google', undefined);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('LoginView - Facebook', () => {
  it('el botón "Iniciar con Facebook" redirige el navegador a la URL de inicio de OAuth', async () => {
    const assignSpy = vi.fn();
    vi.stubGlobal('location', { ...window.location, assign: assignSpy });

    const router = makeRouter();
    router.push('/login');
    await router.isReady();
    const wrapper = mount(LoginView, { global: { plugins: [createPinia(), router] } });

    await wrapper.find('.fb-btn').trigger('click');

    expect(assignSpy).toHaveBeenCalledWith(facebookLoginUrl());
  });

  it('canjea un ticket válido y redirige a la página principal', async () => {
    const token = fakeJwt({ sub: 'agente@andina.pe', rol: 'AGENTE' });
    vi.mocked(api.post).mockResolvedValueOnce({ data: { token, tipo: 'Bearer', expiraEnSegundos: 28800 } });

    const router = makeRouter();
    router.push('/login?ticket=ticket-valido');
    await router.isReady();
    mount(LoginView, { global: { plugins: [createPinia(), router] } });

    await flushPromises();

    expect(api.post).toHaveBeenCalledWith(
      expect.stringContaining('/auth/facebook/session'),
      null,
      { params: { ticket: 'ticket-valido' } }
    );
    expect(router.currentRoute.value.path).toBe('/dashboard');
    expect(router.currentRoute.value.query.ticket).toBeUndefined();
  });

  it('muestra un error y limpia la URL cuando el ticket es inválido o expiró', async () => {
    vi.mocked(api.post).mockRejectedValueOnce({
      isAxiosError: true,
      response: { status: 400, data: { codigo: 'FACEBOOK_TICKET_INVALIDO' } }
    });

    const router = makeRouter();
    router.push('/login?ticket=ticket-vencido');
    await router.isReady();
    const wrapper = mount(LoginView, { global: { plugins: [createPinia(), router] } });

    await flushPromises();

    expect(wrapper.find('.alert.error').text()).toContain(
      'El enlace de acceso con Facebook no es válido o expiró'
    );
    expect(router.currentRoute.value.path).toBe('/login');
    expect(router.currentRoute.value.query.ticket).toBeUndefined();
  });
});
