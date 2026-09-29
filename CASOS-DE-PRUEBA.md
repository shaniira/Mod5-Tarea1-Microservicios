# Casos de prueba de Backend Seguros

Este documento reúne todas las pruebas del sistema de microservicios: qué se prueba, **por qué** y **qué pasa por dentro**, cómo replicarlo paso a paso y qué resultado esperar. Cada caso se comprobó contra el stack de Docker el 2026-09-28.

Antes de empezar, el proyecto tiene que estar levantado siguiendo [GUIA-INSTALACION-EQUIPO.md](GUIA-INSTALACION-EQUIPO.md).

## 0. Preparación

### 0.1 Levantar el stack para probar

Para las pruebas conviene levantar tres cosas más:
- los **simuladores** de WhatsApp y JSON.pe, para no enviar mensajes a teléfonos reales;
- la **observabilidad**;
- la **consola de RabbitMQ**, que abre `docker-compose.debug.yml`.

```bash
WHATSAPP_BASE_URL=http://whatsapp-mock:8080 JSONPE_BASE_URL=http://jsonpe-mock:8080 \
docker compose -f docker-compose.yml -f docker-compose.debug.yml \
  -f infra/observability/docker-compose.observability.yml \
  --profile whatsapp-mock --profile jsonpe-mock up -d
docker compose ps        # espera a que todo diga "healthy"
```

| Herramienta | Dirección | Usuario |
|---|---|---|
| Frontend | http://localhost:5173 | `admin` / `Admin123*` |
| API (gateway) | http://localhost:8080/api | token del login |
| Consola de RabbitMQ | http://localhost:15672 | `andina` / `andina-local` |
| Grafana | http://localhost:3000 | `admin` / `grafana-local` |
| Jaeger | http://localhost:16686 | — |
| Prometheus | http://localhost:9090 | — |
| Alertmanager | http://localhost:9093 | — |

### 0.2 Variables y funciones de ayuda (pegar una vez en Git Bash)

```bash
API=http://localhost:8080/api
H='Content-Type: application/json'
# campo 'expresión': lee JSON de la entrada y muestra la expresión (j es el JSON)
campo() { node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{try{const j=JSON.parse(s);console.log($1)}catch(e){console.log(s)}})"; }
# login <usuario> <clave>: deja el token en TOKEN y la cabecera en AUTH
login() { TOKEN=$(curl -s -X POST $API/auth/login -H "$H" -d "{\"username\":\"$1\",\"password\":\"$2\"}" | campo 'j.token && j.token.token'); AUTH="Authorization: Bearer $TOKEN"; echo "token: ${TOKEN:0:15}..."; }
login admin 'Admin123*'
ANA=10000000-0000-0000-0000-000000000001; AUTO_ANA=20000000-0000-0000-0000-000000000001
LUIS=10000000-0000-0000-0000-000000000002; AUTO_LUIS=20000000-0000-0000-0000-000000000002
```

- Si `login` muestra `token: null...`, esa cuenta tiene MFA: usa otra (caso I-03).
- El login admite 1 intento por segundo: espera entre dos `login` seguidos.

### 0.3 Cómo leer los resultados

- **HTTP**: `201` creado, `200` correcto, `401` sin sesión válida, `403` sin permiso, `404` no existe, `422` regla de negocio (el cuerpo trae `codigo` y `mensaje`), `503` servicio no disponible (trae `Retry-After`).
- **Logs de un servicio**: `docker logs <contenedor> --since 2m`. Son JSON, con `correlationId` y `traceId`.
- **Colas**: `docker exec backend-seguros-rabbitmq-1 rabbitmqctl -q list_queues name messages consumers`, o la consola de RabbitMQ.

## 1. Mapa: qué hace cada servicio y cómo se hablan

