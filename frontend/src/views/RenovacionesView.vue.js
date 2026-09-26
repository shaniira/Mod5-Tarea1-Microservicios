import { computed, onMounted, ref } from 'vue';
import api, { errorMessage } from '@/services/api';
import StatusBadge from '@/components/StatusBadge.vue';
import UiModal from '@/components/UiModal.vue';
const policies = ref([]), history = ref([]), selectedPolicyId = ref(''), selected = ref(null), detailItem = ref(null), showDetail = ref(false), loading = ref(false), action = ref(''), message = ref(''), success = ref('');
const currentPolicy = computed(() => policies.value.find(p => p.id === selectedPolicyId.value));
const canDecide = computed(() => selected.value && ['AUTOMATICA', 'REQUIERE_RECALCULO', 'EVALUACION_MANUAL', 'PENDIENTE'].includes(selected.value.estado));
const decisionLabel = (state) => ({ AUTOMATICA: 'Renovación automática', REQUIERE_RECALCULO: 'Recálculo de prima', EVALUACION_MANUAL: 'Evaluación manual', ACEPTADA: 'Propuesta aprobada', RECHAZADA: 'Propuesta rechazada', VENCIDA: 'Propuesta vencida' }[state] || state);
async function loadPolicies() { const data = (await api.get('/polizas')).data; policies.value = data.filter(p => p.estado === 'VIGENTE'); if (!selectedPolicyId.value && policies.value.length)
    selectedPolicyId.value = policies.value[0].id; await loadHistory(); }
async function loadHistory() { selected.value = null; message.value = ''; try {
    history.value = selectedPolicyId.value ? (await api.get(`/renovaciones/poliza/${selectedPolicyId.value}/historial`)).data : [];
    if (history.value.length)
        selected.value = history.value[0];
}
catch (e) {
    history.value = [];
    message.value = errorMessage(e);
} }
async function run(label, operation) { message.value = ''; success.value = ''; action.value = label; try {
    await operation();
}
catch (e) {
    message.value = errorMessage(e);
}
finally {
    action.value = '';
} }
async function evaluate() { if (!selectedPolicyId.value)
    return; loading.value = true; await run('evaluar', async () => { await api.post(`/renovaciones/poliza/${selectedPolicyId.value}/evaluar`); success.value = 'Evaluación generada correctamente.'; await loadHistory(); }); loading.value = false; }
async function decide(decision) { if (!selected.value)
    return; await run(decision, async () => { selected.value = (await api.patch(`/renovaciones/${selected.value.id}/${decision}`)).data; success.value = decision === 'aprobar' ? 'Propuesta aprobada.' : 'Propuesta rechazada.'; await loadHistory(); }); }
async function generatePolicy() { if (!selected.value)
    return; await run('generar', async () => { const policy = (await api.post(`/renovaciones/${selected.value.id}/generar-poliza`)).data; success.value = `Póliza ${policy.numero} generada correctamente.`; await loadPolicies(); }); }
