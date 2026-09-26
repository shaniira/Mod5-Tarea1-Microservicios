<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import api, { errorMessage } from '@/services/api';
import type { Cliente, Vehiculo, Cotizacion, EstadoCotizacion } from '@/types';
import StatusBadge from '@/components/StatusBadge.vue';
import UiModal from '@/components/UiModal.vue';

const clients = ref<Cliente[]>([]);
const vehicles = ref<Vehiculo[]>([]);
const allVehicles = ref<Vehiculo[]>([]);
const quotes = ref<Cotizacion[]>([]);
const result = ref<Cotizacion | null>(null);
const filter = ref<'TODAS' | EstadoCotizacion>('TODAS');
const msg = ref('');
const success = ref('');
const accepting = ref('');
const detail = ref<Cotizacion | null>(null);
const showDetail = ref(false);
const f = reactive({ clienteId: '', vehiculoId: '', siniestrosResponsables: 0, porcentajeGastos: 0.08, porcentajeRecargo: 0, porcentajeDescuento: 0 });

const clientMap = computed(() => new Map(clients.value.map(x => [x.id, `${x.nombres} ${x.apellidos}`])));
const vehicleMap = computed(() => new Map(allVehicles.value.map(x => [x.id, `${x.placa} · ${x.marca} ${x.modelo}`])));

async function loadCatalogs() {
  clients.value = (await api.get('/clientes')).data;
  const groups = await Promise.all(clients.value.map(async client =>
    (await api.get(`/clientes/${client.id}/vehiculos`)).data as Vehiculo[]
  ));
  allVehicles.value = groups.flat();
}

async function loadQuotes() {
  const params = filter.value === 'TODAS' ? {} : { estado: filter.value };
  quotes.value = (await api.get('/cotizaciones', { params })).data;
}

onMounted(async () => {
  try {
    await Promise.all([loadCatalogs(), loadQuotes()]);
  } catch (error) {
    msg.value = errorMessage(error);
  }
});

watch(filter, async () => {
  try { await loadQuotes(); } catch (error) { msg.value = errorMessage(error); }
});

watch(() => f.clienteId, async id => {
  f.vehiculoId = '';
  vehicles.value = id ? allVehicles.value.filter(x => x.clienteId === id) : [];
});

async function quote() {
  msg.value = ''; success.value = '';
  try {
    result.value = (await api.post('/cotizaciones', f)).data;
    success.value = 'Cotización creada correctamente.';
    await loadQuotes();
  } catch (error) { msg.value = errorMessage(error); }
}

async function accept(item: Cotizacion) {
  msg.value = ''; success.value = ''; accepting.value = item.id;
  try {
    const accepted: Cotizacion = (await api.patch(`/cotizaciones/${item.id}/aceptar`)).data;
    result.value = accepted;
    success.value = 'Cotización aceptada.';
    await loadQuotes();
  } catch (error) { msg.value = errorMessage(error); }
  finally { accepting.value = ''; }
}

async function view(item: Cotizacion) {
  msg.value = '';
  try {
    detail.value = (await api.get(`/cotizaciones/${item.id}`)).data;
    showDetail.value = true;
  } catch (error) { msg.value = errorMessage(error); }
}
async function copyId(id: string) {
  try { await navigator.clipboard.writeText(id); success.value = 'UUID copiado.'; }
  catch { msg.value = 'No se pudo copiar el UUID.'; }
}
</script>

