import { afterEach, describe, expect, it, vi } from 'vitest';
import { facebookLoginUrl, facebookSessionUrl } from './facebookAuth';

afterEach(() => {
  vi.unstubAllEnvs();
});

describe('facebookAuth urls', () => {
  it('usa la URL configurada por variable de entorno', () => {
    vi.stubEnv('VITE_FACEBOOK_API_URL', 'https://backend.andina.pe/api');

    expect(facebookLoginUrl()).toBe('https://backend.andina.pe/api/auth/facebook');
    expect(facebookSessionUrl()).toBe('https://backend.andina.pe/api/auth/facebook/session');
  });

  it('usa localhost:8083 como valor por defecto en desarrollo', () => {
    vi.stubEnv('VITE_FACEBOOK_API_URL', '');

    expect(facebookLoginUrl()).toBe('http://localhost:8083/api/auth/facebook');
    expect(facebookSessionUrl()).toBe('http://localhost:8083/api/auth/facebook/session');
  });
});
