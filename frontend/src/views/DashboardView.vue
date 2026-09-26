<script setup lang="ts">
import { onMounted, ref } from 'vue';
import api, { errorMessage } from '@/services/api';

const clientes = ref(0);
const polizas = ref(0);
const renovaciones = ref(0);
const loading = ref(true);
const error = ref('');

onMounted(async () => {
  try {
    const resultados = await Promise.allSettled([
      api.get('/clientes'),
      api.get('/polizas'),
      api.get('/renovaciones')
    ]);

    const [clientesResult, polizasResult, renovacionesResult] = resultados;

    if (clientesResult.status === 'fulfilled') {
      clientes.value = clientesResult.value.data.length;
    }

    if (polizasResult.status === 'fulfilled') {
      polizas.value = polizasResult.value.data.length;
    }

    if (renovacionesResult.status === 'fulfilled') {
      renovaciones.value = renovacionesResult.value.data.length;
    }

    const resultadoFallido = resultados.find(
      resultado => resultado.status === 'rejected'
    );

    if (resultadoFallido?.status === 'rejected') {
      error.value = errorMessage(resultadoFallido.reason);
    }
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div>
    <div class="hero">
      <div>
        <span class="eyebrow">Panel operativo</span>
        <h2>Control integral del negocio asegurador</h2>
        <p>
          Monitorea clientes, primas, pólizas y renovaciones desde un único lugar.
        </p>
      </div>

      <RouterLink class="primary" to="/cotizaciones">
        Nueva cotización
      </RouterLink>
    </div>

    <div v-if="error" class="alert error" role="alert">
      No se pudieron cargar todos los indicadores. {{ error }}
    </div>

    <div class="stats">
      <div class="stat">
        <span>Clientes registrados</span>
        <b>{{ loading ? '—' : clientes }}</b>
        <small>Base comercial activa</small>
      </div>

      <div class="stat">
        <span>Pólizas emitidas</span>
        <b>{{ loading ? '—' : polizas }}</b>
        <small>Cartera total</small>
      </div>

      <div class="stat">
        <span>Renovaciones</span>
        <b>{{ loading ? '—' : renovaciones }}</b>
        <small>Propuestas generadas</small>
      </div>

      <div class="stat accent">
        <span>Motor de riesgo</span>
        <b>Activo</b>
        <small>Reglas centralizadas</small>
      </div>
    </div>

    <div class="grid-2">
      <div class="panel">
        <h3>Flujo recomendado</h3>
        <div class="steps">
          <div>
            <b>1</b>
            <span>
              <strong>Registra al cliente</strong>
              <small>Datos personales y contacto</small>
            </span>
          </div>
          <div>
            <b>2</b>
            <span>
              <strong>Agrega el vehículo</strong>
              <small>Perfil y zona de circulación</small>
            </span>
          </div>
          <div>
            <b>3</b>
            <span>
              <strong>Calcula la prima</strong>
              <small>Factores y tabla vigente</small>
            </span>
          </div>
          <div>
            <b>4</b>
            <span>
              <strong>Emite y renueva</strong>
              <small>Ciclo completo de la póliza</small>
            </span>
          </div>
        </div>
      </div>

      <div class="panel dark-panel">
        <span class="eyebrow">Arquitectura Clean</span>
        <h3>El dominio mantiene el control</h3>
        <p>
          La interfaz consume casos de uso sin conocer repositorios ni detalles
          de persistencia.
        </p>
        <div class="rings">
          <i>Dominio</i>
          <i>Aplicación</i>
          <i>Infraestructura</i>
        </div>
      </div>
    </div>
  </div>
</template>
