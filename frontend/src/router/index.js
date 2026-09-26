import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
function landingFor(a) { return a.role === 'CLIENTE' ? '/mi-cuenta' : '/dashboard'; }
const routes = [{ path: '/login', component: () => import('@/views/LoginView.vue'), meta: { public: true } }, { path: '/mfa-verification', component: () => import('@/views/MfaVerificationView.vue'), meta: { public: true, mfa: true } }, { path: '/', component: () => import('@/layouts/AppLayout.vue'), children: [{ path: '', redirect: () => landingFor(useAuthStore()) }, { path: 'seguridad', component: () => import('@/views/MfaSetupView.vue') }, { path: 'mi-cuenta', component: () => import('@/views/MiCuentaView.vue'), meta: { roles: ['CLIENTE'] } }, { path: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { roles: ['ADMIN', 'ACTUARIO', 'AGENTE'] } }, { path: 'clientes', component: () => import('@/views/ClientesView.vue'), meta: { roles: ['ADMIN', 'AGENTE'] } }, { path: 'tarifas', component: () => import('@/views/TarifasView.vue'), meta: { roles: ['ADMIN', 'ACTUARIO'] } }, { path: 'cotizaciones', component: () => import('@/views/CotizacionesView.vue'), meta: { roles: ['ADMIN', 'AGENTE'] } }, { path: 'polizas', component: () => import('@/views/PolizasView.vue'), meta: { roles: ['ADMIN', 'AGENTE'] } }, { path: 'renovaciones', component: () => import('@/views/RenovacionesView.vue'), meta: { roles: ['ADMIN', 'AGENTE'] } }] }];
const router = createRouter({ history: createWebHistory(), routes });
router.beforeEach(to => { const a = useAuthStore(); if (to.meta.mfa && !a.mfaChallenge)
    return '/login'; if (!to.meta.public && !a.isAuthenticated)
    return '/login'; if (to.path === '/login' && a.isAuthenticated)
    return landingFor(a); const roles = to.meta.roles; if (roles && a.role && !roles.includes(a.role))
    return landingFor(a); });
export default router;
