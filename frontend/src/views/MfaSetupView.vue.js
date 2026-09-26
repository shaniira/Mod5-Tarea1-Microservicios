import { onMounted, ref } from 'vue';
import api, { errorMessage } from '@/services/api';
const habilitado = ref(false), setup = ref(null), codigo = ref(''), error = ref(''), ok = ref('');
async function estado() { habilitado.value = (await api.get('/mfa/estado')).data.habilitado; }
async function configurar() { error.value = ''; try {
    setup.value = (await api.post('/mfa/configurar')).data;
}
catch (e) {
    error.value = errorMessage(e);
} }
async function activar() { error.value = ''; try {
    await api.post('/mfa/activar', { codigo: codigo.value });
    setup.value = null;
    codigo.value = '';
    ok.value = 'MFA habilitado correctamente.';
    await estado();
}
catch (e) {
    error.value = errorMessage(e);
} }
async function desactivar() { error.value = ''; try {
    await api.delete('/mfa', { data: { codigo: codigo.value } });
    codigo.value = '';
    ok.value = 'MFA desactivado.';
    await estado();
}
catch (e) {
    error.value = errorMessage(e);
} }
onMounted(async () => { try {
    await estado();
}
catch (e) {
    error.value = errorMessage(e);
} });
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "eyebrow" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error" },
    });
    (__VLS_ctx.error);
}
if (__VLS_ctx.ok) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert success" },
    });
    (__VLS_ctx.ok);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.habilitado ? 'Habilitado' : 'Deshabilitado');
if (!__VLS_ctx.habilitado && !__VLS_ctx.setup) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.configurar) },
        ...{ class: "primary" },
    });
}
if (__VLS_ctx.setup) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
        src: (__VLS_ctx.setup.qrCodeDataUri),
        alt: "QR de Google Authenticator",
        width: "260",
        height: "260",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (__VLS_ctx.setup.secret);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        maxlength: "6",
        inputmode: "numeric",
    });
    (__VLS_ctx.codigo);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.activar) },
        ...{ class: "primary" },
    });
}
if (__VLS_ctx.habilitado) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        maxlength: "6",
        inputmode: "numeric",
    });
    (__VLS_ctx.codigo);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.desactivar) },
        ...{ class: "danger" },
    });
}
/** @type {__VLS_StyleScopedClasses['page-head']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['success']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            habilitado: habilitado,
            setup: setup,
            codigo: codigo,
            error: error,
            ok: ok,
            configurar: configurar,
            activar: activar,
            desactivar: desactivar,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
