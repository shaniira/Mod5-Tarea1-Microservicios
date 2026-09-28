// Genera c_DIAGRAMA-ARQUITECTURA-MICROSERVICIOS.svg: la arquitectura de microservicios tal como quedó
// implementada (2026-09-28, monolito retirado). Uso, desde la raíz del repositorio:
//   node "doc/5. Microservicios/diagramas/generar-diagrama-microservicios.js"
// y el PNG con Chrome sin ventana (ver diagramas/README.md).
const fs = require('fs');
const path = require('path');

const W = 1900, H = 1600;
const C = {
  azul: '#1f6fd0', verde: '#1e9e54', rojo: '#d62828', naranja: '#f08c00', gris: '#6b7280', morado: '#7c3aed',
  tinta: '#1e2a4a', texto: '#334155', suave: '#64748b'
};
const out = [];
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function text(x, y, s, o = {}) {
  const size = o.size || 11, fill = o.fill || C.texto, weight = o.bold ? 'bold' : 'normal';
  const anchor = o.anchor || 'start', style = o.italic ? 'font-style:italic' : '';
  out.push(`<text x="${x}" y="${y}" font-size="${size}" font-weight="${weight}" fill="${fill}" text-anchor="${anchor}" style="${style}">${esc(s)}</text>`);
}
function rect(x, y, w, h, fill, stroke, o = {}) {
  const dash = o.dash ? ` stroke-dasharray="${o.dash}"` : '';
  out.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${o.rx ?? 8}" fill="${fill}" stroke="${stroke}" stroke-width="${o.sw || 1.6}"${dash}/>`);
}
function lines(x, y, arr, o = {}) {
  const lh = o.lh || 15;
  arr.forEach((s, i) => text(x, y + i * lh, s, o));
  return y + arr.length * lh;
}
function box(x, y, w, h, fill, stroke, title, arr, o = {}) {
  rect(x, y, w, h, fill, stroke, o);
  text(x + 14, y + 24, title, { size: o.titleSize || 15, bold: true, fill: C.tinta });
  return lines(x + 14, y + 44, arr, { size: o.size || 11, lh: o.lh || 15 });
}
function badge(x, y, s, fill, stroke, color) {
  const w = s.length * 6.4 + 16;
  rect(x, y, w, 16, fill, stroke, { rx: 4, sw: 1 });
  text(x + w / 2, y + 12, s, { size: 10, bold: true, fill: color, anchor: 'middle' });
  return x + w + 6;
}
function flecha(pts, color, o = {}) {
  const marca = { [C.azul]: 'm-azul', [C.verde]: 'm-verde', [C.rojo]: 'm-rojo', [C.naranja]: 'm-naranja', [C.gris]: 'm-gris', [C.morado]: 'm-morado' }[color];
  const d = pts.map((p, i) => (i ? 'L' : 'M') + p[0] + ',' + p[1]).join(' ');
  const dash = o.dash ? ` stroke-dasharray="${o.dash}"` : '';
  const ini = o.doble ? ` marker-start="url(#${marca})"` : '';
  out.push(`<path d="${d}" fill="none" stroke="${color}" stroke-width="${o.sw || 2}"${dash}${ini} marker-end="url(#${marca})"/>`);
}

// --- Cabecera --------------------------------------------------------------------------------
out.push(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}" font-family="'Segoe UI', Helvetica, Arial, sans-serif">`);
out.push('<defs>' + [['m-azul', C.azul], ['m-verde', C.verde], ['m-rojo', C.rojo], ['m-naranja', C.naranja], ['m-gris', C.gris], ['m-morado', C.morado]]
  .map(([id, c]) => `<marker id="${id}" markerUnits="userSpaceOnUse" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto-start-reverse"><path d="M0,1 L11,6 L0,11 z" fill="${c}"/></marker>`).join('') + '</defs>');
