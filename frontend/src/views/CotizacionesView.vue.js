import { computed, onMounted, reactive, ref, watch } from 'vue';
import api, { errorMessage } from '@/services/api';
import StatusBadge from '@/components/StatusBadge.vue';
import UiModal from '@/components/UiModal.vue';
const clients = ref([]);
const vehicles = ref([]);
const allVehicles = ref([]);
const quotes = ref([]);
const result = ref(null);
const filter = ref('TODAS');
const msg = ref('');
const success = ref('');
const accepting = ref('');
const detail = ref(null);
const showDetail = ref(false);
const f = reactive({ clienteId: '', vehiculoId: '', siniestrosResponsables: 0, porcentajeGastos: 0.08, porcentajeRecargo: 0, porcentajeDescuento: 0 });
const clientMap = computed(() => new Map(clients.value.map(x => [x.id, `${x.nombres} ${x.apellidos}`])));
const vehicleMap = computed(() => new Map(allVehicles.value.map(x => [x.id, `${x.placa} · ${x.marca} ${x.modelo}`])));
async function loadCatalogs() {
    clients.value = (await api.get('/clientes')).data;
    const groups = await Promise.all(clients.value.map(async (client) => (await api.get(`/clientes/${client.id}/vehiculos`)).data));
    allVehicles.value = groups.flat();
}
async function loadQuotes() {
    const params = filter.value === 'TODAS' ? {} : { estado: filter.value };
    quotes.value = (await api.get('/cotizaciones', { params })).data;
}
onMounted(async () => {
    try {
        await Promise.all([loadCatalogs(), loadQuotes()]);
    }
    catch (error) {
        msg.value = errorMessage(error);
    }
});
watch(filter, async () => {
    try {
        await loadQuotes();
    }
    catch (error) {
        msg.value = errorMessage(error);
    }
});
watch(() => f.clienteId, async (id) => {
    f.vehiculoId = '';
    vehicles.value = id ? allVehicles.value.filter(x => x.clienteId === id) : [];
});
async function quote() {
    msg.value = '';
    success.value = '';
    try {
        result.value = (await api.post('/cotizaciones', f)).data;
        success.value = 'Cotización creada correctamente.';
        await loadQuotes();
    }
    catch (error) {
        msg.value = errorMessage(error);
    }
}
async function accept(item) {
    msg.value = '';
    success.value = '';
    accepting.value = item.id;
    try {
        const accepted = (await api.patch(`/cotizaciones/${item.id}/aceptar`)).data;
        result.value = accepted;
        success.value = 'Cotización aceptada.';
        await loadQuotes();
    }
    catch (error) {
        msg.value = errorMessage(error);
    }
    finally {
        accepting.value = '';
    }
}
async function view(item) {
    msg.value = '';
    try {
        detail.value = (await api.get(`/cotizaciones/${item.id}`)).data;
        showDetail.value = true;
    }
    catch (error) {
        msg.value = errorMessage(error);
    }
}
async function copyId(id) {
    try {
        await navigator.clipboard.writeText(id);
        success.value = 'UUID copiado.';
    }
    catch {
        msg.value = 'No se pudo copiar el UUID.';
    }
}
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
    ...{ class: "grid-2 quote-layout" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.quote) },
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "full" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.f.clienteId),
    required: true,
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "",
});
for (const [x] of __VLS_getVForSourceType((__VLS_ctx.clients))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        key: (x.id),
        value: (x.id),
    });
    (x.nombres);
    (x.apellidos);
    (x.numeroDocumento);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "full" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.f.vehiculoId),
    required: true,
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "",
});
for (const [x] of __VLS_getVForSourceType((__VLS_ctx.vehicles))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        key: (x.id),
        value: (x.id),
    });
    (x.placa);
    (x.marca);
    (x.modelo);
    (x.tipo);
    (x.uso);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    min: "0",
});
(__VLS_ctx.f.siniestrosResponsables);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    step: ".01",
    min: "0",
});
(__VLS_ctx.f.porcentajeGastos);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    step: ".01",
    min: "0",
});
(__VLS_ctx.f.porcentajeRecargo);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    step: ".01",
    min: "0",
    max: ".5",
});
(__VLS_ctx.f.porcentajeDescuento);
if (__VLS_ctx.msg) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error full" },
    });
    (__VLS_ctx.msg);
}
if (__VLS_ctx.success) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert success full" },
    });
    (__VLS_ctx.success);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "primary full" },
});
if (__VLS_ctx.result) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "panel quote-result" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "quote-top" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (__VLS_ctx.result.numero);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    /** @type {[typeof StatusBadge, ]} */ ;
    // @ts-ignore
    const __VLS_0 = __VLS_asFunctionalComponent(StatusBadge, new StatusBadge({
        value: (__VLS_ctx.result.estado),
    }));
    const __VLS_1 = __VLS_0({
        value: (__VLS_ctx.result.estado),
    }, ...__VLS_functionalComponentArgsRest(__VLS_0));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "price" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.result.moneda);
    (Number(__VLS_ctx.result.prima).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.clientMap.get(__VLS_ctx.result.clienteId) || __VLS_ctx.result.clienteId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.vehicleMap.get(__VLS_ctx.result.vehiculoId) || __VLS_ctx.result.vehiculoId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "lookup" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({});
    (__VLS_ctx.result.id);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.result))
                    return;
                __VLS_ctx.copyId(__VLS_ctx.result.id);
            } },
        ...{ class: "secondary" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "quote-meta" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (new Date(__VLS_ctx.result.creada).toLocaleString());
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (new Date(__VLS_ctx.result.expira).toLocaleDateString());
    if (__VLS_ctx.result.desglose) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.details, __VLS_intrinsicElements.details)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.summary, __VLS_intrinsicElements.summary)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.pre, __VLS_intrinsicElements.pre)({});
        (JSON.stringify(__VLS_ctx.result.desglose, null, 2));
    }
    if (__VLS_ctx.result.estado === 'VIGENTE') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.result))
                        return;
                    if (!(__VLS_ctx.result.estado === 'VIGENTE'))
                        return;
                    __VLS_ctx.accept(__VLS_ctx.result);
                } },
            ...{ class: "primary wide" },
            disabled: (__VLS_ctx.accepting === __VLS_ctx.result.id),
        });
    }
    if (__VLS_ctx.result.estado === 'ACEPTADA') {
        const __VLS_3 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_4 = __VLS_asFunctionalComponent(__VLS_3, new __VLS_3({
            ...{ class: "secondary wide center" },
            to: ({ path: '/polizas', query: { cotizacionId: __VLS_ctx.result.id } }),
        }));
        const __VLS_5 = __VLS_4({
            ...{ class: "secondary wide center" },
            to: ({ path: '/polizas', query: { cotizacionId: __VLS_ctx.result.id } }),
        }, ...__VLS_functionalComponentArgsRest(__VLS_4));
        __VLS_6.slots.default;
        var __VLS_6;
    }
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "panel empty result-empty" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "panel table-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.filter),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.tbody, __VLS_intrinsicElements.tbody)({});
for (const [q] of __VLS_getVForSourceType((__VLS_ctx.quotes))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
        key: (q.id),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (q.numero);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (q.id);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (__VLS_ctx.clientMap.get(q.clienteId) || q.clienteId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (__VLS_ctx.vehicleMap.get(q.vehiculoId) || q.vehiculoId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (q.moneda);
    (Number(q.prima).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (new Date(q.expira).toLocaleDateString());
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    /** @type {[typeof StatusBadge, ]} */ ;
    // @ts-ignore
    const __VLS_7 = __VLS_asFunctionalComponent(StatusBadge, new StatusBadge({
        value: (q.estado),
    }));
    const __VLS_8 = __VLS_7({
        value: (q.estado),
    }, ...__VLS_functionalComponentArgsRest(__VLS_7));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        ...{ class: "actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.view(q);
            } },
        ...{ class: "secondary" },
    });
    if (q.estado === 'VIGENTE') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(q.estado === 'VIGENTE'))
                        return;
                    __VLS_ctx.accept(q);
                } },
            ...{ class: "secondary" },
            disabled: (__VLS_ctx.accepting === q.id),
        });
    }
    if (q.estado === 'ACEPTADA') {
        const __VLS_10 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
            ...{ class: "secondary" },
            to: ({ path: '/polizas', query: { cotizacionId: q.id } }),
        }));
        const __VLS_12 = __VLS_11({
            ...{ class: "secondary" },
            to: ({ path: '/polizas', query: { cotizacionId: q.id } }),
        }, ...__VLS_functionalComponentArgsRest(__VLS_11));
        __VLS_13.slots.default;
        var __VLS_13;
    }
    if (q.estado === 'EMITIDA') {
        const __VLS_14 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_15 = __VLS_asFunctionalComponent(__VLS_14, new __VLS_14({
            ...{ class: "secondary" },
            to: "/polizas",
        }));
        const __VLS_16 = __VLS_15({
            ...{ class: "secondary" },
            to: "/polizas",
        }, ...__VLS_functionalComponentArgsRest(__VLS_15));
        __VLS_17.slots.default;
        var __VLS_17;
    }
}
if (!__VLS_ctx.quotes.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        colspan: "7",
        ...{ class: "empty" },
    });
}
/** @type {[typeof UiModal, typeof UiModal, ]} */ ;
// @ts-ignore
const __VLS_18 = __VLS_asFunctionalComponent(UiModal, new UiModal({
    ...{ 'onClose': {} },
    show: (__VLS_ctx.showDetail),
    title: (`Detalle · ${__VLS_ctx.detail?.numero || ''}`),
}));
const __VLS_19 = __VLS_18({
    ...{ 'onClose': {} },
    show: (__VLS_ctx.showDetail),
    title: (`Detalle · ${__VLS_ctx.detail?.numero || ''}`),
}, ...__VLS_functionalComponentArgsRest(__VLS_18));
let __VLS_21;
let __VLS_22;
let __VLS_23;
const __VLS_24 = {
    onClose: (...[$event]) => {
        __VLS_ctx.showDetail = false;
    }
};
__VLS_20.slots.default;
if (__VLS_ctx.detail) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "quote-result" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "quote-top" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (__VLS_ctx.detail.numero);
    /** @type {[typeof StatusBadge, ]} */ ;
    // @ts-ignore
    const __VLS_25 = __VLS_asFunctionalComponent(StatusBadge, new StatusBadge({
        value: (__VLS_ctx.detail.estado),
    }));
    const __VLS_26 = __VLS_25({
        value: (__VLS_ctx.detail.estado),
    }, ...__VLS_functionalComponentArgsRest(__VLS_25));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "price" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.detail.moneda);
    (Number(__VLS_ctx.detail.prima).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-grid" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.br)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({});
    (__VLS_ctx.detail.id);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.br)({});
    (__VLS_ctx.clientMap.get(__VLS_ctx.detail.clienteId) || __VLS_ctx.detail.clienteId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.br)({});
    (__VLS_ctx.vehicleMap.get(__VLS_ctx.detail.vehiculoId) || __VLS_ctx.detail.vehiculoId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.br)({});
    (new Date(__VLS_ctx.detail.creada).toLocaleString());
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.br)({});
    (new Date(__VLS_ctx.detail.expira).toLocaleString());
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.br)({});
    (__VLS_ctx.detail.estado);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.detail))
                    return;
                __VLS_ctx.copyId(__VLS_ctx.detail.id);
            } },
        ...{ class: "secondary" },
    });
    if (__VLS_ctx.detail.estado === 'ACEPTADA') {
        const __VLS_28 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
            ...{ class: "primary wide center" },
            to: ({ path: '/polizas', query: { cotizacionId: __VLS_ctx.detail.id } }),
        }));
        const __VLS_30 = __VLS_29({
            ...{ class: "primary wide center" },
            to: ({ path: '/polizas', query: { cotizacionId: __VLS_ctx.detail.id } }),
        }, ...__VLS_functionalComponentArgsRest(__VLS_29));
        __VLS_31.slots.default;
        var __VLS_31;
    }
}
var __VLS_20;
/** @type {__VLS_StyleScopedClasses['page-head']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['grid-2']} */ ;
/** @type {__VLS_StyleScopedClasses['quote-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['success']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['quote-result']} */ ;
/** @type {__VLS_StyleScopedClasses['quote-top']} */ ;
/** @type {__VLS_StyleScopedClasses['price']} */ ;
/** @type {__VLS_StyleScopedClasses['lookup']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['quote-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['wide']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['wide']} */ ;
/** @type {__VLS_StyleScopedClasses['center']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['result-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['page-head']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['quote-result']} */ ;
/** @type {__VLS_StyleScopedClasses['quote-top']} */ ;
/** @type {__VLS_StyleScopedClasses['price']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['wide']} */ ;
/** @type {__VLS_StyleScopedClasses['center']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            StatusBadge: StatusBadge,
            UiModal: UiModal,
            clients: clients,
            vehicles: vehicles,
            quotes: quotes,
            result: result,
            filter: filter,
            msg: msg,
            success: success,
            accepting: accepting,
            detail: detail,
            showDetail: showDetail,
            f: f,
            clientMap: clientMap,
            vehicleMap: vehicleMap,
            quote: quote,
            accept: accept,
            view: view,
            copyId: copyId,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