<template>
  <div class="page-head"><div><span class="eyebrow">Cotización</span><h2>Calcula una prima explicable</h2><p>Selecciona al cliente y vehículo; el dominio aplicará la tabla vigente.</p></div></div>
  <div class="grid-2 quote-layout">
    <div class="panel">
      <h3>Datos para tarificación</h3>
      <form class="form-grid" @submit.prevent="quote">
        <label class="full">Cliente<select v-model="f.clienteId" required><option value="">Seleccione…</option><option v-for="x in clients" :key="x.id" :value="x.id">{{x.nombres}} {{x.apellidos}} · {{x.numeroDocumento}}</option></select></label>
        <label class="full">Vehículo<select v-model="f.vehiculoId" required><option value="">Seleccione…</option><option v-for="x in vehicles" :key="x.id" :value="x.id">{{x.placa}} · {{x.marca}} {{x.modelo}} ({{x.tipo}}/{{x.uso}})</option></select></label>
        <label>Siniestros responsables<input v-model.number="f.siniestrosResponsables" type="number" min="0"/></label>
        <label>Gastos<input v-model.number="f.porcentajeGastos" type="number" step=".01" min="0"/></label>
        <label>Recargo<input v-model.number="f.porcentajeRecargo" type="number" step=".01" min="0"/></label>
        <label>Descuento<input v-model.number="f.porcentajeDescuento" type="number" step=".01" min="0" max=".5"/></label>
        <div v-if="msg" class="alert error full">{{msg}}</div><div v-if="success" class="alert success full">{{success}}</div>
        <button class="primary full">Calcular cotización</button>
      </form>
    </div>
    <div v-if="result" class="panel quote-result">
      <div class="quote-top"><div><small>{{result.numero}}</small><h3>Prima calculada</h3></div><StatusBadge :value="result.estado"/></div>
      <div class="price"><span>{{result.moneda}}</span>{{Number(result.prima).toFixed(2)}}</div>
      <p><b>Cliente:</b> {{clientMap.get(result.clienteId) || result.clienteId}}</p>
      <p><b>Vehículo:</b> {{vehicleMap.get(result.vehiculoId) || result.vehiculoId}}</p>
      <div class="lookup"><code>{{result.id}}</code><button class="secondary" @click="copyId(result.id)">Copiar UUID</button></div>
      <div class="quote-meta"><span>Creada <b>{{new Date(result.creada).toLocaleString()}}</b></span><span>Vence <b>{{new Date(result.expira).toLocaleDateString()}}</b></span></div>
      <details v-if="result.desglose"><summary>Ver desglose técnico</summary><pre>{{JSON.stringify(result.desglose,null,2)}}</pre></details>
      <button v-if="result.estado === 'VIGENTE'" class="primary wide" :disabled="accepting===result.id" @click="accept(result)">Aceptar cotización</button>
      <RouterLink v-if="result.estado === 'ACEPTADA'" class="secondary wide center" :to="{path:'/polizas',query:{cotizacionId:result.id}}">Emitir póliza</RouterLink>
    </div>
    <div v-else class="panel empty result-empty"><b>Resultado de cotización</b><span>Completa los datos para visualizar la prima y su trazabilidad.</span></div>
  </div>

  <div class="panel table-panel">
    <div class="page-head"><div><h3>Historial de cotizaciones</h3></div><select v-model="filter"><option>TODAS</option><option>VIGENTE</option><option>ACEPTADA</option><option>EMITIDA</option><option>VENCIDA</option></select></div>
    <table><thead><tr><th>Cotización / UUID</th><th>Cliente</th><th>Vehículo</th><th>Prima</th><th>Vencimiento</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody><tr v-for="q in quotes" :key="q.id"><td><strong>{{q.numero}}</strong><small>{{q.id}}</small></td><td>{{clientMap.get(q.clienteId)||q.clienteId}}</td><td>{{vehicleMap.get(q.vehiculoId)||q.vehiculoId}}</td><td>{{q.moneda}} {{Number(q.prima).toFixed(2)}}</td><td>{{new Date(q.expira).toLocaleDateString()}}</td><td><StatusBadge :value="q.estado"/></td><td class="actions"><button class="secondary" @click="view(q)">Ver</button><button v-if="q.estado==='VIGENTE'" class="secondary" :disabled="accepting===q.id" @click="accept(q)">Aceptar</button><RouterLink v-if="q.estado==='ACEPTADA'" class="secondary" :to="{path:'/polizas',query:{cotizacionId:q.id}}">Emitir póliza</RouterLink><RouterLink v-if="q.estado==='EMITIDA'" class="secondary" to="/polizas">Ver póliza</RouterLink></td></tr><tr v-if="!quotes.length"><td colspan="7" class="empty">No existen cotizaciones para este filtro.</td></tr></tbody>
    </table>
  </div>
  <UiModal :show="showDetail" :title="`Detalle · ${detail?.numero || ''}`" @close="showDetail=false">
    <div v-if="detail" class="quote-result">
      <div class="quote-top"><div><small>Cotización final</small><h3>{{detail.numero}}</h3></div><StatusBadge :value="detail.estado"/></div>
      <div class="price"><span>{{detail.moneda}}</span>{{Number(detail.prima).toFixed(2)}}</div>
      <div class="form-grid">
        <p><b>UUID</b><br/><code>{{detail.id}}</code></p>
        <p><b>Cliente</b><br/>{{clientMap.get(detail.clienteId) || detail.clienteId}}</p>
        <p><b>Vehículo</b><br/>{{vehicleMap.get(detail.vehiculoId) || detail.vehiculoId}}</p>
        <p><b>Creada</b><br/>{{new Date(detail.creada).toLocaleString()}}</p>
        <p><b>Vencimiento</b><br/>{{new Date(detail.expira).toLocaleString()}}</p>
        <p><b>Estado final</b><br/>{{detail.estado}}</p>
      </div>
      <button class="secondary" @click="copyId(detail.id)">Copiar UUID</button>
      <RouterLink v-if="detail.estado==='ACEPTADA'" class="primary wide center" :to="{path:'/polizas',query:{cotizacionId:detail.id}}">Emitir póliza</RouterLink>
    </div>
  </UiModal>
</template>