rect(0, 0, W, H, '#f8fafc', '#f8fafc', { rx: 0 });
text(20, 38, 'Backend Seguros — Arquitectura de microservicios implementada', { size: 28, bold: true, fill: C.tinta });
text(20, 64, '6 microservicios + API Gateway · Docker Compose (docker-compose.yml en la raíz) y manifiestos de Kubernetes (k8s/) · estado al 2026-09-28: migración terminada y monolito retirado', { size: 14.5, fill: C.suave });

// --- Usuarios y navegador --------------------------------------------------------------------
box(20, 150, 230, 110, '#f1e8fb', '#9b72cf', 'Usuarios', ['Roles: ADMIN · AGENTE', 'ACTUARIO · CLIENTE', '', 'MFA con Google Authenticator'], { size: 10.5 });
flecha([[135, 260], [135, 298]], C.gris);
text(145, 283, 'usan', { size: 10, fill: C.suave });
box(20, 300, 230, 200, '#e8f1fd', '#6d9fe0', 'Cliente web (navegador)', ['• Vue 3.5 (SPA) · TypeScript', '• Pinia · Vue Router · Axios', '', 'Habla solo con el gateway.', 'Recibe X-Correlation-Id en', 'cada respuesta (soporte).', 'Con el perfil tls: HTTPS local', '(proxy Caddy 8443 / 8444).'], { size: 10.5 });
// Botón de Google / redirección de Facebook desde el navegador.
flecha([[10, 420], [10, 100], [1640, 100], [1640, 288]], C.azul, { dash: '6,4', sw: 1.6 });
text(300, 94, 'botón de Google / redirección OAuth de Facebook (desde el navegador)', { size: 10, fill: C.azul });

// --- Contenedor de Docker Compose ------------------------------------------------------------
rect(285, 130, 1060, 1135, 'none', '#7a93bd', { dash: '8,5', sw: 1.6, rx: 12 });
text(300, 150, 'Docker Compose · proyecto andina-clean · solo el frontend (5173) y el gateway (8080) publican puertos', { size: 12.5, bold: true, fill: '#2a5aa0' });

box(300, 165, 200, 105, '#e9f8ef', '#46b37a', 'Frontend (Nginx)', ['Puerto 5173 → 80', 'SPA Vue compilada;', 'llama solo al gateway', '(VITE_API_URL).'], { size: 10.5 });
flecha([[250, 330], [275, 330], [275, 215], [298, 215]], C.azul, { sw: 1.6 });
flecha([[250, 405], [298, 405]], C.azul, { doble: true });
text(253, 398, 'HTTP :8080', { size: 9.5, bold: true, fill: C.azul });
text(256, 423, '/api + JWT', { size: 9.5, fill: C.azul });

box(300, 285, 200, 555, '#eaf2fd', '#6d9fe0', 'API Gateway', [
  'Spring Cloud Gateway', 'OAuth2 Resource Server', '', 'Único puerto de API: 8080', '',
  '• Valida JWT RS256 (JWKS', '  de identity)', '• Rechaza tokens revocados:', '  Redis + copia local cada 5 s', '  (AP: sigue si Redis cae)',
  '• Enruta /api/** al servicio', '  dueño (siniestros antes', '  que pólizas)', '• Circuit breaker y timeout', '  por servicio (3–12 s);', '  503 + Retry-After',
  '• Límite de peticiones (Redis)', '  estricto en login y MFA', '• Genera X-Correlation-Id y', '  propaga traceparent', '• Compone "Mi cuenta"', '  (customer + policy,', '  respuesta parcial)',
  '• CORS único', '• Redis sin bloqueo: 2 s', '• 2 réplicas en Kubernetes'
], { size: 10.5, lh: 14.5 });
text(488, 832, 'edge + services', { size: 9, fill: C.suave, anchor: 'end', italic: true });

box(300, 870, 200, 200, '#fdecec', '#e07a7a', 'Redis', [
  'Interno (sin puerto)', '', '• Límite de peticiones', '• Lista de revocaciones', '• Estado efímero de identity:', '  state OAuth, tickets de login,', '  desafíos MFA (GETDEL)', '• Caché de placas (24 h)', '  y de claves de Google'
], { size: 10.5, lh: 14.5 });
flecha([[400, 842], [400, 868]], C.gris, { doble: true });