```
Navegador ──HTTP──► api-gateway ──► identity   (login, MFA, tokens)
                         │     ├──► customer   (clientes, vehículos, placas)  ──► JSON.pe
                         │     ├──► quotation  (tarifas, cotizaciones) ──lectura de refuerzo──► customer
                         │     ├──► policy     (pólizas, renovaciones) ──confirma siniestros──► claims
                         │     └──► claims     (siniestros)
                         └── Redis (límite de peticiones, revocaciones)

Eventos por RabbitMQ (exchange andina.events). Cada servicio publica con Outbox y consume con Inbox:
  customer  ──customer.registered/updated, vehicle.registered──► identity, quotation, notification
  quotation ──quote.accepted──► policy
  policy    ──policy.issued──► quotation (marca EMITIDA), claims (policy_ref), notification (WhatsApp)
  policy    ──policy.issuance-rejected──► quotation (compensación)
  policy    ──policy.renewed──► claims
  claims    ──claim.registered/status-changed──► policy (claim_ref: bloquea renovaciones)
```

**Saga de emisión:** aceptar una cotización publica `quote.accepted`, y policy guarda su copia (`accepted_quotes`). Al emitir, policy valida contra esa copia, sin llamar a quotation. Crea la póliza y publica `policy.issued` en la misma transacción (Outbox). quotation, claims y notification reaccionan cada una por su lado. Si la emisión se rechaza (por ejemplo, porque ya estaba emitida), policy publica `policy.issuance-rejected` y quotation registra el rechazo.

**Renovación:**
1. Evaluar la renovación mira los siniestros de la póliza en la copia de policy (`claim_ref`).
2. Aprobarla la deja lista.
3. Generar la póliza renovada, el paso irreversible, vuelve a confirmar los siniestros llamando a claims. Si claims no responde, responde 503: es preferible un error a renovar con un dato viejo (decisión CP).

## 2. Flujo principal de negocio (de punta a punta)

Es el caso más importante: recorre 5 servicios, la saga y los eventos. Ejecútalo en orden.

| # | Paso | Comando | Resultado esperado | Qué demuestra |
|---|---|---|---|---|
| F-01 | Listar clientes | `curl -s -H "$AUTH" $API/clientes \| campo 'j.map(c=>c.nombres).join(", ")'` | Ana, Luis… | customer-service responde por el gateway |
| F-02 | Cotizar | `COT=$(curl -s -X POST $API/cotizaciones -H "$AUTH" -H "$H" -d "{\"clienteId\":\"$ANA\",\"vehiculoId\":\"$AUTO_ANA\",\"siniestrosResponsables\":0}" \| campo 'j.id'); echo $COT` | Un id. Con `campo 'j.prima'`: **1412.5** (1250 base + 10 % de gastos + 3 % de recargo) | Motor de tarificación de quotation, con los datos del cliente tomados de su copia local |
| F-03 | Aceptar | `curl -s -X PATCH $API/cotizaciones/$COT/aceptar -H "$AUTH" -o /dev/null -w '%{http_code}\n'` | `200` | Publica `quote.accepted` (Outbox) |
| F-04 | Esperar la copia en policy | `sleep 3` | — | Consistencia eventual: el evento tarda 1–2 s en llegar |
| F-05 | Emitir | `POL=$(curl -s -X POST $API/polizas -H "$AUTH" -H "$H" -d "{\"cotizacionId\":\"$COT\",\"inicioVigencia\":\"$(date -d '+7 days' +%F)\"}" \| campo 'j.id'); echo $POL` | Un id de póliza (HTTP 201) | Saga: policy valida contra `accepted_quotes` y publica `policy.issued` |
| F-06 | La cotización quedó emitida | `sleep 3; curl -s -H "$AUTH" $API/cotizaciones/$COT \| campo 'j.estado'` | `EMITIDA` | quotation consumió `policy.issued` |
| F-07 | WhatsApp enviado | `docker logs notification-service --since 1m 2>&1 \| grep -o '"message":"[^"]*ENVIADA'` | `... ENVIADA` | notification consumió `policy.issued` y usó su copia de contactos |
| F-08 | Claims conoce la póliza | Registrar un siniestro (F-09) sin error | — | claims guardó la póliza en su `policy_ref` |
| F-09 | Siniestro responsable | `SIN=$(curl -s -X POST $API/polizas/$POL/siniestros -H "$AUTH" -H "$H" -d "{\"fecha\":\"$(date -d '+10 days' +%F)\",\"tipo\":\"CHOQUE\",\"montoEstimado\":700,\"responsabilidadAsegurado\":true,\"gravedad\":\"LEVE\",\"estado\":\"REPORTADO\"}" \| campo 'j.id'); echo $SIN` | Un id (201) | claims publica `claim.registered` |
| F-10 | Renovar con el siniestro abierto | `sleep 3; curl -s -X POST $API/renovaciones/poliza/$POL/evaluar -H "$AUTH" \| campo 'j.codigo'` | `SINIESTROS_PENDIENTES` (422) | policy recibió el evento de claims y bloquea |
| F-11 | Cerrar el siniestro | `curl -s -X PATCH $API/polizas/$POL/siniestros/$SIN/estado -H "$AUTH" -H "$H" -d '{"estado":"LIQUIDADO"}' -o /dev/null -w '%{http_code}\n'` | `200` | Publica `claim.status-changed` |
| F-12 | Evaluar otra vez | `sleep 3; REN=$(curl -s -X POST $API/renovaciones/poliza/$POL/evaluar -H "$AUTH" \| campo 'j.id+" "+j.estado+" "+j.nuevaPrima'); echo $REN; REN=${REN%% *}` | `REQUIERE_RECALCULO` y prima **+8 %** (1412.5 → 1525.5) | La prima de renovación considera el siniestro responsable |
| F-13 | Aprobar | `curl -s -X PATCH $API/renovaciones/$REN/aprobar -H "$AUTH" -o /dev/null -w '%{http_code}\n'` | `200` | — |
| F-14 | Generar la renovada | `curl -s -X POST $API/renovaciones/$REN/generar-poliza -H "$AUTH" \| campo 'j.numero'` | `POL-REN-…` (201) | policy confirma con claims (llamada síncrona) y publica `policy.renewed` |
| F-15 | La original ya no está vigente | Repetir el POST de F-09 sobre `$POL` | `POLIZA_NO_VIGENTE` (422) | claims recibió `policy.renewed` y actualizó su copia |

