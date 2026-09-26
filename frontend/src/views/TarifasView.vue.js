import { onMounted, reactive, ref } from 'vue';
import api, { errorMessage } from '@/services/api';
import UiModal from '@/components/UiModal.vue';
import StatusBadge from '@/components/StatusBadge.vue';
const list = ref([]), show = ref(false), showDetail = ref(false), detail = ref(null), loadingDetail = ref(false), msg = ref(''), formError = ref(''), saving = ref(false);
const f = reactive({ codigo: 'AUT-PAR', version: 1, tipoVehiculo: 'AUTO', tipoUso: 'PARTICULAR', primaBase: 1200, primaMinima: 700, inicioVigencia: new Date().toISOString().slice(0, 10), finVigencia: `${new Date().getFullYear() + 1}-12-31`, codigoNotaTecnica: 'NT-VEH-001', estado: 'VIGENTE', factores: [{ codigo: 'EDAD', nombre: 'Edad del conductor', tipoVariable: 'EDAD', valorMinimo: 18, valorMaximo: 80, multiplicador: 1.05, orden: 1 }] });
async function load() { msg.value = ''; try {
    list.value = (await api.get('/tablas-tarifarias')).data;
}
catch (e) {
    msg.value = errorMessage(e);
} }
async function save() { formError.value = ''; saving.value = true; try {
    await api.post('/tablas-tarifarias', f);
    show.value = false;
    await load();
}
catch (e) {
    formError.value = errorMessage(e);
}
finally {
    saving.value = false;
} }
function openCreate() { formError.value = ''; show.value = true; }
function closeCreate() { formError.value = ''; show.value = false; }
function add() { f.factores.push({ codigo: '', nombre: '', tipoVariable: '', valorMinimo: 0, valorMaximo: 100, multiplicador: 1, orden: f.factores.length + 1 }); }
async function openDetail(id) { loadingDetail.value = true; msg.value = ''; try {
    detail.value = (await api.get(`/tablas-tarifarias/${id}`)).data;
    showDetail.value = true;
}
catch (e) {
    msg.value = errorMessage(e);
}
finally {
    loadingDetail.value = false;
} }
onMounted(load);
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.openCreate) },
    ...{ class: "primary" },
});
if (__VLS_ctx.msg) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error" },
    });
    (__VLS_ctx.msg);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "cards" },
});
for (const [x] of __VLS_getVForSourceType((__VLS_ctx.list))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "tariff-card" },
        key: (x.id),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (x.codigo);
    (x.version);
    /** @type {[typeof StatusBadge, ]} */ ;
    // @ts-ignore
    const __VLS_0 = __VLS_asFunctionalComponent(StatusBadge, new StatusBadge({
        value: (x.estado),
    }));
    const __VLS_1 = __VLS_0({
        value: (x.estado),
    }, ...__VLS_functionalComponentArgsRest(__VLS_0));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (x.tipoVehiculo);
    (x.tipoUso);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (Number(x.primaBase).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (Number(x.primaMinima).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.hr)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (x.inicio);
    (x.fin);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (x.notaTecnica);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.openDetail(x.id);
            } },
        ...{ class: "secondary wide tariff-detail-btn" },
        disabled: (__VLS_ctx.loadingDetail),
    });
}
if (!__VLS_ctx.list.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "panel empty" },
    });
}
/** @type {[typeof UiModal, typeof UiModal, ]} */ ;
// @ts-ignore
const __VLS_3 = __VLS_asFunctionalComponent(UiModal, new UiModal({
    ...{ 'onClose': {} },
    title: "Registrar tabla tarifaria",
    show: (__VLS_ctx.show),
}));
const __VLS_4 = __VLS_3({
    ...{ 'onClose': {} },
    title: "Registrar tabla tarifaria",
    show: (__VLS_ctx.show),
}, ...__VLS_functionalComponentArgsRest(__VLS_3));
let __VLS_6;
let __VLS_7;
let __VLS_8;
const __VLS_9 = {
    onClose: (__VLS_ctx.closeCreate)
};
__VLS_5.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.save) },
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({});
(__VLS_ctx.f.codigo);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
});
(__VLS_ctx.f.version);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.f.tipoVehiculo),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.f.tipoUso),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    step: ".01",
});
(__VLS_ctx.f.primaBase);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    step: ".01",
});
(__VLS_ctx.f.primaMinima);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "date",
});
(__VLS_ctx.f.inicioVigencia);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "date",
});
(__VLS_ctx.f.finVigencia);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({});
(__VLS_ctx.f.codigoNotaTecnica);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.f.estado),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "full factor-box" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "factor-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.add) },
    type: "button",
    ...{ class: "secondary" },
});
for (const [x, i] of __VLS_getVForSourceType((__VLS_ctx.f.factores))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "factor-row" },
        key: (i),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        placeholder: "Código",
    });
    (x.codigo);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        placeholder: "Nombre",
    });
    (x.nombre);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        placeholder: "Variable",
    });
    (x.tipoVariable);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        type: "number",
        step: ".01",
    });
    (x.multiplicador);
}
if (__VLS_ctx.formError) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error full" },
        role: "alert",
    });
    (__VLS_ctx.formError);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "primary full" },
    disabled: (__VLS_ctx.saving),
});
(__VLS_ctx.saving ? 'Guardando…' : 'Guardar tabla');
var __VLS_5;
/** @type {[typeof UiModal, typeof UiModal, ]} */ ;
// @ts-ignore
const __VLS_10 = __VLS_asFunctionalComponent(UiModal, new UiModal({
    ...{ 'onClose': {} },
    title: (`Detalle · ${__VLS_ctx.detail?.codigo || ''} v${__VLS_ctx.detail?.version || ''}`),
    show: (__VLS_ctx.showDetail),
}));
const __VLS_11 = __VLS_10({
    ...{ 'onClose': {} },
    title: (`Detalle · ${__VLS_ctx.detail?.codigo || ''} v${__VLS_ctx.detail?.version || ''}`),
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
if (__VLS_ctx.detail) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "tariff-detail" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "renew-metrics" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (Number(__VLS_ctx.detail.primaBase).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (Number(__VLS_ctx.detail.primaMinima).toFixed(2));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.detail.tipoVehiculo);
    (__VLS_ctx.detail.tipoUso);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    /** @type {[typeof StatusBadge, ]} */ ;
    // @ts-ignore
    const __VLS_17 = __VLS_asFunctionalComponent(StatusBadge, new StatusBadge({
        value: (__VLS_ctx.detail.estado),
    }));
    const __VLS_18 = __VLS_17({
        value: (__VLS_ctx.detail.estado),
    }, ...__VLS_functionalComponentArgsRest(__VLS_17));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.detail.inicio);
    (__VLS_ctx.detail.fin);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.b, __VLS_intrinsicElements.b)({});
    (__VLS_ctx.detail.notaTecnica);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "panel table-panel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.table, __VLS_intrinsicElements.table)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.thead, __VLS_intrinsicElements.thead)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.th, __VLS_intrinsicElements.th)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tbody, __VLS_intrinsicElements.tbody)({});
    for (const [x] of __VLS_getVForSourceType((__VLS_ctx.detail.factores))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
            key: (x.codigo),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
        (x.orden);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
        (x.codigo);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (x.nombre);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
        (x.tipoVariable);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
        (x.valorMinimo);
        (x.valorMaximo);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
        (Number(x.multiplicador).toFixed(4));
    }
    if (!__VLS_ctx.detail.factores.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
            colspan: "5",
            ...{ class: "empty" },
        });
    }
}
var __VLS_12;
/** @type {__VLS_StyleScopedClasses['page-head']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['cards']} */ ;
/** @type {__VLS_StyleScopedClasses['tariff-card']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['wide']} */ ;
/** @type {__VLS_StyleScopedClasses['tariff-detail-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
/** @type {__VLS_StyleScopedClasses['factor-box']} */ ;
/** @type {__VLS_StyleScopedClasses['factor-head']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['factor-row']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
/** @type {__VLS_StyleScopedClasses['tariff-detail']} */ ;
/** @type {__VLS_StyleScopedClasses['renew-metrics']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            UiModal: UiModal,
            StatusBadge: StatusBadge,
            list: list,
            show: show,
            showDetail: showDetail,
            detail: detail,
            loadingDetail: loadingDetail,
            msg: msg,
            formError: formError,
            saving: saving,
            f: f,
            save: save,
            openCreate: openCreate,
            closeCreate: closeCreate,
            add: add,
            openDetail: openDetail,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
