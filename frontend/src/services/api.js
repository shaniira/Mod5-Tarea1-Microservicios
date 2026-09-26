import axios from 'axios';
const messagesByCode = {
    DOCUMENTO_DUPLICADO: 'Ya existe un cliente registrado con ese documento.',
    PLACA_DUPLICADA: 'Ya existe un vehículo registrado con esa placa.',
    USUARIO_DUPLICADO: 'El nombre de usuario ya está registrado.',
    CREDENCIALES_INVALIDAS: 'El usuario o la contraseña son incorrectos.',
    MFA_CODIGO_INVALIDO: 'El código de Google Authenticator no es válido.',
    MFA_DESAFIO_INVALIDO: 'El desafío MFA no es válido o ya fue utilizado.',
    MFA_DESAFIO_EXPIRADO: 'El desafío MFA expiró. Inicia sesión nuevamente.',
    MFA_YA_HABILITADO: 'MFA ya está habilitado.',
    MFA_NO_CONFIGURADO: 'MFA todavía no está configurado.',
    GOOGLE_TOKEN_INVALIDO: 'No se pudo validar tu cuenta de Google. Intenta nuevamente.',
    GOOGLE_EMAIL_NO_VERIFICADO: 'Tu correo de Google no está verificado.',
    FACEBOOK_TICKET_INVALIDO: 'El enlace de acceso con Facebook no es válido o expiró. Intenta iniciar sesión nuevamente.',
    USUARIO_INACTIVO: 'Tu cuenta está inactiva. Contacta al administrador.',
    CLIENTE_NO_REGISTRADO: 'Tu correo no está registrado como cliente de Andina Seguros. Contacta a un agente para registrarte.',
    ACCESO_DENEGADO: 'No tienes permisos para realizar esta acción.',
    RECURSO_NO_ENCONTRADO: 'No se encontró la información solicitada.',
    VEHICULO_NO_PERTENECE: 'El vehículo seleccionado no pertenece al cliente.',
    TABLA_NO_DISPONIBLE: 'No existe una tabla tarifaria vigente para este vehículo.',
    TABLA_NO_VIGENTE: 'La tabla tarifaria seleccionada no está vigente.',
    TABLA_NO_APLICA: 'La tabla tarifaria no aplica al vehículo seleccionado.',
    COTIZACION_NO_VIGENTE: 'La cotización ya no está vigente.',
    COTIZACION_NO_ACEPTADA: 'Debes aceptar la cotización antes de emitir la póliza.',
    COTIZACION_YA_EMITIDA: 'La cotización ya fue emitida.',
    POLIZA_NO_VIGENTE: 'Esta operación solo está disponible para pólizas vigentes.',
    SINIESTRO_NO_PERTENECE: 'El siniestro no pertenece a la póliza seleccionada.',
    SINIESTRO_CERRADO: 'No puedes modificar un siniestro cerrado.',
    SINIESTROS_PENDIENTES: 'No puedes renovar una póliza con siniestros pendientes.',
    RENOVACION_NO_APROBADA: 'Debes aprobar la renovación antes de generar la póliza.',
    RENOVACION_YA_DECIDIDA: 'La propuesta de renovación ya fue aprobada o rechazada.',
    RENOVACION_VENCIDA: 'La propuesta de renovación está vencida.',
    POLIZA_RENOVADA_EXISTENTE: 'Esta renovación ya generó una nueva póliza.',
    DATOS_DUPLICADOS: 'Ya existe un registro con la información ingresada.',
    ERROR_INTERNO: 'No pudimos completar la operación. Inténtalo nuevamente.'
};
const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8083/api',
    timeout: 15000,
    headers: { 'Content-Type': 'application/json' }
});
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token)
        config.headers.Authorization = `Bearer ${token}`;
    return config;
});
api.interceptors.response.use(response => response, error => {
    const status = error.response?.status;
    const code = error.response?.data?.codigo;
    if (status === 401 || (status === 403 && code === 'TOKEN_INVALIDO')) {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('username');
        if (location.pathname !== '/login')
            location.href = '/login?session=expired';
    }
    return Promise.reject(error);
});
function validationMessage(campos) {
    if (!campos || Object.keys(campos).length === 0) {
        return '';
    }
    return Object.values(campos).join(' ');
}
export function errorMessage(error) {
    if (!axios.isAxiosError(error)) {
        return error instanceof Error
            ? error.message
            : 'Ocurrió un error inesperado. Inténtalo nuevamente.';
    }
    if (!error.response) {
        if (error.code === 'ECONNABORTED') {
            return 'El servidor tardó demasiado en responder. Inténtalo nuevamente.';
        }
        return 'No se pudo conectar con el servidor. Verifica tu conexión e inténtalo nuevamente.';
    }
    const data = error.response.data;
    const fieldMessage = validationMessage(data?.campos);
    if (fieldMessage) {
        return fieldMessage;
    }
    if (data?.mensaje) {
        return data.mensaje;
    }
    if (data?.codigo && messagesByCode[data.codigo]) {
        return messagesByCode[data.codigo];
    }
    if (data?.message || data?.detalle || data?.error) {
        return data.message || data.detalle || data.error || '';
    }
    if (error.response.status === 403) {
        return 'No tienes permisos para realizar esta acción.';
    }
    if (error.response.status === 404) {
        return 'No se encontró la información solicitada.';
    }
    if (error.response.status >= 500) {
        return 'El servidor no pudo completar la operación. Inténtalo nuevamente.';
    }
    return 'No se pudo completar la operación. Revisa los datos e inténtalo nuevamente.';
}
export default api;