## 3. Casos por servicio

### 3.1 identity-service (usuarios, login, tokens)

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| I-01 | Login correcto | `login admin 'Admin123*'` | Token JWT (RS256) |
| I-02 | Clave incorrecta | `curl -s -X POST $API/auth/login -H "$H" -d '{"username":"admin","password":"mal"}' -w ' %{http_code}\n'` | `401` |
| I-03 | Cuenta con MFA | Login de una cuenta con MFA activado | `requiresMfa: true` y un `challengeToken`; el token llega tras `POST /api/auth/mfa/verificar` con el código de Google Authenticator |
| I-04 | Crear personal (ADMIN) | `curl -s -X POST $API/auth/register -H "$AUTH" -H "$H" -d '{"username":"actuario1","password":"Actuario123*","rol":"ACTUARIO"}' -w '%{http_code}\n'` | `201` |
| I-05 | Registro público no crea ADMIN | El mismo POST **sin** `-H "$AUTH"` y con `"rol":"ADMIN"` | `403 ROL_NO_PERMITIDO` |
| I-06 | Registro de cliente | `curl -s -X POST $API/auth/register -H "$H" -d '{"username":"ana.demo@backendseguros.local","password":"Cliente123*"}' -w '%{http_code}\n'` | `201`. El token de esa cuenta trae el `customerId` de Ana: identity lo buscó en su copia de correos, que alimenta `customer.*`. En bases creadas antes del 2026-09-28 el correo de Ana es `ana.demo@andina.local` (compruébalo con `GET $API/clientes/$ANA`) |
| I-07 | Logout | `curl -s -X POST $API/auth/logout -H "$AUTH" -w '%{http_code}\n'` y luego cualquier GET con el mismo token | `204` y después `401`: el token queda revocado en Redis |
| I-08 | Claves públicas (JWKS) | `docker exec api-gateway wget -qO- http://identity-service:8080/.well-known/jwks.json` | Una clave RSA con `kty, e, use, kid, alg, n`: **sin** partes privadas (`d`, `p`, `q`) |

