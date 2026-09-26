<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import api, { errorMessage } from '@/services/api';
import type { Cliente, Vehiculo } from '@/types';
import UiModal from '@/components/UiModal.vue';

const clientes = ref<Cliente[]>([]);
const vehiculos = ref<Record<string, Vehiculo[]>>({});
const mostrarCliente = ref(false);
const mostrarVehiculos = ref(false);
const clienteSeleccionado = ref<Cliente | null>(null);
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
  } catch (error) {
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
  } catch (error) {
    errorCliente.value = errorMessage(error);
  } finally {
    guardandoCliente.value = false;
  }
}

async function abrirVehiculos(cliente: Cliente) {
  clienteSeleccionado.value = cliente;
  formularioVehiculo.clienteId = cliente.id;
  errorVehiculo.value = '';
  exitoVehiculo.value = '';
  mostrarVehiculos.value = true;

  try {
    vehiculos.value[cliente.id] = (
      await api.get(`/clientes/${cliente.id}/vehiculos`)
    ).data;
  } catch (error) {
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
    await api.post(
      `/clientes/${formularioVehiculo.clienteId}/vehiculos`,
      formularioVehiculo
    );

    vehiculos.value[formularioVehiculo.clienteId] = (
      await api.get(`/clientes/${formularioVehiculo.clienteId}/vehiculos`)
    ).data;

    formularioVehiculo.placa = '';
    formularioVehiculo.marca = '';
    formularioVehiculo.modelo = '';
    exitoVehiculo.value = 'Vehículo registrado correctamente.';
  } catch (error) {
    errorVehiculo.value = errorMessage(error);
  } finally {
    guardandoVehiculo.value = false;
  }
}

interface InformacionVehiculoExterna {
  placa: string;
  marca: string | null;
  modelo: string | null;
  anio: number | null;
  tipoUso: string | null;
  fuente: string;
  mensaje: string;
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
    const { data } = await api.get<InformacionVehiculoExterna>(
      '/vehiculos/informacion-externa',
      { params: { placa } }
    );
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
  } catch (error) {
    mensajeConsultaPlaca.value =
      `${errorMessage(error)} Puede continuar manualmente.`;
  } finally {
    consultandoPlaca.value = false;
  }
}

function manejarEnterPlaca(event: KeyboardEvent) {
  if (event.key !== 'Enter') return;

  event.preventDefault();
  void consultarPlaca();
}

onMounted(cargarClientes);
</script>

