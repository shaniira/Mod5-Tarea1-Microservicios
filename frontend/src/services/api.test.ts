import { describe, expect, it } from 'vitest';
import { errorMessage } from './api';

function axiosError(
  status: number,
  data: Record<string, unknown>
) {
  return {
    isAxiosError: true,
    response: {
      status,
      data
    }
  };
}

describe('errorMessage', () => {
  it('muestra el mensaje de documento duplicado enviado por el backend', () => {
    const error = axiosError(422, {
      codigo: 'DOCUMENTO_DUPLICADO',
      mensaje: 'Ya existe un cliente con ese documento'
    });

    expect(errorMessage(error)).toBe(
      'Ya existe un cliente con ese documento'
    );
  });

  it('muestra el mensaje de placa duplicada enviado por el backend', () => {
    const error = axiosError(422, {
      codigo: 'PLACA_DUPLICADA',
      mensaje: 'La placa ya está registrada'
    });

    expect(errorMessage(error)).toBe('La placa ya está registrada');
  });

  it('combina los mensajes de validación por campo', () => {
    const error = axiosError(400, {
      codigo: 'DATOS_INVALIDOS',
      campos: {
        numeroDocumento: 'El documento es obligatorio.',
        nombres: 'El nombre es obligatorio.'
      }
    });

    expect(errorMessage(error)).toBe(
      'El documento es obligatorio. El nombre es obligatorio.'
    );
  });

  it('traduce un error de permisos sin cuerpo', () => {
    expect(errorMessage(axiosError(403, {}))).toBe(
      'No tienes permisos para realizar esta acción.'
    );
  });

  it('muestra el mensaje de ticket de Facebook inválido o expirado', () => {
    const error = axiosError(400, { codigo: 'FACEBOOK_TICKET_INVALIDO' });

    expect(errorMessage(error)).toBe(
      'El enlace de acceso con Facebook no es válido o expiró. Intenta iniciar sesión nuevamente.'
    );
  });

  it('informa cuando no existe conexión con el servidor', () => {
    const error = {
      isAxiosError: true,
      code: 'ERR_NETWORK'
    };

    expect(errorMessage(error)).toContain(
      'No se pudo conectar con el servidor'
    );
  });
});
