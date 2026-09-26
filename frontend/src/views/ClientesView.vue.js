import { onMounted, reactive, ref } from 'vue';
import api, { errorMessage } from '@/services/api';
import UiModal from '@/components/UiModal.vue';
const clientes = ref([]);
const vehiculos = ref({});
const mostrarCliente = ref(false);
const mostrarVehiculos = ref(false);
const clienteSeleccionado = ref(null);
const guardandoCliente = ref(false);
const guardandoVehiculo = ref(false);
const errorPagina = ref('');
const errorCliente = ref('');
const errorVehiculo = ref('');
const exitoVehiculo = ref('');
const consultandoPlaca = ref(false);
const mensajeConsultaPlaca = ref('');
const formularioCliente = reactive({
    tipoDocumento: 'DNI',
    numeroDocumento: '',
    nombres: '',
    apellidos: '',
    fechaNacimiento: '1990-01-01',
    correo: '',
    telefono: ''
});
const formularioVehiculo = reactive({
    clienteId: '',
    placa: '',
    marca: '',
    modelo: '',
    anioFabricacion: new Date().getFullYear(),
    tipo: 'AUTO',
    uso: 'PARTICULAR',
    zonaCirculacion: 'LIMA'
});
async function cargarClientes() {
    errorPagina.value = '';
    try {
        clientes.value = (await api.get('/clientes')).data;
    }
    catch (error) {
        errorPagina.value = errorMessage(error);
    }
}
function abrirNuevoCliente() {
    errorCliente.value = '';
    mostrarCliente.value = true;
}
function cerrarNuevoCliente() {
    errorCliente.value = '';
    mostrarCliente.value = false;
}
async function guardarCliente() {
    errorCliente.value = '';
    guardandoCliente.value = true;
    try {
        await api.post('/clientes', formularioCliente);
        cerrarNuevoCliente();
        await cargarClientes();
    }
    catch (error) {
        errorCliente.value = errorMessage(error);
    }
    finally {
        guardandoCliente.value = false;
    }
}
async function abrirVehiculos(cliente) {
    clienteSeleccionado.value = cliente;
    formularioVehiculo.clienteId = cliente.id;
    errorVehiculo.value = '';
    exitoVehiculo.value = '';
    mostrarVehiculos.value = true;
    try {
        vehiculos.value[cliente.id] = (await api.get(`/clientes/${cliente.id}/vehiculos`)).data;
    }
    catch (error) {
        errorVehiculo.value = errorMessage(error);
    }
}
function cerrarVehiculos() {
    errorVehiculo.value = '';
    exitoVehiculo.value = '';
    mostrarVehiculos.value = false;
}
async function guardarVehiculo() {
    errorVehiculo.value = '';
    exitoVehiculo.value = '';
    guardandoVehiculo.value = true;
    try {
        await api.post(`/clientes/${formularioVehiculo.clienteId}/vehiculos`, formularioVehiculo);
        vehiculos.value[formularioVehiculo.clienteId] = (await api.get(`/clientes/${formularioVehiculo.clienteId}/vehiculos`)).data;
        formularioVehiculo.placa = '';
        formularioVehiculo.marca = '';
        formularioVehiculo.modelo = '';
        exitoVehiculo.value = 'Vehículo registrado correctamente.';
    }
    catch (error) {
        errorVehiculo.value = errorMessage(error);
    }
    finally {
        guardandoVehiculo.value = false;
    }
}
async function consultarPlaca() {
    const placa = formularioVehiculo.placa.trim();
    if (!placa) {
        mensajeConsultaPlaca.value = 'Ingrese una placa.';
        return;
    }
    consultandoPlaca.value = true;
    mensajeConsultaPlaca.value = '';
    try {
        const { data } = await api.get('/vehiculos/informacion-externa', { params: { placa } });
        if (data.fuente === 'SIN_DATOS') {
            mensajeConsultaPlaca.value = data.mensaje;
            return;
        }
        formularioVehiculo.placa = data.placa || formularioVehiculo.placa;
        formularioVehiculo.marca = data.marca || formularioVehiculo.marca;
        formularioVehiculo.modelo = data.modelo || formularioVehiculo.modelo;
        formularioVehiculo.anioFabricacion =
            data.anio || formularioVehiculo.anioFabricacion;
        if (data.tipoUso && ['PARTICULAR', 'TAXI', 'CARGA'].includes(data.tipoUso)) {
            formularioVehiculo.uso = data.tipoUso;
        }
        mensajeConsultaPlaca.value =
            `Datos obtenidos desde ${data.fuente}. Verifique antes de guardar.`;
    }
    catch (error) {
        mensajeConsultaPlaca.value =
            `${errorMessage(error)} Puede continuar manualmente.`;
    }
    finally {
        consultandoPlaca.value = false;
    }
}
function manejarEnterPlaca(event) {
    if (event.key !== 'Enter')
        return;
    event.preventDefault();
    void consultarPlaca();
}
onMounted(cargarClientes);
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
    ...{ onClick: (__VLS_ctx.abrirNuevoCliente) },
    ...{ class: "primary" },
});
if (__VLS_ctx.errorPagina) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error" },
    });
    (__VLS_ctx.errorPagina);
}
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
for (const [cliente] of __VLS_getVForSourceType((__VLS_ctx.clientes))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({
        key: (cliente.id),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (cliente.nombres);
    (cliente.apellidos);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (cliente.correo);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (cliente.tipoDocumento);
    (cliente.numeroDocumento);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    (cliente.telefono || '—');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "badge vigente" },
    });
    (cliente.activo ? 'ACTIVO' : 'INACTIVO');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.abrirVehiculos(cliente);
            } },
        ...{ class: "secondary" },
    });
}
if (!__VLS_ctx.clientes.length && !__VLS_ctx.errorPagina) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.tr, __VLS_intrinsicElements.tr)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.td, __VLS_intrinsicElements.td)({
        colspan: "5",
        ...{ class: "empty" },
    });
}
/** @type {[typeof UiModal, typeof UiModal, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(UiModal, new UiModal({
    ...{ 'onClose': {} },
    title: "Registrar cliente",
    show: (__VLS_ctx.mostrarCliente),
}));
const __VLS_1 = __VLS_0({
    ...{ 'onClose': {} },
    title: "Registrar cliente",
    show: (__VLS_ctx.mostrarCliente),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onClose: (__VLS_ctx.cerrarNuevoCliente)
};
__VLS_2.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.guardarCliente) },
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.formularioCliente.tipoDocumento),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    required: true,
});
(__VLS_ctx.formularioCliente.numeroDocumento);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    required: true,
});
(__VLS_ctx.formularioCliente.nombres);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    required: true,
});
(__VLS_ctx.formularioCliente.apellidos);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "date",
    required: true,
});
(__VLS_ctx.formularioCliente.fechaNacimiento);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "email",
});
(__VLS_ctx.formularioCliente.correo);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({});
(__VLS_ctx.formularioCliente.telefono);
if (__VLS_ctx.errorCliente) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error full" },
        role: "alert",
    });
    (__VLS_ctx.errorCliente);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "primary full" },
    disabled: (__VLS_ctx.guardandoCliente),
});
(__VLS_ctx.guardandoCliente ? 'Guardando…' : 'Guardar cliente');
var __VLS_2;
/** @type {[typeof UiModal, typeof UiModal, ]} */ ;
// @ts-ignore
const __VLS_7 = __VLS_asFunctionalComponent(UiModal, new UiModal({
    ...{ 'onClose': {} },
    title: (`Vehículos de ${__VLS_ctx.clienteSeleccionado?.nombres || ''}`),
    show: (__VLS_ctx.mostrarVehiculos),
}));
const __VLS_8 = __VLS_7({
    ...{ 'onClose': {} },
    title: (`Vehículos de ${__VLS_ctx.clienteSeleccionado?.nombres || ''}`),
    show: (__VLS_ctx.mostrarVehiculos),
}, ...__VLS_functionalComponentArgsRest(__VLS_7));
let __VLS_10;
let __VLS_11;
let __VLS_12;
const __VLS_13 = {
    onClose: (__VLS_ctx.cerrarVehiculos)
};
__VLS_9.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "mini-list" },
});
for (const [vehiculo] of __VLS_getVForSourceType((__VLS_ctx.vehiculos[__VLS_ctx.clienteSeleccionado?.id || '']))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (vehiculo.id),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (vehiculo.placa);
    (vehiculo.marca);
    (vehiculo.modelo);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.small, __VLS_intrinsicElements.small)({});
    (vehiculo.tipo);
    (vehiculo.uso);
    (vehiculo.anioFabricacion);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.guardarVehiculo) },
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onKeydown: (__VLS_ctx.manejarEnterPlaca) },
    required: true,
    autocomplete: "off",
});
(__VLS_ctx.formularioVehiculo.placa);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.consultarPlaca) },
    type: "button",
    ...{ class: "secondary" },
    disabled: (__VLS_ctx.consultandoPlaca),
});
(__VLS_ctx.consultandoPlaca ? 'Consultando…' : 'Consultar placa');
if (__VLS_ctx.mensajeConsultaPlaca) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert full" },
        role: "status",
    });
    (__VLS_ctx.mensajeConsultaPlaca);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    required: true,
});
(__VLS_ctx.formularioVehiculo.marca);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    required: true,
});
(__VLS_ctx.formularioVehiculo.modelo);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    required: true,
});
(__VLS_ctx.formularioVehiculo.anioFabricacion);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.formularioVehiculo.tipo),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.formularioVehiculo.uso),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "full" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    required: true,
});
(__VLS_ctx.formularioVehiculo.zonaCirculacion);
if (__VLS_ctx.errorVehiculo) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert error full" },
        role: "alert",
    });
    (__VLS_ctx.errorVehiculo);
}
if (__VLS_ctx.exitoVehiculo) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "alert success full" },
        role: "status",
    });
    (__VLS_ctx.exitoVehiculo);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "primary full" },
    disabled: (__VLS_ctx.guardandoVehiculo),
});
(__VLS_ctx.guardandoVehiculo ? 'Guardando…' : 'Agregar vehículo');
var __VLS_9;
/** @type {__VLS_StyleScopedClasses['page-head']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['vigente']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['full']} */ ;
/** @type {__VLS_StyleScopedClasses['mini-list']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['alert']} */ ;
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
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            UiModal: UiModal,
            clientes: clientes,
            vehiculos: vehiculos,
            mostrarCliente: mostrarCliente,
            mostrarVehiculos: mostrarVehiculos,
            clienteSeleccionado: clienteSeleccionado,
            guardandoCliente: guardandoCliente,
            guardandoVehiculo: guardandoVehiculo,
            errorPagina: errorPagina,
            errorCliente: errorCliente,
            errorVehiculo: errorVehiculo,
            exitoVehiculo: exitoVehiculo,
            consultandoPlaca: consultandoPlaca,
            mensajeConsultaPlaca: mensajeConsultaPlaca,
            formularioCliente: formularioCliente,
            formularioVehiculo: formularioVehiculo,
            abrirNuevoCliente: abrirNuevoCliente,
            cerrarNuevoCliente: cerrarNuevoCliente,
            guardarCliente: guardarCliente,
            abrirVehiculos: abrirVehiculos,
            cerrarVehiculos: cerrarVehiculos,
            guardarVehiculo: guardarVehiculo,
            consultarPlaca: consultarPlaca,
            manejarEnterPlaca: manejarEnterPlaca,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
