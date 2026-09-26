<script setup lang="ts">
import { onMounted, ref } from 'vue';
import api, { errorMessage } from '@/services/api';
import type { MiCuenta } from '@/types';
import StatusBadge from '@/components/StatusBadge.vue';

const data = ref<MiCuenta | null>(null);
const loading = ref(true);
const error = ref('');

onMounted(async () => {
  try {
    data.value = (await api.get('/mi-cuenta')).data;
  } catch (e) {
    error.value = errorMessage(e);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <span class="eyebrow">Mi cuenta</span>
        <h2>
          {{
            data?.cliente
              ? `Hola, ${data.cliente.nombres} ${data.cliente.apellidos}`
              : 'Mi cuenta'
          }}
        </h2>
        <p>Consulta el estado de tu póliza y tu renovación.</p>
      </div>
    </div>

    <div v-if="loading" class="empty">Cargando…</div>

    <div v-else-if="error" class="alert error" role="alert">{{ error }}</div>

    <template v-else>
      <div v-if="!data?.cliente" class="panel">
        <h3>Aún no tienes datos registrados</h3>
        <p>
          No encontramos una cuenta de cliente asociada a tu correo. Contacta
          a un agente de Andina Seguros para registrarte.
        </p>
      </div>

      <template v-else>
        <div class="panel">
          <h3>Mis datos</h3>
          <div class="mini-list">
            <div>
              <strong>{{ data.cliente.nombres }} {{ data.cliente.apellidos }}</strong>
              <small>{{ data.cliente.tipoDocumento }} {{ data.cliente.numeroDocumento }}</small>
            </div>
            <div>
              <strong>{{ data.cliente.correo }}</strong>
              <small>{{ data.cliente.telefono }}</small>
            </div>
          </div>
        </div>

        <div v-if="!data.polizas.length" class="panel empty">
          Todavía no tienes pólizas registradas.
        </div>

        <div v-for="p in data.polizas" :key="p.poliza.id" class="panel">
          <div class="renew-top">
            <h3>Póliza {{ p.poliza.numero }}</h3>
            <StatusBadge :value="p.poliza.estado" />
          </div>
          <p>Vigencia: {{ p.poliza.inicio }} → {{ p.poliza.fin }}</p>
          <b>{{ p.poliza.moneda }} {{ Number(p.poliza.prima).toFixed(2) }}</b>

          <template v-if="p.renovaciones.length">
            <hr />
            <h3>Renovación</h3>
            <div class="mini-list">
              <div v-for="r in p.renovaciones" :key="r.id">
                <div class="renew-top">
                  <strong>Nueva prima: {{ Number(r.nuevaPrima).toFixed(2) }}</strong>
                  <StatusBadge :value="r.estado" />
                </div>
                <small>
                  Variación {{ Number(r.porcentajeVariacion).toFixed(2) }}% · Vence
                  {{ r.venceEn }}
                </small>
              </div>
            </div>
          </template>
        </div>
      </template>
    </template>
  </div>
</template>