// --- Microservicios -------------------------------------------------------------------------
const SX = 570, SW = 440, SH = 122;
const servicios = [
  { y: 290, nombre: 'identity-service', rol: 'SOLO CONSUME', badges: ['Inbox', 'CB', 'Bulkhead'], lineas: [
    'Usuarios, login (contraseña, Google, Facebook), MFA, revocación (logout/baja)',
    'Firma JWT RS256 (clave privada solo aquí) y publica /.well-known/jwks.json',
    'Consume customer.registered/updated → customer_email_index',
    'Externos: Google y Facebook (CB + bulkhead + timeout); estado en Redis'] },
  { y: 420, nombre: 'customer-service', rol: 'SOLO PUBLICA', badges: ['Outbox', 'CB', 'Bulkhead'], lineas: [
    'Clientes, vehículos, consulta de placas',
    'API: /api/clientes/** · /api/vehiculos/informacion-externa',
    'Publica customer.registered/updated · vehicle.registered',
    'JSON.pe placas: CB + reintento + bulkhead + caché Redis (24 h)'] },
  { y: 550, nombre: 'quotation-service', rol: 'PUBLICA + CONSUME', badges: ['Outbox', 'Inbox', 'CB'], lineas: [
    'Tablas tarifarias, motor de tarificación, cotizaciones',
    'Publica quote.accepted · Consume customer.*, vehicle.registered,',
    'policy.issued (→ EMITIDA), policy.issuance-rejected (compensación)',
    'Proyecciones customer_ref, vehicle_ref + lectura de refuerzo a customer'] },
  { y: 680, nombre: 'policy-service', rol: 'PUBLICA + CONSUME', badges: ['Outbox', 'Inbox', 'CB'], lineas: [
    'Pólizas, Saga de emisión (índice único por cotización), renovaciones',
    'Publica policy.issued · policy.renewed · policy.issuance-rejected',
    'Consume quote.accepted → accepted_quotes · claim.* → claim_ref',
    'Al generar la renovada confirma siniestros con claims (CP: 503 si cae)'] },
  { y: 810, nombre: 'claims-service', rol: 'PUBLICA + CONSUME', badges: ['Outbox', 'Inbox'], lineas: [
    'Siniestros',
    'API: /api/polizas/{id}/siniestros/** · /api/siniestros/eventos/reenvio',
    'Publica claim.registered · claim.status-changed',
    'Consume policy.issued/renewed/expired/cancelled → policy_ref'] },
  { y: 940, nombre: 'notification-service', rol: 'SOLO CONSUME', badges: ['Inbox', 'CB'], lineas: [
    'Notificaciones: worker sin API HTTP ni puerto',
    'Consume policy.issued → WhatsApp · customer.* → customer_contacts',
    'CB de WhatsApp abierto → pausa el listener (los mensajes esperan',
    'en la cola, no van a la DLQ); DLQ con alerta y reproceso'] }
];
for (const s of servicios) {
  rect(SX, s.y, SW, SH, '#ffffff', '#1f5fb8', { sw: 1.8 });
  text(SX + 14, s.y + 22, s.nombre, { size: 14, bold: true, fill: C.tinta });
  const rw = s.rol.length * 6.3 + 16;
  rect(SX + 190, s.y + 9, rw, 16, '#fde2e2', C.rojo, { rx: 4, sw: 1 });
  text(SX + 190 + rw / 2, s.y + 21, s.rol, { size: 9.5, bold: true, fill: '#b42318', anchor: 'middle' });
  text(SX + SW - 10, s.y + 20, 'Clean Architecture + ArchUnit', { size: 9, fill: C.suave, anchor: 'end', italic: true });
  lines(SX + 14, s.y + 40, s.lineas, { size: 10.5, lh: 14.5 });
  let bx = SX + 14;
  for (const b of s.badges) {
    const est = b === 'Outbox' ? ['#fff4cc', '#e0b000', '#8a6d00'] : b === 'Inbox' ? ['#dcebfd', '#5b8fd6', '#1f5fb8'] : b === 'CB' ? ['#fde2e2', '#e07a7a', '#b42318'] : ['#e9f8ef', '#46b37a', '#1b7a45'];
    bx = badge(bx, s.y + SH - 22, b, ...est);
  }
  rect(SX + SW - 180, s.y + SH - 22, 168, 16, '#f1e8fb', '#9b72cf', { rx: 4, sw: 1 });
  text(SX + SW - 96, s.y + SH - 10, 'logs · métricas · trazas', { size: 10, bold: true, fill: '#5b3aa0', anchor: 'middle' });
  // Gateway → servicio (notification no tiene API).
  if (s.nombre !== 'notification-service') flecha([[500, s.y + 22], [568, s.y + 22]], C.azul);
}
text(505, 305, 'HTTP + JWT', { size: 9.5, fill: C.azul });

