import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
const a = useAuthStore(), r = useRouter(), route = useRoute(), open = ref(false);
const items = computed(() => [{ to: '/mi-cuenta', label: 'Mi cuenta', icon: '◈', roles: ['CLIENTE'] }, { to: '/dashboard', label: 'Resumen', icon: '▦', roles: ['ADMIN', 'ACTUARIO', 'AGENTE'] }, { to: '/clientes', label: 'Clientes y vehículos', icon: '◉', roles: ['ADMIN', 'AGENTE'] }, { to: '/tarifas', label: 'Tablas tarifarias', icon: '%', roles: ['ADMIN', 'ACTUARIO'] }, { to: '/cotizaciones', label: 'Cotizaciones', icon: '◎', roles: ['ADMIN', 'AGENTE'] }, { to: '/polizas', label: 'Pólizas y siniestros', icon: '▤', roles: ['ADMIN', 'AGENTE'] }, { to: '/renovaciones', label: 'Renovaciones', icon: '↻', roles: ['ADMIN', 'AGENTE'] }, { to: '/seguridad', label: 'Seguridad MFA', icon: '*' }].filter(i => !i.roles || i.roles.includes(a.role)));
function out() { a.logout(); r.push('/login'); }
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "shell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: (['sidebar', { open: __VLS_ctx.open }]) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "brand" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "brand-mark" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.nav, __VLS_intrinsicElements.nav)({});
for (const [i] of __VLS_getVForSourceType((__VLS_ctx.items))) {
    const __VLS_0 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
        ...{ 'onClick': {} },
        key: (i.to),
        to: (i.to),
    }));
    const __VLS_2 = __VLS_1({
        ...{ 'onClick': {} },
        key: (i.to),
        to: (i.to),
    }, ...__VLS_functionalComponentArgsRest(__VLS_1));
    let __VLS_4;
    let __VLS_5;
    let __VLS_6;
    const __VLS_7 = {
        onClick: (...[$event]) => {
            __VLS_ctx.open = false;
        }
    };
    __VLS_3.slots.default;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (i.icon);
    (i.label);
    var __VLS_3;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "side-footer" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.a.username);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.a.role);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.out) },
    ...{ class: "link-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "topbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.open = !__VLS_ctx.open;
        } },
    ...{ class: "menu" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
(String(__VLS_ctx.route.meta.title || 'Gestión de seguros'));
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "avatar" },
});
(__VLS_ctx.a.username.slice(0, 2).toUpperCase());
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "content" },
});
const __VLS_8 = {}.RouterView;
/** @type {[typeof __VLS_components.RouterView, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({}));
const __VLS_10 = __VLS_9({}, ...__VLS_functionalComponentArgsRest(__VLS_9));
/** @type {__VLS_StyleScopedClasses['shell']} */ ;
/** @type {__VLS_StyleScopedClasses['open']} */ ;
/** @type {__VLS_StyleScopedClasses['sidebar']} */ ;
/** @type {__VLS_StyleScopedClasses['brand']} */ ;
/** @type {__VLS_StyleScopedClasses['brand-mark']} */ ;
/** @type {__VLS_StyleScopedClasses['side-footer']} */ ;
/** @type {__VLS_StyleScopedClasses['link-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
/** @type {__VLS_StyleScopedClasses['menu']} */ ;
/** @type {__VLS_StyleScopedClasses['avatar']} */ ;
/** @type {__VLS_StyleScopedClasses['content']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            a: a,
            route: route,
            open: open,
            items: items,
            out: out,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
