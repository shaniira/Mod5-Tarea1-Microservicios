import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import api, { errorMessage } from '@/services/api';
import UiModal from '@/components/UiModal.vue';
import StatusBadge from '@/components/StatusBadge.vue';
const list = ref([]), pending = ref([]), clients = ref([]), vehicles = ref([]), sin = ref({}), show = ref(false), selected = ref(null), msg = ref(''), success = ref(''), updating = ref(''), renewing = ref(''), estado = ref('');
const stateEdits = reactive({});
const route = useRoute();
const emit = reactive({ cotizacionId: String(route.query.cotizacionId || ''), inicioVigencia: new Date().toISOString().slice(0, 10) });
const f = reactive({ polizaId: '', fecha: new Date().toISOString().slice(0, 10), tipo: 'COLISION', montoEstimado: 1000, responsabilidadAsegurado: false, gravedad: 'LEVE', estado: 'REPORTADO' });
const clientMap = computed(() => new Map(clients.value.map(x => [x.id, `${x.nombres} ${x.apellidos}`])));
const vehicleMap = computed(() => new Map(vehicles.value.map(x => [x.id, `${x.placa} · ${x.marca} ${x.modelo}`])));
function quoteLabel(q) { return `${q.numero} — ${clientMap.value.get(q.clienteId) || 'Cliente'} — ${vehicleMap.value.get(q.vehiculoId) || 'Vehículo'} — ${q.moneda} ${Number(q.prima).toFixed(2)}`; }
async function load() { msg.value = ''; try {
    list.value = (await api.get('/polizas', { params: { estado: estado.value || undefined } })).data;
}
catch (e) {
    msg.value = errorMessage(e);
} }
async function loadPending() { pending.value = (await api.get('/cotizaciones/pendientes-emision')).data; if (emit.cotizacionId && !pending.value.some(q => q.id === emit.cotizacionId))
    emit.cotizacionId = ''; }
async function loadCatalogs() { clients.value = (await api.get('/clientes')).data; const groups = await Promise.all(clients.value.map(async (c) => (await api.get(`/clientes/${c.id}/vehiculos`)).data)); vehicles.value = groups.flat(); }
async function issue() { msg.value = ''; success.value = ''; try {
    await api.post('/polizas', emit);
    success.value = 'Póliza emitida correctamente.';
    emit.cotizacionId = '';
    await Promise.all([load(), loadPending()]);
}
catch (e) {
    msg.value = errorMessage(e);
} }
async function loadClaims(policyId) { sin.value[policyId] = (await api.get(`/polizas/${policyId}/siniestros`)).data; sin.value[policyId].forEach(x => stateEdits[x.id] = x.estado); }
async function open(p) { selected.value = p; f.polizaId = p.id; show.value = true; msg.value = ''; try {
    await loadClaims(p.id);
}
catch (e) {
    msg.value = errorMessage(e);
} }
async function saveS() { try {
    await api.post(`/polizas/${f.polizaId}/siniestros`, f);
    await loadClaims(f.polizaId);
    msg.value = '';
}
catch (e) {
    msg.value = errorMessage(e);
} }
async function updateState(claim) { if (!selected.value)
    return; updating.value = claim.id; msg.value = ''; try {
    await api.patch(`/polizas/${selected.value.id}/siniestros/${claim.id}/estado`, { estado: stateEdits[claim.id] });
    await loadClaims(selected.value.id);
}
catch (e) {
    msg.value = errorMessage(e);
}
finally {
    updating.value = '';
} }
async function renew(p) { msg.value = ''; success.value = ''; renewing.value = p.id; try {
    await api.post(`/renovaciones/poliza/${p.id}/evaluar`);
    success.value = `Evaluación de renovación generada para la póliza ${p.numero}.`;
}
catch (e) {
    msg.value = errorMessage(e);
}
finally {
    renewing.value = '';
} }
onMounted(async () => { try {
    await Promise.all([load(), loadCatalogs(), loadPending()]);
}
catch (e) {
    msg.value = errorMessage(e);
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
    ...{ class: "panel issue-box" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.issue) },
    ...{ class: "inline-form" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.emit.cotizacionId),
    required: true,
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "",
});
for (const [q] of __VLS_getVForSourceType((__VLS_ctx.pending))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        key: (q.id),
        value: (q.id),
    });
    (__VLS_ctx.quoteLabel(q));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "date",
    required: true,
});
(__VLS_ctx.emit.inicioVigencia);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "primary" },
});
if (__VLS_ctx.emit.cotizacionId) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (__VLS_ctx.emit.cotizacionId);
}
if (!__VLS_ctx.pending.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty" },
    });
}
if (__VLS_ctx.msg && !__VLS_ctx.show) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error" },
        role: "alert",
    });
    (__VLS_ctx.msg);
}
if (__VLS_ctx.success && !__VLS_ctx.show) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert success" },
        role: "status",
    });
    (__VLS_ctx.success);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "panel table-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "inline-form" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    ...{ onChange: (__VLS_ctx.load) },
    value: (__VLS_ctx.estado),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.table, __VLS_intrinsicElements.table)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.thead, __VLS_intrinsicElements.thead)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tbody, __VLS_intrinsicElements.tbody)({});