<template>
  <div class="page-head">
    <div>
      <span class="eyebrow">Administración</span>
      <h2>Clientes y vehículos</h2>
      <p>Gestiona asegurados y bienes vinculados a sus cotizaciones.</p>
    </div>

    <button class="primary" @click="abrirNuevoCliente">
      + Nuevo cliente
    </button>
  </div>

  <div v-if="errorPagina" class="alert error">
    {{ errorPagina }}
  </div>

  <div class="panel table-panel">
    <table>
      <thead>
        <tr>
          <th>Cliente</th>
          <th>Documento</th>
          <th>Contacto</th>
          <th>Estado</th>
          <th></th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="cliente in clientes" :key="cliente.id">
          <td>
            <strong>{{ cliente.nombres }} {{ cliente.apellidos }}</strong>
            <small>{{ cliente.correo }}</small>
          </td>
          <td>{{ cliente.tipoDocumento }} {{ cliente.numeroDocumento }}</td>
          <td>{{ cliente.telefono || '—' }}</td>
          <td>
            <span class="badge vigente">
              {{ cliente.activo ? 'ACTIVO' : 'INACTIVO' }}
            </span>
          </td>
          <td>
            <button class="secondary" @click="abrirVehiculos(cliente)">
              Vehículos
            </button>
          </td>
        </tr>

        <tr v-if="!clientes.length && !errorPagina">
          <td colspan="5" class="empty">No hay clientes registrados.</td>
        </tr>
      </tbody>
    </table>
  </div>

  <UiModal
    title="Registrar cliente"
    :show="mostrarCliente"
    @close="cerrarNuevoCliente"
  >
    <form class="form-grid" @submit.prevent="guardarCliente">
      <label>
        Tipo documento
        <select v-model="formularioCliente.tipoDocumento">
          <option>DNI</option>
          <option>CE</option>
          <option>PASAPORTE</option>
        </select>
      </label>

      <label>
        Número
        <input v-model="formularioCliente.numeroDocumento" required />
      </label>

      <label>
        Nombres
        <input v-model="formularioCliente.nombres" required />
      </label>

      <label>
        Apellidos
        <input v-model="formularioCliente.apellidos" required />
      </label>

      <label>
        Fecha de nacimiento
        <input
          v-model="formularioCliente.fechaNacimiento"
          type="date"
          required
        />
      </label>

      <label>
        Correo
        <input v-model="formularioCliente.correo" type="email" />
      </label>

      <label>
        Teléfono
        <input v-model="formularioCliente.telefono" />
      </label>

      <div v-if="errorCliente" class="alert error full" role="alert">
        {{ errorCliente }}
      </div>

      <button class="primary full" :disabled="guardandoCliente">
        {{ guardandoCliente ? 'Guardando…' : 'Guardar cliente' }}
      </button>
    </form>
  </UiModal>

  <UiModal
    :title="`Vehículos de ${clienteSeleccionado?.nombres || ''}`"
    :show="mostrarVehiculos"
    @close="cerrarVehiculos"
  >
    <div class="mini-list">
      <div
        v-for="vehiculo in vehiculos[clienteSeleccionado?.id || '']"
        :key="vehiculo.id"
      >
        <strong>
          {{ vehiculo.placa }} · {{ vehiculo.marca }} {{ vehiculo.modelo }}
        </strong>
        <small>
          {{ vehiculo.tipo }} / {{ vehiculo.uso }} ·
          {{ vehiculo.anioFabricacion }}
        </small>
      </div>
    </div>

    <h3>Agregar vehículo</h3>

    <form class="form-grid" @submit.prevent="guardarVehiculo">
      <label>
        Placa
        <input
          v-model="formularioVehiculo.placa"
          required
          autocomplete="off"
          @keydown="manejarEnterPlaca"
        />
      </label>

      <button
        type="button"
        class="secondary"
        :disabled="consultandoPlaca"
        @click="consultarPlaca"
      >
        {{ consultandoPlaca ? 'Consultando…' : 'Consultar placa' }}
      </button>

      <div
        v-if="mensajeConsultaPlaca"
        class="alert full"
        role="status"
      >
        {{ mensajeConsultaPlaca }}
      </div>

      <label>
        Marca
        <input v-model="formularioVehiculo.marca" required />
      </label>

      <label>
        Modelo
        <input v-model="formularioVehiculo.modelo" required />
      </label>

      <label>
        Año
        <input
          v-model.number="formularioVehiculo.anioFabricacion"
          type="number"
          required
        />
      </label>

      <label>
        Tipo
        <select v-model="formularioVehiculo.tipo">
          <option>AUTO</option>
          <option>CAMIONETA</option>
          <option>MOTO</option>
        </select>
      </label>

      <label>
        Uso
        <select v-model="formularioVehiculo.uso">
          <option>PARTICULAR</option>
          <option>TAXI</option>
          <option>CARGA</option>
        </select>
      </label>

      <label class="full">
        Zona
        <input v-model="formularioVehiculo.zonaCirculacion" required />
      </label>

      <div v-if="errorVehiculo" class="alert error full" role="alert">
        {{ errorVehiculo }}
      </div>

      <div v-if="exitoVehiculo" class="alert success full" role="status">
        {{ exitoVehiculo }}
      </div>

      <button class="primary full" :disabled="guardandoVehiculo">
        {{ guardandoVehiculo ? 'Guardando…' : 'Agregar vehículo' }}
      </button>
    </form>
  </UiModal>
</template>