// Llamadas síncronas entre servicios (excepciones justificadas, con circuit breaker).
flecha([[SX, 640], [548, 640], [548, 500], [SX - 2, 500]], C.naranja, { sw: 2.2 });
text(543, 606, 'lectura de', { size: 9, bold: true, fill: '#b36200', anchor: 'end' });
text(543, 618, 'refuerzo', { size: 9, bold: true, fill: '#b36200', anchor: 'end' });
text(543, 630, '2 s · CB', { size: 9, fill: '#b36200', anchor: 'end' });
flecha([[SX, 772], [548, 772], [548, 890], [SX - 2, 890]], C.naranja, { sw: 2.2 });
text(543, 740, 'confirma', { size: 9, bold: true, fill: '#b36200', anchor: 'end' });
text(543, 752, 'siniestros', { size: 9, bold: true, fill: '#b36200', anchor: 'end' });
text(543, 764, 'CP·2 s·CB', { size: 9, fill: '#b36200', anchor: 'end' });

// --- Una base MongoDB por servicio -----------------------------------------------------------
rect(1030, 262, 180, 818, 'none', '#46b37a', { dash: '6,4', sw: 1.5 });
text(1120, 276, 'MongoDB 8 (replica set rs0)', { size: 10.5, bold: true, fill: '#1b7a45', anchor: 'middle' });
text(1120, 289, '1 instancia + 1 usuario por servicio', { size: 9.5, fill: '#1b7a45', anchor: 'middle' });
const bases = [['identity_db', 'identity-mongodb'], ['customer_db', 'customer-mongodb'], ['quotation_db', 'quotation-mongodb'], ['policy_db', 'policy-mongodb'], ['claims_db', 'claims-mongodb'], ['notification_db', 'notification-mongodb']];
servicios.forEach((s, i) => {
  const y = s.y + 14;
  rect(1042, y, 156, 58, '#efe7fb', '#9b72cf');
  text(1120, y + 23, bases[i][0], { size: 12, bold: true, fill: C.tinta, anchor: 'middle' });
  text(1120, y + 40, bases[i][1], { size: 9.5, fill: C.suave, anchor: 'middle' });
  text(1120, y + 52, 'red propia · sin puerto', { size: 8.5, fill: C.suave, anchor: 'middle' });
  flecha([[1012, y + 29], [1040, y + 29]], C.verde, { doble: true });
});