function show(item) { detailItem.value = item; showDetail.value = true; }
onMounted(async () => { loading.value = true; try {
    await loadPolicies();
}
catch (e) {
    message.value = errorMessage(e);
}
finally {
    loading.value = false;
} });
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "eyebrow" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "panel renewal-workflow" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "renew-selector" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    ...{ onChange: (__VLS_ctx.loadHistory) },
    value: (__VLS_ctx.selectedPolicyId),
    disabled: (__VLS_ctx.loading),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "",
});
for (const [p] of __VLS_getVForSourceType((__VLS_ctx.policies))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        key: (p.id),
        value: (p.id),
    });
    (p.numero);
    (p.moneda);
    (Number(p.prima).toFixed(2));
    (p.fin);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.evaluate) },
    ...{ class: "primary" },
    disabled: (!__VLS_ctx.selectedPolicyId || __VLS_ctx.loading || !!__VLS_ctx.action),
});
(__VLS_ctx.action === 'evaluar' ? 'Evaluando…' : 'Evaluar renovación');
if (__VLS_ctx.currentPolicy) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "policy-summary" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.currentPolicy.numero);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.currentPolicy.moneda);
    (Number(__VLS_ctx.currentPolicy.prima).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.currentPolicy.inicio);
    (__VLS_ctx.currentPolicy.fin);
}
if (__VLS_ctx.message) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error" },
    });
    (__VLS_ctx.message);
}
if (__VLS_ctx.success) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert success" },
    });
    (__VLS_ctx.success);
}
if (!__VLS_ctx.policies.length && !__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty" },
    });
}
if (__VLS_ctx.selected) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "panel renewal-detail" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "quote-top" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "eyebrow" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (__VLS_ctx.decisionLabel(__VLS_ctx.selected.estado));
    /** @type {[typeof StatusBadge, ]} */ ;
    // @ts-ignore
    const __VLS_0 = __VLS_asFunctionalComponent(StatusBadge, new StatusBadge({
        value: (__VLS_ctx.selected.estado),
    }));
    const __VLS_1 = __VLS_0({
        value: (__VLS_ctx.selected.estado),
    }, ...__VLS_functionalComponentArgsRest(__VLS_0));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "renew-metrics" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (Number(__VLS_ctx.selected.primaAnterior).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (Number(__VLS_ctx.selected.nuevaPrima).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({
        ...{ class: ({ up: __VLS_ctx.selected.porcentajeVariacion > 0 }) },
    });
    (__VLS_ctx.selected.porcentajeVariacion > 0 ? '+' : '');
    (Number(__VLS_ctx.selected.porcentajeVariacion).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.selected.siniestrosConsiderados);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "decision-reason" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.selected.motivo);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "renew-actions" },
    });
    if (__VLS_ctx.canDecide) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.selected))
                        return;
                    if (!(__VLS_ctx.canDecide))
                        return;
                    __VLS_ctx.decide('aprobar');
                } },
            ...{ class: "primary" },
            disabled: (!!__VLS_ctx.action),
        });
        (__VLS_ctx.action === 'aprobar' ? 'Aprobando…' : 'Aprobar propuesta');
    }
    if (__VLS_ctx.canDecide) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.selected))
                        return;
                    if (!(__VLS_ctx.canDecide))
                        return;
                    __VLS_ctx.decide('rechazar');
                } },
            ...{ class: "secondary danger" },
            disabled: (!!__VLS_ctx.action),
        });
        (__VLS_ctx.action === 'rechazar' ? 'Rechazando…' : 'Rechazar propuesta');
    }
    if (__VLS_ctx.selected.estado === 'ACEPTADA' && !__VLS_ctx.selected.polizaRenovadaId) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.generatePolicy) },
            ...{ class: "primary" },
            disabled: (!!__VLS_ctx.action),
        });
        (__VLS_ctx.action === 'generar' ? 'Generando…' : 'Generar nueva póliza');
    }
    if (__VLS_ctx.selected.polizaRenovadaId) {
        const __VLS_3 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_4 = __VLS_asFunctionalComponent(__VLS_3, new __VLS_3({
            ...{ class: "secondary" },
            to: "/polizas",
        }));
        const __VLS_5 = __VLS_4({
            ...{ class: "secondary" },
            to: "/polizas",
        }, ...__VLS_functionalComponentArgsRest(__VLS_4));
        __VLS_6.slots.default;
        var __VLS_6;
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "panel table-panel renewal-history" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "history-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
(__VLS_ctx.history.length);
__VLS_asFunctionalElement(__VLS_intrinsicElements.table, __VLS_intrinsicElements.table)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.thead, __VLS_intrinsicElements.thead)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tbody, __VLS_intrinsicElements.tbody)({});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.history))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
        key: (item.id),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (new Date(item.creadaEn).toLocaleString());
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (__VLS_ctx.decisionLabel(item.estado));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (Number(item.nuevaPrima).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (Number(item.primaAnterior).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (Number(item.porcentajeVariacion).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (item.siniestrosConsiderados);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    /** @type {[typeof StatusBadge, ]} */ ;
    // @ts-ignore
    const __VLS_7 = __VLS_asFunctionalComponent(StatusBadge, new StatusBadge({
        value: (item.estado),
    }));
    const __VLS_8 = __VLS_7({
        value: (item.estado),
    }, ...__VLS_functionalComponentArgsRest(__VLS_7));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.show(item);
            } },
        ...{ class: "secondary" },
    });
}
if (!__VLS_ctx.history.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        colspan: "7",
        ...{ class: "empty" },
    });
}
/** @type {[typeof UiModal, typeof UiModal, ]} */ ;
// @ts-ignore
const __VLS_10 = __VLS_asFunctionalComponent(UiModal, new UiModal({
    ...{ 'onClose': {} },
    title: "Detalle de evaluación",
    show: (__VLS_ctx.showDetail),
}));
const __VLS_11 = __VLS_10({
    ...{ 'onClose': {} },
    title: "Detalle de evaluación",
    show: (__VLS_ctx.showDetail),
}, ...__VLS_functionalComponentArgsRest(__VLS_10));
let __VLS_13;
let __VLS_14;
let __VLS_15;
const __VLS_16 = {
    onClose: (...[$event]) => {
        __VLS_ctx.showDetail = false;
    }
};
__VLS_12.slots.default;
if (__VLS_ctx.detailItem) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "renewal-modal-detail" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "quote-top" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (new Date(__VLS_ctx.detailItem.creadaEn).toLocaleString());
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (__VLS_ctx.decisionLabel(__VLS_ctx.detailItem.estado));
    /** @type {[typeof StatusBadge, ]} */ ;
    // @ts-ignore
    const __VLS_17 = __VLS_asFunctionalComponent(StatusBadge, new StatusBadge({
        value: (__VLS_ctx.detailItem.estado),
    }));
    const __VLS_18 = __VLS_17({
        value: (__VLS_ctx.detailItem.estado),
    }, ...__VLS_functionalComponentArgsRest(__VLS_17));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "renew-metrics" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (Number(__VLS_ctx.detailItem.primaAnterior).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (Number(__VLS_ctx.detailItem.nuevaPrima).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({
        ...{ class: ({ up: __VLS_ctx.detailItem.porcentajeVariacion > 0 }) },
    });
    (__VLS_ctx.detailItem.porcentajeVariacion > 0 ? '+' : '');
    (Number(__VLS_ctx.detailItem.porcentajeVariacion).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.detailItem.siniestrosConsiderados);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "decision-reason" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.detailItem.motivo);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "renewal-trace" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.detailItem.id);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.detailItem.polizaOrigenId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (new Date(__VLS_ctx.detailItem.venceEn).toLocaleString());
    if (__VLS_ctx.detailItem.decididaEn) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
        (new Date(__VLS_ctx.detailItem.decididaEn).toLocaleString());
    }
    if (__VLS_ctx.detailItem.polizaRenovadaId) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
        (__VLS_ctx.detailItem.polizaRenovadaId);
    }
}
var __VLS_12;
/** @type {__VLS_StyleScopedClasses['page-head']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['renewal-workflow']} */ ;
/** @type {__VLS_StyleScopedClasses['renew-selector']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['policy-summary']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['success']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['renewal-detail']} */ ;
/** @type {__VLS_StyleScopedClasses['quote-top']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['renew-metrics']} */ ;
/** @type {__VLS_StyleScopedClasses['up']} */ ;
/** @type {__VLS_StyleScopedClasses['decision-reason']} */ ;
/** @type {__VLS_StyleScopedClasses['renew-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['renewal-history']} */ ;
/** @type {__VLS_StyleScopedClasses['history-title']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['renewal-modal-detail']} */ ;
/** @type {__VLS_StyleScopedClasses['quote-top']} */ ;
/** @type {__VLS_StyleScopedClasses['renew-metrics']} */ ;
/** @type {__VLS_StyleScopedClasses['up']} */ ;
/** @type {__VLS_StyleScopedClasses['decision-reason']} */ ;
/** @type {__VLS_StyleScopedClasses['renewal-trace']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            StatusBadge: StatusBadge,
            UiModal: UiModal,
            policies: policies,
            history: history,
            selectedPolicyId: selectedPolicyId,
            selected: selected,
            detailItem: detailItem,
            showDetail: showDetail,
            loading: loading,
            action: action,
            message: message,
            success: success,
            currentPolicy: currentPolicy,
            canDecide: canDecide,
            decisionLabel: decisionLabel,
            loadHistory: loadHistory,
            evaluate: evaluate,
            decide: decide,
            generatePolicy: generatePolicy,
            show: show,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
