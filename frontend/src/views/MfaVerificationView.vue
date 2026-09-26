<script setup lang="ts">
import {ref} from 'vue';import {useRouter} from 'vue-router';import {useAuthStore} from '@/stores/auth';import {errorMessage} from '@/services/api';
const codigo=ref(''),loading=ref(false),error=ref(''),a=useAuthStore(),r=useRouter();
async function verificar(){loading.value=true;error.value='';try{await a.verifyMfa(codigo.value);r.push(a.role==='CLIENTE'?'/mi-cuenta':'/dashboard')}catch(e){error.value=errorMessage(e)}finally{loading.value=false}}
function cancelar(){a.logout();r.push('/login')}
</script>
<template><div class="login-page"><div class="login-panel"><form class="login-card" @submit.prevent="verificar"><div class="logo-large">A</div><h2>Verificación en dos pasos</h2><p>Ingresa el código de 6 dígitos de Google Authenticator.</p><label>Código<input v-model="codigo" inputmode="numeric" autocomplete="one-time-code" maxlength="6" pattern="[0-9]{6}" required autofocus/></label><div v-if="error" class="alert error">{{error}}</div><button class="primary wide" :disabled="loading">{{loading?'Verificando…':'Verificar'}}</button><button type="button" class="link-btn" @click="cancelar">Cancelar</button></form></div></div></template>