// --- Bus de eventos --------------------------------------------------------------------------
flecha([[1260, 330], [1260, 1098]], C.rojo, { sw: 3 });
out.push(`<text x="1280" y="740" font-size="10.5" font-weight="bold" fill="${C.rojo}" text-anchor="middle" transform="rotate(-90 1280 740)">bus de eventos: Outbox → RabbitMQ → Inbox (AMQP)</text>`);
const ev = [
  [290, 'consume: customer.*', false], [420, 'publica: customer.*, vehicle.registered', true],
  [550, 'publica: quote.accepted', true], [550, 'consume: customer.*, vehicle.*, policy.*', false],
  [680, 'publica: policy.*', true], [680, 'consume: quote.accepted, claim.*', false],
  [810, 'publica: claim.*', true], [810, 'consume: policy.*', false], [940, 'consume: policy.issued, customer.*', false]
];
const usados = {};
for (const [y, s, pub] of ev) {
  usados[y] = (usados[y] || 0) + 1;
  const yy = y + (usados[y] === 1 ? 92 : 108);
  if (pub) flecha([[1012, yy], [1258, yy]], C.rojo, { sw: 1.6 });
  else flecha([[1258, yy], [1014, yy]], C.rojo, { sw: 1.6, dash: '6,4' });
  text(1016, yy - 3, s, { size: 8.8, bold: true, fill: '#b42318' });
}

box(570, 1100, 760, 150, '#fff5f5', C.rojo, 'RabbitMQ 3.13 · exchange topic andina.events · DLX andina.events.dlx', [
  'Sin puertos en el host · hostname fijo (colas y mensajes sobreviven a recrear el contenedor) · 11 contratos en contracts/events',
  'customer.registered.v1 · customer.updated.v1 · vehicle.registered.v1 · quote.accepted.v1 · policy.issued.v1 · policy.renewed.v1',
  'policy.issuance-rejected.v1 · claim.registered.v1 · claim.status-changed.v1 · (policy.expired/cancelled.v1: contrato sin productor)',
  'Cada consumidor: su cola <servicio>.<origen>.events + su DLQ <cola>.dlq (alerta y reproceso con infra/rabbitmq/reprocesar-dlq.sh)',
  'Sobre de cada evento: eventId · eventType · eventVersion · occurredAt · aggregateId · aggregateVersion · correlationId · traceparent'
], { titleSize: 14, size: 10.5, lh: 16 });

// --- Columna derecha ---------------------------------------------------------------------------
box(1400, 290, 480, 118, '#fff8e1', '#e0b43a', 'Proveedores de identidad', ['• Google Identity (ID token, JWKS con caché)', '• Facebook OAuth + Graph API v22.0 (sin reintentos: código de un uso)', '• HTTPS · usados por identity-service y por el navegador'], { size: 11 });
flecha([[1012, 298], [1398, 298]], C.azul, { dash: '6,4', sw: 1.6 });
box(1400, 420, 480, 100, '#fff8e1', '#e0b43a', 'JSON.pe — consulta de placas', ['• POST /api/placa · Bearer token · HTTPS', '• Usado por customer-service; simulado con --profile jsonpe-mock'], { size: 11 });
flecha([[1012, 428], [1398, 428]], C.azul, { dash: '6,4', sw: 1.6 });
box(1400, 940, 480, 100, '#fff8e1', '#e0b43a', 'JSON.pe — WhatsApp', ['• POST /send/text · Bearer token · HTTPS', '• Usado por notification-service; simulado con --profile whatsapp-mock'], { size: 11 });
flecha([[1012, 948], [1398, 948]], C.azul, { dash: '6,4', sw: 1.6 });

rect(1400, 535, 480, 205, '#ffffff', '#cbd5e1');
text(1414, 559, 'Leyenda', { size: 15, bold: true, fill: C.tinta });
const ley = [[C.azul, '', 'HTTP síncrono (navegador → gateway → servicio)'], [C.azul, '6,4', 'HTTPS con proveedores externos (Internet)'],
  [C.verde, '', 'Acceso a la base propia del servicio (MongoDB)'], [C.rojo, '', 'Publica un evento (servicio → RabbitMQ, con Outbox)'],
  [C.rojo, '6,4', 'Consume un evento (RabbitMQ → servicio, con Inbox)'], [C.naranja, '', 'Llamada síncrona entre servicios (excepción, con CB)'],
  [C.morado, '6,4', 'Telemetría de todos los contenedores']];
