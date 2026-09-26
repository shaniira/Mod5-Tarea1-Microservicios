import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { errorMessage } from '@/services/api';
import { facebookLoginUrl } from '@/services/facebookAuth';
const u = ref('admin'), p = ref('Admin123*'), loading = ref(false), error = ref('');
const a = useAuthStore(), r = useRouter(), ruta = useRoute();
async function go() { loading.value = true; error.value = ''; try {
    const ok = await a.login(u.value.trim(), p.value);
    r.push(ok ? (a.role === 'CLIENTE' ? '/mi-cuenta' : '/dashboard') : '/mfa-verification');
}
catch (e) {
    error.value = errorMessage(e);
}
finally {
    loading.value = false;
} }
async function onGoogleCredential(response) { loading.value = true; error.value = ''; try {
    await a.loginWithGoogle(response.credential);
    r.push(a.role === 'CLIENTE' ? '/mi-cuenta' : '/dashboard');
}
catch (e) {
    error.value = errorMessage(e);
}
finally {
    loading.value = false;
} }
// El backend gestiona todo el intercambio OAuth (code/state/App Secret); el frontend solo redirige.
function onFacebookLogin() { window.location.assign(facebookLoginUrl()); }
async function onFacebookTicket(ticket) { loading.value = true; error.value = ''; try {
    await a.exchangeFacebookTicket(ticket);
    await r.replace({ path: '/login', query: {} });
    r.push(a.role === 'CLIENTE' ? '/mi-cuenta' : '/dashboard');
}
catch (e) {
    await r.replace({ path: '/login', query: {} });
    error.value = errorMessage(e);
}
finally {
    loading.value = false;
} }
onMounted(() => {
    const ticket = ruta.query.ticket;
    if (typeof ticket === 'string' && ticket) {
        onFacebookTicket(ticket);
        return;
    }
    if (ruta.query.error) {
        r.replace({ path: '/login', query: {} });
        error.value = 'No se pudo completar el inicio de sesión con Facebook.';
    }
});
onMounted(() => { let intentos = 0; const timer = setInterval(() => { const google = window.google; if (google) {
    clearInterval(timer);
    google.accounts.id.initialize({ client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID, callback: onGoogleCredential });
    google.accounts.id.renderButton(document.getElementById('google-btn'), { theme: 'outline', size: 'large', width: 320, text: 'continue_with' });
}
else if (++intentos > 50) {
    clearInterval(timer);
} }, 100); });
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-art" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "art-copy" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "eyebrow" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "art-metrics" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.go) },
    ...{ class: "login-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "logo-large" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    autocomplete: "username",
    required: true,
});
(__VLS_ctx.u);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "password",
    autocomplete: "current-password",
    required: true,
});
(__VLS_ctx.p);
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error" },
    });
    (__VLS_ctx.error);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "primary wide" },
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.loading ? 'Ingresando…' : 'Iniciar sesión');
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.onFacebookLogin) },
    type: "button",
    ...{ class: "fb-btn" },
    disabled: (__VLS_ctx.loading),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "google-divider" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    id: "google-btn",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
/** @type {__VLS_StyleScopedClasses['login-page']} */ ;
/** @type {__VLS_StyleScopedClasses['login-art']} */ ;
/** @type {__VLS_StyleScopedClasses['art-copy']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['art-metrics']} */ ;
/** @type {__VLS_StyleScopedClasses['login-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['login-card']} */ ;
/** @type {__VLS_StyleScopedClasses['logo-large']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['wide']} */ ;
/** @type {__VLS_StyleScopedClasses['fb-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['google-divider']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            u: u,
            p: p,
            loading: loading,
            error: error,
            go: go,
            onFacebookLogin: onFacebookLogin,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