### 3.2 customer-service (clientes, vehículos, placas)

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| C-01 | Crear cliente | `POST $API/clientes` con `tipoDocumento`, `numeroDocumento`, `nombres`, `apellidos`, `fechaNacimiento`, `correo`, `telefono` | `201`. Publica `customer.registered`: en segundos aparece en las copias de identity, quotation y notification (verifícalo con la reconciliación, caso D-06) |
| C-02 | Documento repetido | Repetir C-01 con el mismo documento | `422 DOCUMENTO_DUPLICADO` |
| C-03 | Consultar placa (simulador) | `curl -s -H "$AUTH" "$API/vehiculos/informacion-externa?placa=ABC123"` | TOYOTA YARIS, `fuente: JSON_PE`; queda en caché de Redis 24 h |
| C-04 | Placa con JSON.pe caído | `docker stop jsonpe-mock` y consultar `ABC123` y luego una placa nueva | La ya consultada sale de la caché; la nueva, `SIN_DATOS` o `503` (registro manual). Después: `docker start jsonpe-mock` |
| C-05 | Cambiar contacto | `curl -s -X PATCH $API/clientes/$ANA/contacto -H "$AUTH" -H "$H" -d '{"correo":"ana.nueva@backendseguros.local","telefono":"900000002"}' -w ' %{http_code}\n'` | `200`. Publica `customer.updated`; las copias de identity, quotation y notification suben de versión (reconciliación, D-06) |

### 3.3 quotation-service (tarifas y cotizaciones)

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| QT-01 | Tablas tarifarias | `curl -s -H "$AUTH" $API/tablas-tarifarias \| campo 'j.map(t=>t.codigo+" v"+t.version+" "+t.estado).join("\n")'` | DEMO-AUTO v1 VIGENTE, DEMO-CAMIONETA v1 VIGENTE, DEMO-AUTO v2 BORRADOR |
| QT-02 | Otro tipo de vehículo | F-02 con `$LUIS` y `$AUTO_LUIS` (camioneta) | Prima **1898.4** (1680 base de la tabla de camionetas + 10 % + 3 %): el motor elige la tabla según el tipo de vehículo |
| QT-03 | Cotizar sin customer-service | `docker stop customer-service`, luego F-02 con Ana | `201`: usa su copia local (`customer_ref`, `vehicle_ref`). Después: `docker start customer-service` |
| QT-04 | Aceptar dos veces | F-03 dos veces | La segunda: `422 COTIZACION_NO_VIGENTE` |

### 3.4 policy-service (pólizas, saga, renovaciones)

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| P-01 | Doble emisión | Repetir F-05 con la misma `$COT` | `422`. La saga garantiza **una sola póliza por cotización** (índice único) |
| P-02 | Compensación de la saga | Tras P-01, consultar los rechazos en quotation (abajo) | Un documento `COTIZACION_YA_EMITIDA` para esa cotización: quotation recibió `policy.issuance-rejected` |
| P-03 | Emitir sin aceptar | F-05 con una cotización recién creada, sin F-03 | `422 COTIZACION_NO_ACEPTADA`: policy no la tiene en `accepted_quotes` |
| P-04 | Mis pólizas (cliente) | Con la cuenta de I-06: `curl -s -H "$AUTH" $API/mi-cuenta` | El cliente Ana con sus pólizas. El gateway **compone** customer + policy |

Consulta de P-02:
```bash
MSYS_NO_PATHCONV=1 docker exec quotation-mongodb sh -c 'mongosh --quiet -u root -p "$MONGO_ROOT_PASSWORD" --authenticationDatabase admin quotation_db --eval "printjson(db.rechazos_emision.find({cotizacionId:\"'$COT'\"}).toArray())"'
```