ley.forEach(([c, d, s], i) => { const y = 582 + i * 19; flecha([[1414, y], [1462, y]], c, { dash: d || undefined }); text(1472, y + 4, s, { size: 11 }); });
let bx = badge(1414, 716, 'Outbox', '#fff4cc', '#e0b000', '#8a6d00'); text(bx, 728, 'evento en la misma transacción', { size: 9.5 });
bx = badge(1650, 716, 'Inbox', '#dcebfd', '#5b8fd6', '#1f5fb8'); text(bx, 728, 'descarta repetidos', { size: 9.5 });

box(1400, 752, 480, 175, '#eaf2fd', '#6d9fe0', 'Consistencia elegida por operación (CAP)', [
  '• Emitir póliza: Saga por coreografía; índice único por cotización',
  '  → una sola póliza aunque lleguen 2 pedidos a la vez; compensación',
  '  con policy.issuance-rejected',
  '• Generar renovada: CP — confirma siniestros con claims (503 si cae)',
  '• Revocar tokens: AP — copia local en el gateway (≤ 5 s) si Redis cae',
  '• Proyecciones: consistencia eventual con aggregateVersion +',
  '  reconciliación (infra/operacion/reconciliar.sh)'
], { size: 10.8, lh: 15 });

rect(1400, 1060, 480, 205, '#f6f0ff', C.morado, { sw: 2 });
text(1414, 1082, 'Observabilidad (infra/observability/, archivo -f aparte)', { size: 13, bold: true, fill: '#5b3aa0' });
const obs = [[1414, 1095, 'Promtail', 'logs JSON + correlationId'], [1560, 1095, 'Loki', 'almacén de logs'], [1414, 1140, 'OTel Collector', 'trazas OTLP'], [1560, 1140, 'Jaeger', ':16686'],
  [1414, 1185, 'Prometheus :9090', 'métricas + alertas'], [1560, 1185, 'Alertmanager :9093', 'correo por Gmail']];
for (const [x, y, t, st] of obs) { rect(x, y, 132, 38, '#ffffff', '#b89be6', { rx: 5, sw: 1.2 }); text(x + 66, y + 16, t, { size: 11, bold: true, fill: C.tinta, anchor: 'middle' }); text(x + 66, y + 30, st, { size: 9, fill: C.suave, anchor: 'middle' }); }
flecha([[1546, 1114], [1558, 1114]], C.morado, { sw: 1.5 }); flecha([[1546, 1159], [1558, 1159]], C.morado, { sw: 1.5 }); flecha([[1546, 1204], [1558, 1204]], C.morado, { sw: 1.5 });
rect(1712, 1095, 155, 128, '#ffffff', '#b89be6', { rx: 5, sw: 1.2 });
text(1790, 1113, 'Grafana :3000', { size: 11.5, bold: true, fill: C.tinta, anchor: 'middle' });
lines(1722, 1131, ['tablero Resumen', 'logs · métricas · trazas', 'búsqueda por', 'correlationId', 'blackbox: readiness', 'de cada servicio'], { size: 9.5, lh: 14, fill: C.suave });
flecha([[1694, 1114], [1710, 1114]], C.morado, { dash: '5,3', sw: 1.4 }); flecha([[1694, 1159], [1710, 1159]], C.morado, { dash: '5,3', sw: 1.4 });
text(1414, 1245, 'Un emitir póliza = una traza: gateway → policy → RabbitMQ → notification, claims, quotation', { size: 9.5, fill: '#5b3aa0' });
flecha([[1347, 1180], [1398, 1180]], C.morado, { dash: '6,4', sw: 1.6 });
text(1352, 1172, 'telemetría', { size: 9, fill: C.morado });

