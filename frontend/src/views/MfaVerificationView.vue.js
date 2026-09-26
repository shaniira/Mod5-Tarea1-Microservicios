import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { errorMessage } from '@/services/api';
const codigo = ref(''), loading = ref(false), error = ref(''), a = useAuthStore(), r = useRouter();
async function verificar() { loading.value = true; error.value = ''; try {
    await a.verifyMfa(codigo.value);
    r.push(a.role === 'CLIENTE' ? '/mi-cuenta' : '/dashboard');
}
catch (e) {
    error.value = errorMessage(e);
}
finally {
    loading.value = false;
} }
function cancelar() { a.logout(); r.push('/login'); }
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.verificar) },
    ...{ class: "login-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "logo-large" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    inputmode: "numeric",
    autocomplete: "one-time-code",
    maxlength: "6",
    pattern: "[0-9]{6}",
    required: true,
    autofocus: true,
});
(__VLS_ctx.codigo);
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
(__VLS_ctx.loading ? 'Verificando…' : 'Verificar');
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.cancelar) },
    type: "button",
    ...{ class: "link-btn" },
});
/** @type {__VLS_StyleScopedClasses['login-page']} */ ;
/** @type {__VLS_StyleScopedClasses['login-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['login-card']} */ ;
/** @type {__VLS_StyleScopedClasses['logo-large']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['wide']} */ ;
/** @type {__VLS_StyleScopedClasses['link-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            codigo: codigo,
            loading: loading,
            error: error,
            verificar: verificar,
            cancelar: cancelar,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