for (const [p] of __VLS_getVForSourceType((__VLS_ctx.list))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
        key: (p.id),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (p.numero);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (p.id);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (p.moneda);
    (Number(p.prima).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (p.inicio);
    (p.fin);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    /** @type {[typeof StatusBadge, ]} */ ;
    // @ts-ignore
    const __VLS_0 = __VLS_asFunctionalComponent(StatusBadge, new StatusBadge({
        value: (p.estado),
    }));
    const __VLS_1 = __VLS_0({
        value: (p.estado),
    }, ...__VLS_functionalComponentArgsRest(__VLS_0));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.open(p);
            } },
        ...{ class: "secondary" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.renew(p);
            } },
        ...{ class: "secondary" },
        disabled: (__VLS_ctx.renewing === p.id),
    });
    (__VLS_ctx.renewing === p.id ? 'Evaluando…' : 'Evaluar renovación');
}
if (!__VLS_ctx.list.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        colspan: "5",
        ...{ class: "empty" },
    });
}
/** @type {[typeof UiModal, typeof UiModal, ]} */ ;
// @ts-ignore
const __VLS_3 = __VLS_asFunctionalComponent(UiModal, new UiModal({
    ...{ 'onClose': {} },
    title: (`Siniestros · ${__VLS_ctx.selected?.numero || ''}`),
    show: (__VLS_ctx.show),
}));
const __VLS_4 = __VLS_3({
    ...{ 'onClose': {} },
    title: (`Siniestros · ${__VLS_ctx.selected?.numero || ''}`),
    show: (__VLS_ctx.show),
}, ...__VLS_functionalComponentArgsRest(__VLS_3));
let __VLS_6;
let __VLS_7;
let __VLS_8;
const __VLS_9 = {
    onClose: (...[$event]) => {
        __VLS_ctx.show = false;
    }
};
__VLS_5.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "mini-list claim-list" },
});
for (const [x] of __VLS_getVForSourceType((__VLS_ctx.sin[__VLS_ctx.selected?.id || '']))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (x.id),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "claim-info" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (x.tipo);
    (Number(x.montoEstimado).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (x.fecha);
    (x.gravedad);
    /** @type {[typeof StatusBadge, ]} */ ;
    // @ts-ignore
    const __VLS_10 = __VLS_asFunctionalComponent(StatusBadge, new StatusBadge({
        value: (x.estado),
    }));
    const __VLS_11 = __VLS_10({
        value: (x.estado),
    }, ...__VLS_functionalComponentArgsRest(__VLS_10));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "claim-state" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        value: (__VLS_ctx.stateEdits[x.id]),
        disabled: (x.estado === 'LIQUIDADO' || x.estado === 'RECHAZADO'),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.updateState(x);
            } },
        ...{ class: "secondary" },
        disabled: (__VLS_ctx.updating === x.id || __VLS_ctx.stateEdits[x.id] === x.estado || x.estado === 'LIQUIDADO' || x.estado === 'RECHAZADO'),
    });
    (__VLS_ctx.updating === x.id ? 'Guardando…' : 'Actualizar estado');
}
if (__VLS_ctx.msg) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error" },
    });
    (__VLS_ctx.msg);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.saveS) },
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "date",
});
(__VLS_ctx.f.fecha);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({});
(__VLS_ctx.f.tipo);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
});
(__VLS_ctx.f.montoEstimado);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.f.gravedad),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.f.estado),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "check" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "checkbox",
});
(__VLS_ctx.f.responsabilidadAsegurado);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "primary full" },
});
var __VLS_5;
/** @type {__VLS_StyleScopedClasses['page-head']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['issue-box']} */ ;
/** @type {__VLS_StyleScopedClasses['inline-form']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['success']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['inline-form']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['mini-list']} */ ;
/** @type {__VLS_StyleScopedClasses['claim-list']} */ ;
/** @type {__VLS_StyleScopedClasses['claim-info']} */ ;
/** @type {__VLS_StyleScopedClasses['claim-state']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['check']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            UiModal: UiModal,
            StatusBadge: StatusBadge,
            list: list,
            pending: pending,
            sin: sin,
            show: show,
            selected: selected,
            msg: msg,
            success: success,
            updating: updating,
            renewing: renewing,
            estado: estado,
            stateEdits: stateEdits,
            emit: emit,
            f: f,
            quoteLabel: quoteLabel,
            load: load,
            issue: issue,
            open: open,
            saveS: saveS,
            updateState: updateState,
            renew: renew,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
