import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/services/api', () => ({
  default: { post: vi.fn() },
  errorMessage: (e: unknown) => String(e)
}));

import api from '@/services/api';
import { useAuthStore } from './auth';

function memoryStorage(): Storage {
  const data = new Map<string, string>();
  return {
    get length() {
      return data.size;
    },
    clear: () => data.clear(),
    getItem: (key: string) => data.get(key) ?? null,
    key: () => null,
    removeItem: (key: string) => void data.delete(key),
    setItem: (key: string, value: string) => void data.set(key, value)
  } as Storage;
}

function fakeJwt(payload: Record<string, unknown>): string {
  const part = (obj: unknown) => Buffer.from(JSON.stringify(obj)).toString('base64');
  return `${part({ alg: 'none' })}.${part(payload)}.sig`;
}

beforeEach(() => {
  vi.stubGlobal('localStorage', memoryStorage());
  vi.stubGlobal('sessionStorage', memoryStorage());
  setActivePinia(createPinia());
  vi.mocked(api.post).mockReset();
});

describe('exchangeFacebookTicket', () => {
  it('canjea un ticket válido y deja la sesión iniciada', async () => {
    const token = fakeJwt({ sub: 'cliente@andina.pe', rol: 'CLIENTE' });
    vi.mocked(api.post).mockResolvedValueOnce({ data: { token, tipo: 'Bearer', expiraEnSegundos: 28800 } });

    const store = useAuthStore();
    const ok = await store.exchangeFacebookTicket('ticket-valido');

    expect(ok).toBe(true);
    expect(store.isAuthenticated).toBe(true);
    expect(store.role).toBe('CLIENTE');
    expect(api.post).toHaveBeenCalledWith(
      expect.stringContaining('/auth/facebook/session'),
      null,
      { params: { ticket: 'ticket-valido' } }
    );
  });

  it('propaga el error y no deja sesión parcial cuando el ticket es inválido o expiró', async () => {
    const apiError = {
      isAxiosError: true,
      response: { status: 400, data: { codigo: 'FACEBOOK_TICKET_INVALIDO' } }
    };
    vi.mocked(api.post).mockRejectedValueOnce(apiError);

    const store = useAuthStore();
    await expect(store.exchangeFacebookTicket('ticket-vencido')).rejects.toBe(apiError);

    expect(store.isAuthenticated).toBe(false);
    expect(store.token).toBe('');
  });
});