// --- Franja inferior -------------------------------------------------------------------------
rect(20, 1285, 900, 300, '#ffffff', '#cbd5e1', { rx: 10 });
text(36, 1310, 'Redes Docker (segmentación real del Compose) y puertos publicados', { size: 14, bold: true, fill: C.tinta });
const redes = [
  ['gateway_network', '#e8f1fd', '#6d9fe0', ['gateway · redis', 'proxy tls (perfil)', '', 'Publica: 8080']],
  ['services_network', '#e9f8ef', '#46b37a', ['gateway · identity', 'customer · quotation', 'policy · claims', 'Sin puertos']],
  ['rabbitmq_network', '#fdecec', '#e07a7a', ['rabbitmq · los 6', 'servicios', '', 'Sin puertos']],
  ['<servicio>_data_network ×6', '#f1e8fb', '#9b72cf', ['cada servicio con', 'SU MongoDB (identity', 'y customer, además Redis)', 'Nadie llega a otra base']],
  ['observability_network', '#f6f0ff', '#b89be6', ['servicios · gateway', 'rabbitmq · stack de', 'observabilidad', 'Publica 3000/9090/16686/9093']]
];
redes.forEach(([n, f, st, arr], i) => {
  const x = 36 + i * 176;
  rect(x, 1325, 166, 120, f, st, { rx: 6 });
  text(x + 10, 1345, n, { size: n.length > 20 ? 10 : 12, bold: true, fill: C.tinta });
  lines(x + 10, 1363, arr, { size: 10, lh: 14.5 });
});
lines(36, 1468, [
  'default: frontend (publica 5173). Perfil tls: proxy Caddy con CA local (publica 8443 y 8444).',
  'Secretos solo por variables de entorno (.env en la raíz, nunca versionado). Cada servicio valida el JWT y aplica rol y propietario (customerId).',
  'Contenedores sin root; healthchecks de MongoDB y RabbitMQ espaciados (5 s al arrancar, luego 30 s); cada servicio se "calienta" antes de declararse listo.',
  'Monolito retirado: código en la etiqueta de git monolito-final; su base, en respaldos/ (no versionado).'
], { size: 10.5, lh: 16 });

box(935, 1285, 470, 300, '#ffffff', '#cbd5e1', 'Calidad y operación (verificado)', [
  '• 272 pruebas automáticas + ArchUnit en cada servicio',
  '• Pruebas de contrato: 11 esquemas de eventos y 5 OpenAPI (CI)',
  '• 8 pipelines de GitHub Actions (6 servicios, gateway, k8s)',
  '• Caos: infra/operacion/caos.sh apaga cada servicio, RabbitMQ,',
  '  Redis o una base y comprueba degradación y cero eventos perdidos',
  '• Reconciliación de las 7 proyecciones contra su fuente',
  '• Respaldo de las 6 bases con prueba de restauración',
  '• Carga con k6 (infra/carga/carga.js)',
  '• Relay del Outbox con turno (lease en MongoDB): varias réplicas',
  '• Alertas: servicio caído o no listo, CB abierto, DLQ, Outbox',
  '  atrasado, 5xx, copia de revocaciones atrasada'
], { size: 10.8, lh: 17 });

box(1420, 1285, 460, 300, '#ffffff', '#cbd5e1', 'Kubernetes (k8s/, objetivo de despliegue)', [
  '• Namespace, ConfigMap y Secret (ejemplos) por servicio',
  '• Deployment + Service ClusterIP para los 6 servicios y el gateway',
  '• Gateway: 2 réplicas + HPA; identity: 2 réplicas',
  '• Ingress NGINX con TLS delante del gateway',
  '• MongoDB replica set con autenticación',
  '• RabbitMQ como StatefulSet con volumen persistente',
  '• Probes liveness / readiness / startup con timeoutSeconds',
  '• Validados con kubeconform en CI y --dry-run=server',
  '• Probados en kind (fase 0) y Minikube (MongoDB);',
  '  los de los servicios nuevos, sin aplicar todavía'
], { size: 10.8, lh: 17 });

out.push('</svg>');
const destino = path.join(__dirname, '..', 'c_DIAGRAMA-ARQUITECTURA-MICROSERVICIOS.svg');
fs.writeFileSync(destino, out.join('\n') + '\n');
console.log('Escrito', destino);
