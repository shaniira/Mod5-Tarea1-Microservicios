// Prueba de carga (paso 7.4 de la ruta) sobre el gateway, con k6.
//
// Escenarios:
//   lecturas: 20 peticiones/s durante 60 s sobre pólizas, clientes, cotizaciones y renovaciones.
//   emision:  2 emisiones de póliza/s durante 30 s (la saga completa: policy + eventos a notification,
//             claims y quotation). Las cotizaciones aceptadas se preparan en setup().
//   pico:     120 peticiones/s durante 10 s a una sola ruta, por encima del límite del gateway
//             (50/s, ráfaga 100): el exceso debe responder 429, nunca 5xx.
//
// Uso (desde la raíz del repositorio, con el stack levantado; WhatsApp simulado para no enviar
// mensajes reales):
//   docker run --rm -i --network andina_gateway_network -e BASE=http://gateway:8080 \
//     grafana/k6:0.54.0 run - < infra/carga/carga.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import exec from 'k6/execution';

const BASE = __ENV.BASE || 'http://gateway:8080';
const EMISIONES = 70; // más que las iteraciones del escenario (2/s x 30 s): cada una emite una cotización distinta
const errores5xx = new Counter('respuestas_5xx');
const limitadas = new Counter('respuestas_429');

export const options = {
  setupTimeout: '5m',
  scenarios: {
    lecturas: { executor: 'constant-arrival-rate', rate: 20, timeUnit: '1s', duration: '60s',
                preAllocatedVUs: 20, maxVUs: 100, exec: 'lecturas' },
    emision: { executor: 'constant-arrival-rate', rate: 2, timeUnit: '1s', duration: '30s',
               preAllocatedVUs: 10, maxVUs: 40, exec: 'emision', startTime: '5s' },
    pico: { executor: 'constant-arrival-rate', rate: 120, timeUnit: '1s', duration: '10s',
            preAllocatedVUs: 60, maxVUs: 300, exec: 'pico', startTime: '70s' },
  },
  thresholds: {
    'http_req_duration{scenario:lecturas}': ['p(95)<1000'],
    'http_req_failed{scenario:lecturas}': ['rate<0.01'],
    'http_req_duration{scenario:emision}': ['p(95)<3000'],
    'checks{scenario:emision}': ['rate>0.99'],
    respuestas_5xx: ['count==0'],
  },
};

function login() {
  for (let i = 0; i < 10; i++) {
    const r = http.post(`${BASE}/api/auth/login`, JSON.stringify({ username: 'admin2', password: 'admin2' }),
      { headers: { 'Content-Type': 'application/json' } });
    if (r.status === 200) {
      const t = r.json('token');
      return typeof t === 'string' ? t : t.token;
    }
    sleep(1.2); // el login admite 1 por segundo
  }
  throw new Error('No se pudo iniciar sesión');
}

function h(token, correlationId) {
  return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json',
                      'X-Correlation-Id': correlationId } };
}

function contar(r) {
  if (r.status >= 500) errores5xx.add(1);
  if (r.status === 429) limitadas.add(1);
}

export function setup() {
  const token = login();
  const cab = h(token, `carga-setup-${Date.now()}`);
  const clientes = http.get(`${BASE}/api/clientes`, cab).json();
  let vehiculo = null;
  let cliente = null;
  for (const c of clientes) {
    const vs = http.get(`${BASE}/api/clientes/${c.id}/vehiculos`, cab).json();
    if (vs.length > 0) { cliente = c.id; vehiculo = vs[0].id; break; }
  }
  const cotizaciones = [];
  for (let i = 0; i < EMISIONES; i++) {
    const q = http.post(`${BASE}/api/cotizaciones`,
      JSON.stringify({ clienteId: cliente, vehiculoId: vehiculo, siniestrosResponsables: 0 }), cab);
    if (q.status !== 201) throw new Error(`cotizar: ${q.status} ${q.body}`);
    const a = http.patch(`${BASE}/api/cotizaciones/${q.json('id')}/aceptar`, null, cab);
    if (a.status !== 200) throw new Error(`aceptar: ${a.status} ${a.body}`);
    cotizaciones.push(q.json('id'));
    sleep(0.05); // bajo el límite del gateway (50/s)
  }
  sleep(5); // que quote.accepted llegue a accepted_quotes de policy-service
  return { token, cotizaciones };
}

const RUTAS = ['/api/polizas', '/api/clientes', '/api/cotizaciones', '/api/renovaciones', '/api/tablas-tarifarias'];

export function lecturas(d) {
  const ruta = RUTAS[Math.floor(Math.random() * RUTAS.length)];
  const r = http.get(`${BASE}${ruta}`, h(d.token, `carga-lectura-${__VU}-${__ITER}`));
  contar(r);
  check(r, { 'lectura 200': (x) => x.status === 200 });
}

export function emision(d) {
  // Una cotización distinta por iteración: índice global del escenario, no el del VU.
  const i = exec.scenario.iterationInTest % d.cotizaciones.length;
  const r = http.post(`${BASE}/api/polizas`,
    JSON.stringify({ cotizacionId: d.cotizaciones[i], inicioVigencia: '2026-11-01' }),
    h(d.token, `carga-emision-${d.cotizaciones[i]}`));
  contar(r);
  check(r, { 'emision 201': (x) => x.status === 201 });
}

export function pico(d) {
  const r = http.get(`${BASE}/api/tablas-tarifarias`, h(d.token, `carga-pico-${__VU}-${__ITER}`));
  contar(r);
  check(r, { 'pico 200 o 429': (x) => x.status === 200 || x.status === 429 });
}