### 3.5 claims-service (siniestros)

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| S-01 | Registrar | F-09 | `201` |
| S-02 | Cambiar estado de uno cerrado | F-11 dos veces (LIQUIDADO → LIQUIDADO) | `422 SINIESTRO_CERRADO` |
| S-03 | Póliza que no existe | F-09 con un UUID inventado | `404 RECURSO_NO_ENCONTRADO` |
| S-04 | Póliza no vigente | F-15 | `422 POLIZA_NO_VIGENTE` |
| S-05 | Siniestros de una póliza | `curl -s -H "$AUTH" $API/polizas/$POL/siniestros` | Lista con su estado |

### 3.6 notification-service (WhatsApp)

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| N-01 | Envío al emitir | F-07 | `ENVIADA` en el log |
| N-02 | WhatsApp caído | `bash infra/operacion/caos.sh whatsapp` | El circuito se abre, el consumo se **pausa**, nada va a la DLQ; al volver salen los mensajes retenidos |
| N-03 | Evento repetido | Caso D-04 | `DUPLICADO` en el log: no envía dos veces |

### 3.7 api-gateway

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| G-01 | Correlation-Id | `curl -s -i -H "$AUTH" $API/clientes \| grep -i x-correlation` | Cabecera `X-Correlation-Id` (la genera el gateway) |
| G-02 | Correlation-Id propio | Igual, con `-H "X-Correlation-Id: mi-prueba-1"` | La respuesta devuelve `mi-prueba-1` |
| G-03 | Límite de peticiones en login | 8 logins seguidos en paralelo: `for i in $(seq 8); do curl -s -o /dev/null -w '%{http_code} ' -X POST $API/auth/login -H "$H" -d '{"username":"x","password":"y"}' & done; wait` | Algunos `429` |
| G-04 | Ruta inexistente | `curl -s -H "$AUTH" $API/no-existe -w ' %{http_code}\n'` | `404` |
| G-05 | Id mal formado | `curl -s -H "$AUTH" $API/polizas/abc -w ' %{http_code}\n'` | `400 SOLICITUD_MAL_FORMADA` |

## 4. Seguridad (rol y propietario)

Cada servicio valida el token y aplica rol y propietario, no solo el gateway.

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| SEG-01 | Sin token | `curl -s -o /dev/null -w '%{http_code}\n' $API/polizas` | `401` |
| SEG-02 | Token alterado | Con `-H "Authorization: Bearer ${TOKEN}x"` | `401` (firma inválida) |
| SEG-03 | ACTUARIO no cotiza | `login actuario1 'Actuario123*'` (I-04) y F-02 | `403 ACCESO_DENEGADO` |
| SEG-04 | ACTUARIO sí lee | Con ese token: `GET $API/polizas` | `200` |
| SEG-05 | Cliente ve lo suyo | `login ana.demo@backendseguros.local 'Cliente123*'` (I-06); `GET $API/clientes/$ANA` | `200` |
| SEG-06 | Cliente no ve lo ajeno | `GET $API/clientes/$LUIS` y `GET $API/clientes` | `403` y `403` |
| SEG-07 | Sesión cerrada | I-07 | `401` tras el logout |

Roles:
- **ADMIN** hace todo.
- **AGENTE** opera: clientes, cotizaciones, pólizas y siniestros.
- **ACTUARIO** lee y administra tarifas.
- **CLIENTE** solo ve lo propio (`customerId` del token).

## 5. Fallas y resiliencia (pruebas de caos)

`infra/operacion/caos.sh` apaga un componente, comprueba la degradación prevista, lo vuelve a levantar y verifica que no se perdió nada. Requiere los simuladores (0.1).

```bash
bash infra/operacion/caos.sh <caso> [<caso>...]   # uno o varios
bash infra/operacion/caos.sh                      # los básicos (~25 min)
bash infra/operacion/caos.sh todos                # básicos + consistencia + largos (~40 min)
```

