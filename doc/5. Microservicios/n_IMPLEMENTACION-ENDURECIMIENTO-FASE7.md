# Endurecimiento y cierre (fase 7 de la migración a microservicios)

Este documento registra lo que se hizo en la fase 7 de la [ruta de implementación](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#11-fase-7--endurecimiento-y-cierre): las pruebas, lo que encontraron, cómo se corrigió y cómo quedó el sistema frente a los criterios de "terminado" de la [propuesta](c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md#11-criterios-de-aceptación-definición-de-terminado). Cómo operar el sistema día a día está en [o_GUIA-OPERACION.md](o_GUIA-OPERACION.md).

**Resultado:** los 9 pasos de la fase están hechos y verificados contra el stack de Compose. Las pruebas encontraron **12 defectos reales** (uno más en la revisión posterior, sección 9) que no se habían visto en las fases anteriores (sección 2). Los más graves:

- con Redis caído, el gateway se colgaba;
- el relay del Outbox no admitía 2 réplicas;
- el corte dejó copias de datos divergentes y huérfanas;
- `quote.accepted` no cumplía su contrato.

Todos están corregidos y vueltos a probar. Quedan documentadas las decisiones que no se implementaron (sección 6).

Fecha: 2026-09-28. Entorno de las pruebas: un equipo con 4 CPU y 8 GB con todo el stack (25 contenedores, observabilidad incluida). El clúster kind se apagó antes de empezar.

---

## 1. Qué se hizo, paso por paso

| Paso | Qué se hizo | Dónde |
|---|---|---|
| 7.1 Caos | Script que apaga un componente a la vez (claims, notification, customer, quotation, policy, identity, RabbitMQ, Redis, MongoDB de policy, WhatsApp, JSON.pe), comprueba la degradación planificada por el gateway y la recuperación sin pérdida de eventos. Además un caso de 2 réplicas y uno de identity caído 6 minutos | `infra/operacion/caos.sh` |
| 7.2 Alertas | Nueva alerta `ServicioNoListo` (readiness) con un blackbox exporter; `DlqConMensajes` cubre las DLQ de todos los servicios (métrica de RabbitMQ); `ErroresServidorAltos` ya no cuenta `/actuator`. Paneles de readiness y DLQ en Grafana | `infra/observability/` |
| 7.3 Reconciliación | Script que compara cada proyección con su fuente (7 comparaciones, por id y campos) y sale con 1 si hay diferencias, para programarlo cada noche | `infra/operacion/reconciliar.sh` |
| 7.4 Carga | Script k6 con tres escenarios (lecturas, emisión, pico sobre el límite del gateway) y ajustes a partir de lo medido (sección 3.4) | `infra/carga/carga.js` |
| 7.5 Seguridad | Revisión de S1–S11 y A1–A6 sobre el sistema final (sección 5); cabeceras de seguridad y usuario sin root en el frontend (S11); perfil `tls` con HTTPS local (S6) | `frontend/nginx.conf.template`, `infra/tls/Caddyfile` |
| 7.6 Contratos en CI | Productores validan sus eventos contra JSON Schema; consumidores aceptan un ejemplo de cada evento; los controladores se comparan con el OpenAPI. Obligatorias en CI; `k8s/` se valida con kubeconform | `contracts/events/ejemplos/`, `*/contratos/`, `.github/workflows/` |
| 7.7 Respaldos y DLQ | Respaldo de las 6 bases con manifiesto (SHA-256 y conteos) y prueba de restauración en un MongoDB temporal; reproceso de cualquier DLQ con un shovel (plugin activo de forma permanente) | `infra/mongo/respaldar.sh`, `probar-restauracion.sh`, `infra/rabbitmq/` |
| 7.8 Documentación operativa | Guía: levantar, leer el tablero, qué hacer ante cada alerta, reconciliación, degradación, DLQ, respaldos | [o_GUIA-OPERACION.md](o_GUIA-OPERACION.md) |
| 7.9 Auditoría | **Se eliminó** la cola `andina.policy.audit.queue`: la declaraba el monolito, nunca tuvo consumidor y ya no recibía nada. Si hace falta auditoría, será un consumidor propio de `andina.events` | Sección 6 |

Pendientes de la fase 6 que se cerraron aquí:

- DLX heredada: la cola de pólizas de notification pasa a `notification.policy.events` con DLX `andina.events.dlx`, y se borraron la cola vieja, su DLQ y `andina.insurance.events.dlx`.
- Ventana de `claim_ref` en la renovación.
- Ids que no son UUID.
- RabbitMQ en Kubernetes: pasa a `StatefulSet` con volumen.

## 2. Defectos encontrados y corregidos

| # | Defecto | Cómo se encontró | Corrección |
|---|---|---|---|
| 1 | **Con Redis caído, el gateway no respondía** (más de 70 s en todas las rutas). Lettuce guardaba los comandos esperando reconectar y los comandos reactivos (rate limiter, lista de revocación) no tenían timeout | Caos, caso Redis | `RedisSinBloqueo`: sin conexión el comando se rechaza al instante; con conexión lenta, timeout de 2 s. El rate limiter y la revocación dejan pasar (la firma del JWT se sigue validando). Ahora: 200 en 0,1–0,4 s con Redis caído |
| 2 | **El relay del Outbox no admitía varias réplicas** (su propio comentario lo decía): dos réplicas podían publicar el mismo evento a la vez o fuera de orden | Criterio "2 réplicas" | Turno en MongoDB (`outbox_lock`, lease de 15 s con findAndModify atómico) en customer, claims, quotation y policy |
| 3 | **Copias huérfanas tras el corte:** 12 pólizas en `policy_ref` (claims), 2 siniestros en `claim_ref` y 12 cotizaciones en `accepted_quotes` (policy) de las pruebas de la saga. Al repetir las migraciones con `--drop` se borraron de su fuente, pero no de las copias en otros servicios | Reconciliación | Borradas, comprobando antes que la cantidad coincidiera con la reportada. Lección para futuras migraciones: limpiar también las proyecciones de otros servicios |
| 4 | **9 contactos con teléfono viejo** en notification: la migración reinició la versión del cliente y el backfill del corte (versión 2) se descartó porque la copia ya tenía otra versión 2 de las pruebas de la fase 1 | Reconciliación | Se borraron esas 9 copias y se reenvió desde customer (backfill). El procedimiento queda en la guía |
| 5 | **`quote.accepted` sin zona horaria** (`2026-10-12T10:00:00`); el contrato pide RFC 3339 | Prueba de contrato del productor | quotation publica en UTC con zona (`...Z`); policy lo sigue leyendo |
| 6 | **Contrato OpenAPI de identity desactualizado:** faltaba `PATCH /api/auth/usuarios/{username}/estado` | Prueba de contrato de API | Contrato regenerado (cambio compatible) |
| 7 | **Primera petición tras reiniciar > 5 s** (límite del gateway) → 503, aunque el servicio figuraba sano y la operación se completaba | Caos y carga | `Calentamiento` al arrancar (ping a MongoDB y descarga del JWKS) antes de declararse listo: primera lectura de 5,7 s a 1,4 s |
| 8 | **Healthchecks de MongoDB y RabbitMQ cada 5 s** lanzaban `mongosh`/Erlang y se solapaban: las bases usaban 40–120 % de CPU en reposo | Carga (CPU por contenedor) | Cada 5 s solo mientras arrancan (`start_interval`), luego cada 30 s: 3–8 % en reposo |
| 9 | `quotationCB` esperaba 6 s pero el timeout HTTP global del gateway (5 s) cortaba antes | Análisis de un 503 | `response-timeout: 6000` en la ruta de quotation |
| 10 | La renovación podía no ver un siniestro recién llegado: solo se contaban los mensajes *listos* de la cola; con prefetch 250 cientos podían estar entregados y sin aplicar | Análisis (fase 6, sección 5) | Prefetch 1 en policy y un contador de eventos de siniestros en proceso (`SiniestrosEnProceso`, antes de los reintentos) |
| 11 | Un id que no es UUID, un JSON roto o una ruta inexistente → 500 | Fase 6 | 400 `SOLICITUD_MAL_FORMADA`, 404 y 405 en los 5 servicios con API |
| 12 | **El turno del Outbox podía vencer a mitad de una pasada:** se renovaba solo al empezar; un lote de 100 eventos con confirmaciones lentas podía pasar los 15 s y otra réplica publicaría a la vez (duplicados o fuera de orden) | Revisión externa (Codex, sección 9) | El turno se renueva antes de cada evento y la pasada se corta si se pierde; el servicio no arranca si el turno dura menos del doble de la espera de confirmación. Prueba unitaria y caso de caos `relay-lote` |

Dos más, de la verificación:

- **`ErroresServidorAltos`** se disparaba en todos los servicios durante una caída, porque contaba los 503 de readiness. Ahora excluye `/actuator`.
- **RabbitMQ en Kubernetes** perdía colas y mensajes al reiniciar (`emptyDir` y nombre de Pod aleatorio), el mismo defecto que se corrigió en Compose en la fase 6.

## 3. Verificación

### 3.1 Pruebas automáticas

| Módulo | Pruebas | Nuevas en la fase 7 |
|---|---|---|
| gateway | 1 | — |
| identity-service | 62 | contrato de API, consumidor de `customer.*`, 400/404 |
| notification-service | 30 | consumidor de `customer.*` y `policy.issued` |
| customer-service | 43 | turno del relay (también a mitad de lote), `customer.registered` contra su esquema, contrato de API, 400 |
| claims-service | 33 | turno, consumidor de `policy.*` (4 eventos), contrato de API, 400 |
| quotation-service | 46 | turno, `quote.accepted` con zona, consumidores, contrato de API, 400 |
| policy-service | 49 | turno, `SiniestrosEnProceso`, `policy.renewed`, consumidores, contrato de API, 400/404 |

Todas pasan con `CONTRATOS_OBLIGATORIOS=true` (como en CI). `k8s/`: 39 recursos válidos con kubeconform.

### 3.2 Caos (`bash infra/operacion/caos.sh`)

| Componente caído | Resultado |
|---|---|
| claims-service | Siniestros 503 (fallback); emitir 201; `policy.issued` espera en la cola y claims lo aplica al volver |
| notification-service | Emitir 201; el evento espera; el WhatsApp sale al volver |
| customer-service | Clientes 503; cotizar un cliente conocido 201 (copias en quotation) |
| quotation-service | Cotizaciones 503; emitir una cotización ya aceptada 201; al volver la marca EMITIDA |
| policy-service | Pólizas 503; cotizar y aceptar funcionan; `quote.accepted` llega a `accepted_quotes` al volver |
| identity-service | Login 503; los tokens vigentes siguen dando 200, **también tras 6 minutos** (más que la caché de 5 min de las claves) |
| **RabbitMQ durante una emisión** | Emitir 201; el evento queda PENDING en el Outbox; al volver se publica y llega a claims, quotation y al WhatsApp: **ningún evento perdido** |
| Redis | Lecturas 200 (tras la corrección 1); login 200 |
| MongoDB de policy | Pólizas 503 y policy "no listo"; clientes y cotizaciones siguen en 200; se recupera solo |
| WhatsApp | El circuito se abre, el consumo se pausa, **0 mensajes en la DLQ**; al volver salen los retenidos |
| JSON.pe | Placa en caché: 200; placa nueva: 200 con `SIN_DATOS` ("complete el registro manualmente") |
| 2 réplicas de policy | Las dos emiten (5 y 5); cada `policy.issued` llega **exactamente una vez** a claims; al caer la dueña del turno la otra lo toma y publica |
| Lote acumulado con 2 réplicas (`relay-lote`) | 20 emisiones con RabbitMQ caído (repartidas entre las dos réplicas) dejan 20 eventos en el Outbox; al volver RabbitMQ las dos compiten por publicarlos y los 20 llegan **exactamente una vez** a claims |

Dos límites que se ven y quedan documentados en la guía:

- **Arranque con identity caído:** un servicio que se reinicia mientras identity está caído no puede validar tokens (no tiene las claves) hasta que identity vuelve.
- **Reparto entre réplicas en Compose:** el gateway no reparte, porque reutiliza la conexión abierta. En Kubernetes reparte el Service.

### 3.3 Reconciliación, respaldos y DLQ

- **Reconciliación:** la primera corrida encontró los defectos 3 y 4. Tras corregirlos, y después de todas las pruebas de caos y de carga (284 pólizas y 298 cotizaciones aceptadas), las **7 comparaciones coinciden**, las DLQ están en 0 y no hay eventos pendientes en ningún Outbox.
- **Respaldo y restauración:** las 6 bases se respaldaron y se restauraron en un MongoDB temporal: 22 colecciones con los mismos conteos. Alterando un conteo del manifiesto, la prueba lo detecta.
- **Reproceso de DLQ:** un evento válido puesto en `identity.customer.events.dlq` volvió a la cola con el script, fue consumido e identity lo descartó por versión antigua (idempotencia). La DLQ quedó en 0.

### 3.4 Carga (`infra/carga/carga.js`)

Medido en el equipo de 4 CPU, con el generador de carga en el mismo equipo:

| Corrida | Lecturas (20/s) p95 | Emisión (2/s) p95 | 5xx |
|---|---|---|---|
| Con un proceso ajeno usando 2,5 CPU | 3,96 s | 6,38 s | 2 |
| Equipo sin ese proceso | **1,70 s** | **1,79 s** | **0** |
| Con 300 VUs en el pico | 7,0 s | 7,4 s | 224 (503 de timeout) |

Conclusiones:

- **Sin sobrecarga no hay errores**, y la emisión cumple su objetivo (p95 < 3 s). Las lecturas quedan por encima del objetivo de 1 s en este equipo. El gateway suma unos 350–400 ms sobre los servicios (entre 80 y 240 ms en promedio) por las dos consultas a Redis por petición y la CPU compartida.
- **La capacidad real de este equipo (~35 peticiones/s en una ruta) es menor que el límite del gateway por IP (50/s).** Por eso el exceso termina en 503 controlados (con `Retry-After`) y no en 429; el limitador funciona (una ráfaga de 400 dio 13 respuestas 429). En producción, el límite debe fijarse por entorno según la capacidad medida.
- **Nunca hubo 500 ni pérdida de datos:** el índice único impidió duplicados (una emisión repetida dio 422 `COTIZACION_YA_EMITIDA`) y la reconciliación posterior dio todo en orden.

**Ajustes hechos por lo medido:**

- healthchecks (defecto 8);
- calentamiento (defecto 7);
- timeout de quotation (defecto 9);
- prefetch 1 en policy (defecto 10);
- timeout de Redis de 2 s: con 500 ms habría fallado abierto con la CPU saturada.

**Nota sobre el entorno:** durante las pruebas se vieron procesos ajenos a este trabajo sobre el mismo stack: un contenedor de Maven y consultas `mongosh` a la base de policy. Explican la diferencia entre la primera corrida y las demás.

## 4. Criterios de "terminado" de la migración (propuesta, sección 11)

| Criterio | Estado |
|---|---|
| Cada servicio con su base; ninguna consulta cruza | ✅ Un MongoDB por servicio, en contenedores y redes separados; cada usuario solo tiene permiso sobre su base |
| notification sin acceso a `clientes`; solo su proyección | ✅ Desde la fase 1 |
| Outbox en todo productor, inbox o versión en todo consumidor; apagar RabbitMQ en una emisión no pierde eventos | ✅ Verificado (sección 3.2) |
| Toda llamada síncrona con timeout, circuit breaker y fallback, comprobado apagando el destino | ✅ Gateway → servicios, quotation → customer, customer → JSON.pe, notification → WhatsApp (sección 3.2) |
| Con el circuito de WhatsApp abierto, nada llega a la DLQ | ✅ Verificado |
| Funciona con 2 réplicas de cada servicio | ✅ Estado en Redis/MongoDB y turno del relay; verificado con policy (el de más riesgo: saga e índice único). Los demás usan el mismo relay y no guardan estado en memoria |
| JWT RS256; solo identity tiene la clave privada | ✅ Desde la fase 2 |
| Solo gateway y frontend publican puertos | ✅ (el perfil `tls` publica esos mismos dos, cifrados) |
| Pruebas de contrato para cada API y evento en CI | ✅ Sección 1, paso 7.6 |
| "Emitir póliza" se sigue con un solo `correlationId` en logs y trazas | ✅ Desde el cierre de la fase 6 |
| S1–S11 y A1–A6 resueltos o con decisión documentada | ✅ Sección 5 |

## 5. Revisión de seguridad y de arquitectura (hallazgos del análisis inicial)

| Hallazgo | Estado en el sistema final |
|---|---|
| S1 Registro con elección de rol | ✅ identity solo respeta el rol si quien registra es ADMIN |
| S2 Autorización por rol y propietario | ✅ En cada servicio, con pruebas `ControlAcceso*` |
| S3 Secretos en el repositorio | ✅ Solo variables de entorno (`.env` no versionado); JWT RS256 con clave en volumen. Pendiente fuera del código: rotar los tokens de JSON.pe que estuvieron versionados |
| S4 MongoDB sin autenticación y expuesto | ✅ Autenticación, un usuario por base, sin puertos en el host |
| S5 RabbitMQ con credenciales por defecto y consola publicada | 🟡 Consola y puertos cerrados; contraseña por `.env` (el valor por defecto solo sirve en local). **Decisión:** todos los servicios comparten un usuario; permisos por servicio (cada uno solo sus colas y exchanges) y TLS quedan para producción |
| S6 Tráfico sin cifrar | ✅ Kubernetes: TLS en el Ingress. Local: perfil `tls` (Caddy, HTTPS en 8443/8444 con HSTS). El tráfico interno entre contenedores sigue sin cifrar (misma máquina o red privada del clúster) |
| S7 ADMIN demo con contraseña conocida | ✅ Datos demo solo con `APP_DEMO_DATA_ENABLED=true` (Compose local); por defecto apagado |
| S8 Sesión débil | 🟡 Revocación en Redis (logout y usuarios desactivados). **Decisión:** el token sigue en `localStorage` con 8 h de vida; cookie `HttpOnly` y tokens cortos con renovación quedan como evolución. La CSP nueva (S11) reduce el riesgo de XSS |
| S9 Sin límite de intentos | ✅ Login y MFA: 1 por segundo por IP en el gateway |
| S10 Swagger público | ✅ Apagado por defecto; el gateway no enruta `/v3/api-docs` |
| S11 Cabeceras de seguridad | ✅ CSP, `X-Frame-Options`, `nosniff`, `Referrer-Policy`, `Permissions-Policy` en el frontend; el gateway agrega las de Spring Security |
| Contenedores como root | ✅ Todas las imágenes corren sin root (frontend incluido desde esta fase) |
| A1 Sin Outbox | ✅ |
| A2 Base compartida | ✅ |
| A3 Estado en memoria | ✅ Redis/MongoDB y turno del relay |
| A4 Cola de auditoría sin consumidor | ✅ Eliminada (7.9) |
| A5 Puntos únicos de fallo | 🟡 Respaldos con prueba de restauración, alertas y degradación verificada. **Decisión:** MongoDB y RabbitMQ siguen con un nodo; la replicación de 3 nodos queda como evolución si el negocio la exige |
| A6 Dependencia de terceros | ✅ Circuit breakers, caché de placas, retención de WhatsApp, alerta de DLQ y reproceso |

## 6. Decisiones y lo que no se hizo

| Tema | Decisión |
|---|---|
| Consumidor de auditoría (7.9) | Se eliminó la cola en lugar de implementar un consumidor: nadie la usaba. Los 6 mensajes que tenía quedaron en el directorio de un nodo anterior de RabbitMQ (fase 6, sección 9.4); no se recuperan |
| Kubernetes | Manifiestos validados (kubeconform) y RabbitMQ con volumen; **no se aplicaron** al clúster: el clúster kind local corría el despliegue de la fase 0 y saturaba el equipo, y se apagó |
| Alertmanager | Fuera del código (pendiente): las alertas se ven en Prometheus y Grafana |
| Permisos de RabbitMQ por servicio, token en cookie, clústeres de 3 nodos | Sección 5 |
| Límite de tasa del gateway | Se deja en 50/s por IP; fijarlo por entorno según la capacidad medida (sección 3.4) |
| Reparto entre réplicas en Compose | Compose no balancea (el gateway reutiliza la conexión); en Kubernetes reparte el Service |

## 7. Impacto en el monolito

Ninguno: ya estaba retirado desde la fase 6. Se usó su respaldo (`respaldos/monolito-…`) para confirmar el valor correcto de los contactos divergentes (defecto 4).

## 8. Archivos

| Área | Archivos |
|---|---|
| Servicios | `OutboxRelay`, `MongoOutboxLease` y `OutboxConfig` (customer, claims, quotation, policy); `Calentamiento` (los mismos); `GlobalExceptionHandler` (los 5 con API); policy: `SiniestrosEnProceso`, `RabbitMqConfiguration`, `UseCaseConfig`, prefetch; quotation: `IntegrationEventMapper`/`IntegrationEventMessage` (fechas con zona); notification: cola `notification.policy.events` |
| Gateway | `RedisSinBloqueo`, `GatewayConfig`, `RevocacionesRedis`, timeout de quotation en `application.yml` |
| Pruebas | `*/contratos/Contratos.java` y `ContratoApiTest`, `ContratoEventosConsumidosTest`, pruebas de relay, 400/404 y `SiniestrosEnProcesoTest`; `pom.xml` (json-schema-validator) |
| Contratos | `contracts/events/ejemplos/`, `contracts/openapi/identity-service.json`, `contracts/README.md` |
| Frontend | `nginx.conf.template`, `Dockerfile` (nginx sin root, puerto 8080), `docker-compose.yml` |
| Infraestructura | `infra/operacion/` (caos, reconciliación), `infra/mongo/` (respaldo, restauración), `infra/rabbitmq/` (plugins, reproceso), `infra/carga/`, `infra/tls/`, `infra/observability/` (blackbox, alertas, tablero), `Arquitectura-Clean/docker-compose.yml` (healthchecks, plugins, TLS, frontend) |
| Kubernetes y CI | `k8s/31-rabbitmq.yaml` (StatefulSet), `k8s/README.md`, `.github/workflows/k8s.yml` y `CONTRATOS_OBLIGATORIOS` en los workflows |
| Documentación | este documento, [o_GUIA-OPERACION.md](o_GUIA-OPERACION.md), `d_RUTA…` (fase 7), `DOCKER-EJECUCION.md`, `CLAUDE.md`, `services/notification-service/README.md` |

## 9. Revisión externa (Codex)

Una revisión con Codex, hecha mientras la fase estaba en curso (antes de las pruebas, los documentos y los commits), señaló estos puntos:

| Punto | Estado |
|---|---|
| El turno del Outbox puede vencer durante un lote lento | **Corregido** (defecto 12): se renueva antes de cada evento; validación al arrancar; prueba unitaria y caso `relay-lote` en vivo |
| Documentos de las fases 3 a 6 con estados "pendiente" que ya no lo son, y 6.10/6.11 marcados "No se hizo" | **Corregido:** cada fila indica cómo quedó resuelta, sin borrar lo que se decidió en su momento |
| `reprocesar-dlq.sh` indicaba `deploy/rabbitmq` en Kubernetes | **Corregido:** RabbitMQ es un StatefulSet; el comando es `kubectl exec -n andina-seguros rabbitmq-0 -- …` |
| `caos.sh` no corre por defecto los casos de réplicas | **Corregido:** `bash infra/operacion/caos.sh todos` suma `replicas`, `relay-lote` e `identity-larga` |
| Correr caos, carga, reconciliación y restauración; confirmar que las pruebas pasan | Ya hecho (sección 3) |
| `o_GUIA` enlazaba a un informe que no existía; `nginx.conf` borrado sin su plantilla | Ya resuelto: este documento existe y los dos archivos entraron en el mismo commit |
| Qué hacer con la cola de auditoría | Ya decidido (paso 7.9) |
| **Siniestro recién registrado y renovación** con varias réplicas o con el evento todavía en el Outbox de claims | **Abierto, pendiente de decisión.** Opciones: (a) la evaluación queda "provisional" y se confirma cuando `claim_ref` alcanza la versión de claims; (b) policy consulta a claims en ese momento (llamada síncrona con circuit breaker, y "pendientes" si no responde) |
| **Revocación de tokens con Redis caído** (se deja pasar: un token revocado vale hasta que vence, como mucho 8 h) | **Abierto, pendiente de aprobación.** Alternativas: aceptar el riesgo, o acortar la vida del token para acotarlo |
| Aplicar Kubernetes, rotar tokens de JSON.pe, Alertmanager, login social real | Pendientes ya anotados (sección 6 y `CLAUDE.md`) |