| Caso | Qué se apaga | Qué debe pasar |
|---|---|---|
| `claims` | claims-service | Siniestros responden 503; emitir sigue funcionando; `policy.issued` espera en la cola y claims lo aplica al volver |
| `notification` | notification-service | Todo funciona salvo el WhatsApp, que sale al volver |
| `customer` | customer-service | Clientes responde 503; se sigue cotizando con la copia local |
| `quotation` | quotation-service | Cotizar responde 503; una cotización ya aceptada se emite igual; al volver queda EMITIDA |
| `policy` | policy-service | Pólizas responde 503; aceptar cotizaciones sigue funcionando y el evento se aplica al volver |
| `identity` | identity-service | Login 503; los tokens ya emitidos siguen sirviendo (el gateway guarda las claves públicas) |
| `rabbitmq` | RabbitMQ | Emitir responde 201; el evento queda PENDING en el Outbox y se publica al volver: **ningún evento perdido** |
| `redis` | Redis | El gateway sigue atendiendo; el login funciona |
| `mongo-policy` | La base de policy | Solo pólizas deja de atender; el resto sigue |
| `whatsapp` | Simulador de WhatsApp | Circuito abierto, consumo pausado, DLQ en 0; al volver salen los retenidos |
| `jsonpe` | Simulador de JSON.pe | Placa ya consultada: caché; placa nueva: 503 |
| `renovacion-reciente` | — | Siniestro registrado justo antes de generar la renovada: se bloquea (422) aunque la copia de policy aún no lo tenga |
| `renovacion-claims` | claims al renovar | Generar responde 503 `SINIESTROS_NO_DISPONIBLE`; la póliza sigue VIGENTE; al volver claims, se genera |
| `revocacion-redis` | Redis | Un token cerrado sigue rechazado (copia local del gateway); uno vigente sigue sirviendo |
| `replicas` | Una de 2 réplicas de policy | Las dos emiten; cada evento sale una sola vez; la otra réplica toma el turno del Outbox |
| `relay-lote` | RabbitMQ con 2 réplicas | Un lote acumulado sale **exactamente una vez** |
| `identity-larga` | identity 6 minutos | Más que la caché de claves de 5 min: los tokens siguen sirviendo |

Resultado final esperado: `Todas las degradaciones se comportaron como estaba planificado.`

- Si un caso falla justo después de levantar el stack, repítelo: con los servicios "en frío" la primera respuesta puede superar el límite de tiempo.
- Si un servicio queda detenido tras un caso interrumpido, levántalo con `docker compose up -d`.

## 6. Colas, DLQ y mensajes perdidos

Cada consumidor tiene su cola `<servicio>.<origen>.events` y su DLQ `<cola>.dlq`. Un mensaje que falla varias veces va a la DLQ: no se pierde, queda apartado y dispara una alerta.

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| Q-01 | Ver colas | `docker exec backend-seguros-rabbitmq-1 rabbitmqctl -q list_queues name messages consumers` o la consola (http://localhost:15672 → Queues) | 16 colas (8 principales + 8 DLQ), todas en 0 y cada principal con consumidores |
| Q-02 | Mensaje venenoso | Publicar un mensaje que no es JSON en la cola de notification (abajo) | Tras los reintentos, `notification.policy.events.dlq` = 1; en el log de notification: `Mensaje … enviado a la DLQ` y `La DLQ … tiene 1 mensaje(s)` |
| Q-03 | Alerta de DLQ | Esperar ~1–2 min | Prometheus → Alerts: `DlqConMensajes` en **firing** para esa cola; también en Alertmanager |
| Q-04 | Reprocesar sin corregir la causa | `sh infra/rabbitmq/reprocesar-dlq.sh notification.policy.events.dlq` | El script mueve el mensaje a la cola principal; como sigue sin ser JSON, **vuelve a la DLQ** (1). Primero se corrige la causa y después se reprocesa |
| Q-05 | Descartar el mensaje | `docker exec backend-seguros-rabbitmq-1 rabbitmqctl -q purge_queue notification.policy.events.dlq` | DLQ en 0; la alerta se resuelve sola en 1–2 min |
| Q-06 | Ningún evento perdido con RabbitMQ caído | `bash infra/operacion/caos.sh rabbitmq` | Outbox PENDING → publicado al volver (sección 5) |
| Q-07 | Consumo pausado en vez de DLQ | `bash infra/operacion/caos.sh whatsapp` | Con WhatsApp caído la cola retiene los mensajes (0 consumidores) y la DLQ sigue en 0 |

Publicar el mensaje venenoso (Q-02) por la API de la consola de RabbitMQ:
```bash
curl -s -u andina:andina-local -X POST http://localhost:15672/api/exchanges/%2F/amq.default/publish -H "$H" \
  -d '{"properties":{"content_type":"application/json"},"routing_key":"notification.policy.events","payload":"esto no es JSON","payload_encoding":"string"}'
```

Qué hacer con una DLQ real (causa, corrección y reproceso) está en [o_GUIA-OPERACION.md](doc/5.%20Microservicios/o_GUIA-OPERACION.md).

## 7. Consistencia de datos (patrones distribuidos)

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| D-01 | Outbox (atomicidad cambio + evento) | Q-06 | Ningún evento perdido |
| D-02 | Saga y doble emisión | P-01 y P-02 | Una sola póliza y la compensación registrada |
| D-03 | Consistencia eventual | F-03 → F-05 **sin** el `sleep 3` | Puede responder 422 si el evento aún no llegó; 1–2 s después, funciona |
| D-04 | Idempotencia (Inbox) | Reenviar a notification un `policy.issued` ya procesado (abajo) | Log `PolicyIssued POL-…: DUPLICADO`; no se envía otro WhatsApp |
| D-05 | CP al renovar | `bash infra/operacion/caos.sh renovacion-reciente renovacion-claims` | Nunca se renueva con un siniestro no visto |
| D-06 | Reconciliación | `sh infra/operacion/reconciliar.sh` | 7 comparaciones `OK` y `Todo coincide.` |
| D-07 | Respaldo y restauración | `sh infra/mongo/respaldar.sh` y luego `sh infra/mongo/probar-restauracion.sh respaldos/<carpeta>` | Las 6 bases respaldadas; la restauración de prueba da los mismos conteos |

Reenviar un evento ya procesado (D-04):
```bash
MSYS_NO_PATHCONV=1 docker exec policy-mongodb sh -c 'mongosh --quiet -u root -p "$MONGO_ROOT_PASSWORD" --authenticationDatabase admin policy_db --eval "print(db.outbox.find({routingKey:\"policy.issued.v1\"}).sort({createdAt:-1}).limit(1).toArray()[0].payload)"' > evento.json
node -e "const p=require('fs').readFileSync('evento.json','utf8').trim();require('fs').writeFileSync('pub.json',JSON.stringify({properties:{content_type:'application/json'},routing_key:'notification.policy.events',payload:p,payload_encoding:'string'}))"
curl -s -u andina:andina-local -X POST http://localhost:15672/api/exchanges/%2F/amq.default/publish -H "$H" -d @pub.json
sleep 5; docker logs notification-service --since 20s 2>&1 | grep -o '"message":"[^"]*DUPLICADO'
rm evento.json pub.json
```

## 8. Observabilidad

Requiere el archivo de observabilidad (0.1).

| # | Caso | Cómo | Esperado |
|---|---|---|---|
| O-01 | Seguir una petición en los logs | Emitir con un id propio: agrega `-H "X-Correlation-Id: guia-1"` a F-05. En Grafana → Explore → Loki: `{service=~".+"} \|= "guia-1"` | Líneas de gateway, quotation, claims y notification con ese `correlationId` y **el mismo `traceId`** |
| O-02 | La traza completa | Copia el `traceId` de O-01 y búscalo en Jaeger (http://localhost:16686 → Search → Trace ID) | Una sola traza con spans de api-gateway, policy-service, claims-service, quotation-service y notification-service: la petición HTTP continúa a través de RabbitMQ |
| O-03 | Tablero | Grafana → Dashboards → Backend Seguros → "Backend Seguros — Resumen" | Peticiones, errores, latencia, circuitos, colas y Outbox por servicio |
| O-04 | Métricas | Prometheus (http://localhost:9090) → Graph, por ejemplo: `rabbitmq_detailed_queue_messages`, `outbox_events_oldest_pending_age_seconds`, `resilience4j_circuitbreaker_state`, `notification_listener_paused`, `gateway_revocaciones_copia_edad_seconds` | Valores por servicio/cola |
| O-05 | Alerta de servicio caído | `docker stop claims-service`; esperar ~2 min; Prometheus → Alerts | `ServicioCaido` (job claims-service) y `ServicioNoListo` en **firing** |
| O-06 | La alerta se resuelve | `docker start claims-service`; esperar ~1–2 min | Sin alertas activas |
| O-07 | Alertmanager | http://localhost:9093 durante O-05 o Q-03 | La alerta aparece agrupada. Con `ALERTMANAGER_SMTP_PASSWORD` configurada llega un correo; sin ella, el log de `alertmanager` muestra el intento fallido |
| O-08 | Circuito abierto | `bash infra/operacion/caos.sh whatsapp` y mirar `resilience4j_circuitbreaker_state{state="open"}` | Vale 1 mientras WhatsApp está caído; con más de 2 min, alerta `CircuitoAbierto` |

Alertas definidas (`infra/observability/prometheus/alertas.yml`):

| Alerta | Se dispara cuando |
|---|---|
| `ServicioCaido` | Prometheus no puede leer las métricas de un servicio durante 1 min |
| `ServicioNoListo` | El readiness de un servicio falla durante 1 min |
| `ErroresServidorAltos` | Más del 5 % de respuestas 5xx durante 5 min |
| `CircuitoAbierto` | Un circuit breaker lleva 2 min abierto |
| `DlqConMensajes` | Una DLQ tiene mensajes durante 1 min |
| `OutboxAtrasado` | Un evento lleva más de 5 min sin publicarse |
| `NotificacionesPausadas` | El consumo de notification lleva 2 min pausado |
| `CopiaRevocacionesAtrasada` | La copia de revocaciones del gateway tiene más de 60 s |

## 9. Carga

```bash
docker run --rm -i --network gateway_network -e BASE=http://gateway:8080 grafana/k6:0.54.0 run - < infra/carga/carga.js
```

Mide lecturas, emisión y el límite del gateway. Esperado:
- 0 errores 5xx.
- El exceso sobre el límite (50 peticiones/s) responde `429`.
- Referencia en un equipo de 4 CPU: lecturas con p95 ≈ 1,7 s y emisión con p95 ≈ 1,8 s (`n_…`, sección 3.4).

## 10. Pruebas automáticas

**272 pruebas** en total:

| Servicio | Pruebas |
|---|---|
| identity | 62 |
| policy | 52 |
| quotation | 48 |
| customer | 43 |
| claims | 33 |
| notification | 30 |
| gateway | 4 |

Incluyen reglas de arquitectura (ArchUnit), pruebas de contrato contra `contracts/` y reglas de negocio. Corren al construir cada imagen y en CI. Para correrlas a mano, mira [GUIA-INSTALACION-EQUIPO.md](GUIA-INSTALACION-EQUIPO.md), sección 6.8.

## 11. Planilla de resultados

Copia esta tabla y marca cada caso al probarlo.

| Área | Casos | Resultado | Observaciones |
|---|---|---|---|
| Flujo principal | F-01 a F-15 | ☐ | |
| identity | I-01 a I-08 | ☐ | |
| customer | C-01 a C-05 | ☐ | |
| quotation | QT-01 a QT-04 | ☐ | |
| policy | P-01 a P-04 | ☐ | |
| claims | S-01 a S-05 | ☐ | |
| notification | N-01 a N-03 | ☐ | |
| gateway | G-01 a G-05 | ☐ | |
| Seguridad | SEG-01 a SEG-07 | ☐ | |
| Caos | Los casos de la sección 5 | ☐ | |
| Colas y DLQ | Q-01 a Q-07 (sección 6) | ☐ | |
| Consistencia | D-01 a D-07 | ☐ | |
| Observabilidad | O-01 a O-08 | ☐ | |
| Carga | Sección 9 | ☐ | |
| Pruebas automáticas | 272 | ☐ | |

Al terminar: `docker compose -f docker-compose.yml -f docker-compose.debug.yml -f infra/observability/docker-compose.observability.yml --profile whatsapp-mock --profile jsonpe-mock stop`.
