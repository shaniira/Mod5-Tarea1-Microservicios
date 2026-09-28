# Implementación de policy-service (fase 6 de la migración a microservicios)

Este documento registra lo que se implementó en la fase 6 de la [ruta de implementación](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#10-fase-6--policy-service-y-apagado-del-monolito), las decisiones, el impacto en el monolito y la verificación contra el stack de Docker.

**Resultado:** pólizas, la saga de emisión y las renovaciones viven en `services/policy-service`, con base propia (`policy_db`), las proyecciones `accepted_quotes` y `claim_ref`, y `policy.*` por Outbox. La saga completa (cotizar → aceptar → emitir → notificar → siniestro → renovar) funcionó de extremo a extremo **solo con eventos y proyecciones**, y la doble emisión en paralelo nunca generó más de una póliza. El 2026-09-27 se hizo **el corte de las fases 3 a 6** (sección 7): el gateway envía todo el negocio a los microservicios y el monolito quedó sin tráfico, con su código sin modificar. Ese mismo día **se cerró la fase** (sección 9): el monolito salió del Compose con su base respaldada (6.10), se retiró el exchange heredado (6.11) y los servicios nuevos entraron en Prometheus, Grafana y Jaeger. Los 4 criterios de salida se cumplen.

---

## 1. Qué se hizo, paso por paso

| Paso de la ruta | Implementación | Estado |
|---|---|---|
| 6.1 Crear policy-service con la plantilla | Estructura Clean con 8 reglas ArchUnit. Se trajeron `poliza/*`, `renovacion/*`, `PropuestaRenovacion`, `CalculadorPrimaRenovacion`, `PoliticaVariacionPrima` y `EvaluadorRenovacion` (este último recibe referencias de siniestros en lugar de siniestros completos). `Poliza` suma la versión del agregado. Mismas rutas, reglas de acceso, respuestas y errores que el monolito | ✅ |
| 6.2 Proyecciones `accepted_quotes` y `open_claims_by_policy` | `accepted_quotes` (cola `policy.quote.events`, de `quote.accepted.v1`) y **`claim_ref`** (cola `policy.claim.events`, de `claim.*`), cada cola con su DLQ. En lugar de un contador se guarda una entrada por siniestro (abierto, responsable, versión): la renovación necesita saber si hay abiertos, cuántos fueron responsabilidad del asegurado y cuántos hubo, y con una entrada por siniestro los eventos repetidos o desordenados no descuadran la cuenta. Carga inicial con `migracion/migrar-polizas.sh` | ✅ |
| 6.3 Saga de emisión e índice único por `quoteId` | Emitir valida contra `accepted_quotes` (no consulta a quotation), crea la póliza VIGENTE y publica `policy.issued.v1` en la misma transacción. Índice **único y parcial** por `cotizacionId` (las pólizas renovadas no tienen cotización). Ante un conflicto de escritura (`WriteConflict`) se reintenta hasta 5 veces con espera aleatoria; en el reintento la validación ve la póliza ganadora | ✅ |
| 6.4 Compensación | Si la cotización ya tiene póliza o ya venció: 422 y `policy.issuance-rejected.v1`. quotation-service la consume, deja la cotización como está (ACEPTADA, o EMITIDA si ganó otra solicitud) y registra el motivo en `rechazos_emision` | ✅ |
| 6.5 Renovaciones dentro del servicio | Evaluar, aprobar, rechazar y generar la póliza renovada. El historial sale de `claim_ref`; si esa proyección no tiene la carga inicial, se responde `SINIESTROS_PENDIENTES` ("intenta más tarde") en lugar de arriesgarse a renovar con un siniestro abierto | ✅ |
| 6.6 Publicar `policy.*` (Outbox) | `policy.issued.v1` (mismos campos que publicaba el backend, más los opcionales), `policy.renewed.v1` y `policy.issuance-rejected.v1`. `policy.expired.v1` y `policy.cancelled.v1` quedaron definidos en `contracts/` pero sin productor: ni el monolito ni policy-service tienen una operación que venza o cancele pólizas | ✅ (vencer y cancelar: sin operación en el sistema) |
| 6.7 Migrar `polizas` y `propuestas_renovacion`; interruptor único | `migrar-polizas.sh` copia y compara (huella SHA-256). Interruptor `POLICY_EVENTS_PUBLISH_ENABLED` (en Compose: `false`) | ✅ (interruptor apagado hasta el corte) |
| 6.8 Rutas en el gateway | `/api/polizas/**` y `/api/renovaciones/**` a policy-service, con las de siniestros antes (sección 7) | ✅ |
| 6.9 "Mi cuenta" pasa a policy-service | Endpoint `GET /api/mi-cuenta/polizas` (solo CLIENTE, con el `customerId` del token) y composición en el gateway (`MiCuentaController`) con customer-service, con respuesta parcial | ✅ |
| 6.10 Apagar el monolito | Primero se decidió mantenerlo congelado y corriendo (sección 3.2); al cerrar la fase se retiró con su base respaldada | ✅ Hecho (sección 9.1) |
| 6.11 Retirar `andina.insurance.events` | Se retiró cuando el monolito dejó de publicar ahí; su DLX, en la fase 7 | ✅ Hecho (sección 9.2) |

Cambios en servicios anteriores para cerrar la saga:

| Servicio | Cambio |
|---|---|
| claims-service | Consume `policy.renewed.v1` (la anterior pasa a RENOVADA y la nueva queda VIGENTE), `policy.expired.v1` y `policy.cancelled.v1`, con versión. `policy_ref` ya no depende de repetir el script de resincronización |
| quotation-service | Consume `policy.issuance-rejected.v1` y registra el motivo (colección `rechazos_emision`, sin cambiar la respuesta de la API) |
| contratos | `policy.issued.v1` con campos opcionales nuevos (vehículo, prima, vigencia, estado); nuevos `policy.renewed.v1`, `policy.issuance-rejected.v1`, `policy.expired.v1` y `policy.cancelled.v1` |

## 2. Decisiones

| Decisión | Motivo |
|---|---|
| **`claim_ref` por siniestro en lugar de un contador** | La renovación usa tres datos (abiertos, responsables y total considerado) y los eventos pueden llegar repetidos o desordenados. Con una entrada por siniestro y su versión, la cuenta siempre es correcta |
| **Reintento ante `WriteConflict`** | En una transacción de MongoDB, dos inserciones simultáneas de la misma cotización no dan "clave duplicada" sino un conflicto transitorio, y MongoDB indica reintentar. Se detectó en la verificación (sección 4) |
| **Rechazar una cotización vencida** | El monolito no lo hacía; la ruta lo pide (paso 6.4). Es la única diferencia de comportamiento con el monolito |
| **"No aceptada" para una cotización que no está en `accepted_quotes`** | Sin consultar a quotation no se puede distinguir "no existe" de "no está aceptada" o "todavía no llegó el evento"; se responde el mismo error del monolito (`COTIZACION_NO_ACEPTADA`) |
| **Índice único parcial** | El índice del monolito sobre `cotizacionId` no es parcial: las pólizas renovadas, que no tienen cotización, chocarían entre sí. La migración no copia los índices del monolito |
| **policy-service publica en `andina.events`** | Es el exchange objetivo; todos los consumidores de `policy.issued.v1` ya escuchan los dos |
| **Nombres sin prefijo de la empresa** | Imagen `policy-service:1.0.0`, contenedores `policy-service` y `policy-mongodb`, volumen `policy_mongo_data`, red `policy_data_network` |

## 3. Impacto en el monolito

**No se modificó el código del monolito.** En `Arquitectura-Clean/docker-compose.yml` y `.env.example` solo se agregaron entradas nuevas. Detalle de todas las fases en [k_IMPACTO-EN-EL-MONOLITO.md](k_IMPACTO-EN-EL-MONOLITO.md).

### 3.1 Qué sigue haciendo el monolito

Nada. Desde el corte (sección 7) no recibe tráfico, y desde el cierre de la fase (sección 9) no arranca con el stack: queda en el perfil `monolito` del Compose, solo para una reversa.

| Funcionalidad | Antes del corte | Hoy |
|---|---|---|
| Rutas de la API (clientes, cotizaciones, pólizas, siniestros, renovaciones, "Mi cuenta") | Monolito | customer, quotation, policy y claims; "Mi cuenta" la compone el gateway |
| Publicar `customer.*` y `policy.issued.v1` | Monolito (`policy.issued` en `andina.insurance.events`) | customer-service y policy-service, en `andina.events` |

### 3.2 El corte (fases 3 a 6 juntas) y el apagado del monolito

Con las fases 3 a 6 implementadas, el corte puede hacerse de una vez y **sin modificar el monolito**, porque ya no queda ninguna función que dependa de él:

1. Detener la escritura en el monolito (ventana de mantenimiento) y repetir, en orden, `migrar-clientes.sh`, `migrar-siniestros.sh`, `migrar-cotizaciones.sh` y `migrar-polizas.sh`.
2. Descartar los eventos `PENDING` de pruebas que haya en los Outbox de los servicios nuevos.
3. Activar la publicación en los cuatro servicios: `CUSTOMER_EVENTS_PUBLISH_ENABLED`, `CLAIMS_EVENTS_PUBLISH_ENABLED`, `QUOTATION_EVENTS_PUBLISH_ENABLED` y `POLICY_EVENTS_PUBLISH_ENABLED` en `true`.
4. En el gateway, reemplazar la ruta `backend` por las rutas de cada servicio. Las de siniestros van **antes** que `/api/polizas/**`. `/api/mi-cuenta` pasa a ser una composición de customer-service (`/api/clientes/{customerId}`) y policy-service (`/api/mi-cuenta/polizas`), con respuesta parcial si uno no responde.
5. Ejecutar los backfill: `POST /api/clientes/eventos/reenvio` y `POST /api/siniestros/eventos/reenvio`.
6. **Paso 6.10:** retirar `backend` (y su dependencia del gateway) de `docker-compose.yml`. Respaldar `andina_clean_mongo_data` y conservarlo un tiempo antes de borrarlo. El código del monolito se conserva en el repositorio, sin cambios. *(Hecho: sección 9.)*
7. **Paso 6.11:** cuando nadie publique en `andina.insurance.events`, quitar los enlaces a ese exchange en notification, claims y quotation. *(Hecho: sección 9.)*

**Reversa (antes del paso 6):** volver la ruta `backend` al gateway y apagar los interruptores.

## 4. Verificación

Pruebas hechas el 2026-09-27 contra el stack de Compose, por la red interna (los servicios no están enrutados), con el token RS256 de identity-service. Para probar la saga, la publicación se activó **temporalmente** en quotation, claims y policy (customer-service no), con WhatsApp simulado. Al terminar se volvió a apagar.

| # | Criterio de salida (ruta, sección 10) | Cómo se probó | Resultado |
|---|---|---|---|
| 1 | Emitir y renovar pólizas funciona solo con eventos y proyecciones | Cotizar y aceptar en quotation → `quote.accepted.v1` llegó a `accepted_quotes`; emitir en policy → 201; `policy.issued.v1` dejó la cotización EMITIDA en quotation, la póliza VIGENTE en `policy_ref` de claims y el WhatsApp enviado por notification. Un siniestro abierto registrado en claims llegó a `claim_ref` y la evaluación respondió **422 `SINIESTROS_PENDIENTES`**; al liquidarlo, la evaluación dio 201 (REQUIERE_RECALCULO, prima 1525.50), se aprobó y se generó la póliza renovada; `policy.renewed.v1` dejó en claims la anterior RENOVADA (v2) y la nueva VIGENTE. Colas y DLQ en 0 | ✅ Cumple |
| 2 | Emitir la misma cotización dos veces en paralelo genera una sola póliza | 5 rondas de 3 solicitudes simultáneas por cotización: **exactamente 1 póliza en las 5**; las perdedoras respondieron 422 `COTIZACION_YA_EMITIDA` y quotation registró cada compensación. Ver el error corregido abajo | ✅ Cumple |
| 3 | El flujo cotizar → aceptar → emitir → WhatsApp con un solo `correlationId` visible en Grafana y Jaeger | Con el stack de observabilidad y sin el monolito (sección 9): el `correlationId` del flujo aparece en Loki/Grafana en 6 servicios, y la emisión es **una sola traza en Jaeger** (16 spans) que va del gateway a policy y, por RabbitMQ, a notification, quotation y claims | ✅ Cumple |
| 4 | El monolito ya no está en el Compose | `backend` y su MongoDB solo arrancan con `--profile monolito`; el stack normal no los levanta (sección 9) | ✅ Cumple |

Pruebas adicionales:

| Prueba | Resultado |
|---|---|
| Migración (`migrar-polizas.sh`) | 17 pólizas y 5 propuestas con la misma huella SHA-256; `claim_ref` con el siniestro abierto de la fase 4 |
| Paridad de lecturas | `GET /api/polizas` (17), `?estado=VIGENTE` (14), `/api/renovaciones` (5), una póliza y su historial: **idénticos** al monolito. "Mi cuenta" del cliente de prueba (`POL-CLI-TEST-001` y su renovación): mismas pólizas y renovaciones, elemento por elemento |
| Pruebas automáticas | policy-service 39 (8 ArchUnit, sincronización de claim_ref, saga con compensación y reintento, renovaciones con `claim_ref`, prima igual a la del monolito, formato de `policy.*`, acceso por rol y propietario, "Mi cuenta"); claims-service 25; quotation-service 37 |
| Error encontrado y corregido | En la primera prueba de doble emisión la solicitud perdedora respondió **500** (se creó una sola póliza, pero sin 422 ni compensación): MongoDB informa `WriteConflict` dentro de la transacción. Se agregó el reintento; con 3 intentos cortos todavía falló la primera ronda (servicio recién arrancado), y se subió a 5 con espera aleatoria |

## 5. Limitaciones conocidas

| Punto | Detalle |
|---|---|
| **Vencer y cancelar pólizas** | No existe la operación en el sistema; los contratos están listos para cuando exista |
| **Kubernetes** | Manifiestos escritos y validados con `--dry-run=server`; no aplicados. El clúster kind local todavía corre el despliegue de la fase 0 (gateway, **backend**, MongoDB, RabbitMQ, Redis) y carga mucho el equipo (sección 9) **Fase 7:** validados con kubeconform en CI; el clúster kind se apagó; siguen sin aplicarse |
| **Siniestro recién registrado y renovación** | La sincronización de `claim_ref` mira la carga inicial y los mensajes *listos* de `policy.claim.events`. No ve un evento que todavía está en el Outbox de claims ni uno ya entregado al consumidor y sin confirmar: una evaluación hecha en ese instante no ve el siniestro. Con el equipo cargado se vio una ventana de ~1,3 s (sección 9). Para la fase 7: contar también los mensajes sin confirmar, o que la evaluación quede provisional hasta que `claim_ref` esté al día **Fase 7:** se cuentan los mensajes sin confirmar (prefetch 1 y `SiniestrosEnProceso`); quedan abiertos el evento que sigue en el Outbox de claims y el que aplica otra réplica (`n_…`) |
| **Id que no es UUID** | `GET /api/polizas/abc` (y equivalentes en claims) responde 500 en lugar de 400. El monolito hacía lo mismo; queda para la fase 7 **Resuelto en la fase 7:** 400 `SOLICITUD_MAL_FORMADA` |
| **RabbitMQ en Kubernetes** | Usa `emptyDir`: al reiniciar el Pod pierde colas y mensajes. En Compose se corrigió el equivalente (sección 9); en Kubernetes queda para la fase 7 (StatefulSet con volumen) **Resuelto en la fase 7:** `StatefulSet` con volumen |

## 7. Corte de las fases 3 a 6 (2026-09-27)

Se ejecutó el corte descrito en la sección 3.2, **sin modificar el código del monolito**:

1. Se descartó el único evento de prueba pendiente (un `vehicle.registered` de customer-service), marcándolo `DESCARTADO`.
2. Se repitieron las cuatro migraciones (clientes, siniestros, cotizaciones, pólizas): todas con la misma huella que el monolito.
3. Se activó la publicación en los cuatro servicios (en Compose ahora es `true` por defecto) y el gateway pasó a enrutar `/api/clientes`, `/api/vehiculos`, `/api/cotizaciones`, `/api/tablas-tarifarias`, `/api/polizas/*/siniestros` (antes que pólizas), `/api/siniestros`, `/api/polizas` y `/api/renovaciones` a sus servicios, cada uno con su circuit breaker, timeout y *fallback*. `/api/mi-cuenta` lo compone el gateway (customer + policy).
4. Backfill por el gateway: 14 clientes, 14 vehículos y 1 siniestro publicados; DLQ en 0.

Verificación del corte:

| Prueba | Resultado |
|---|---|
| Paridad por el gateway frente al monolito (consultado directo por la red interna) | 10 consultas **idénticas**: clientes, vehículos, tablas, cotizaciones, pendientes de emisión, pólizas, renovaciones, una póliza, su historial y sus siniestros |
| "Mi cuenta" (composición) | Idéntica a la del monolito para el cliente de prueba; un ADMIN recibe 403; con policy-service caído responde el cliente y `seccionesNoDisponibles: ["polizas"]` |
| Flujo completo por el gateway | Alta de cliente y vehículo → cotizar → aceptar → emitir (la segunda emisión, 422) → cotización EMITIDA → siniestro abierto bloquea la renovación → liquidar → evaluar, aprobar y generar la renovada → póliza original RENOVADA. El WhatsApp salió al teléfono del cliente **nuevo** (prueba de que su `customer.registered` de customer-service llegó a notification) |
| El monolito ya no recibe negocio | El `correlationId` del flujo aparece en 6 servicios y en **0** líneas del backend |
| Proyecciones (criterio 3 de la fase 3) | 15 clientes en `customer_db` y 15 en `customer_contacts` (notification), `customer_email_index` (identity) y `customer_ref` (quotation) |
| Pruebas automáticas | customer 38, claims 25, quotation 37, policy 39, gateway 1 (contexto con las rutas nuevas) |

**Reversa:** apuntar `CUSTOMER_SERVICE_URL`, `CLAIMS_SERVICE_URL`, `QUOTATION_SERVICE_URL` y `POLICY_SERVICE_URL` del gateway a `http://backend:8080` y poner los `*_EVENTS_PUBLISH_ENABLED` en `false`. Las escrituras hechas después del corte quedan solo en los servicios nuevos: antes de revertir habría que copiarlas al monolito.

## 8. Revisión externa (Codex)

Una revisión con Codex, hecha mientras la fase estaba en curso, señaló 8 puntos. Se corrigieron los que eran defectos reales:

| Punto | Acción |
|---|---|
| `claim_ref` "sincronizada" solo por la carga inicial | Corregido: además, la cola `policy.claim.events` no debe tener eventos esperando; si no se puede comprobar, se trata como atrasada |
| `quote.accepted` sin `expiresAt` aceptado | Corregido: va a la DLQ y el dominio exige la fecha |
| La migración borraba los índices sin recrearlos | Corregido: el script recrea los índices (incluido el único parcial de cotización) |
| Publicación "mixta" | Era la activación temporal de la prueba de la saga; con el corte, los cuatro servicios publican |
| Emisión concurrente contra MongoDB real y flujo completo | Hechos (secciones 4 y 7) |
| Vencer/cancelar, Kubernetes, auditoría, commits | Documentados: sin operación en el sistema, manifiestos ya escritos, cola heredada sin consumidor (fase 7) y commits hechos al cerrar |

## 9. Cierre de la fase: retiro del monolito, exchange heredado y observabilidad (2026-09-27)

### 9.1 Paso 6.10 — monolito retirado

| Qué | Cómo |
|---|---|
| Respaldo de la base | `mongodump` de `andina_seguros_clean` en `respaldos/monolito-andina_seguros_clean-2026-09-27.archive.gz` (SHA-256 `0e8381b1…afda9792`). Se restauró en un MongoDB temporal: las 9 colecciones con los mismos conteos (clientes 14, cotizaciones 57, outbox 39, pólizas 17, propuestas 5, siniestros 1, tablas 3, usuarios 5, vehículos 14). `respaldos/` está en `.gitignore`: tiene datos de clientes |
| Compose | `backend` y `mongodb` pasan al perfil `monolito`: `docker compose up` ya no los levanta. Se detuvieron y eliminaron los contenedores; el volumen `andina_clean_mongo_data` **se conserva** |
| Gateway | Sin la ruta de reserva `backend`, su circuito `backendCB`, su *fallback* ni `BACKEND_SERVICE_URL`. Una ruta `/api/**` sin dueño responde 404. El gateway sale de `clean_network` (la red de la base del monolito) |
| Kubernetes | Los manifiestos 20-23 del backend pasan a `k8s/archivo-monolito/`; `BACKEND_SERVICE_URL` sale del ConfigMap del gateway y el Ingress deja de publicar el Swagger del monolito |
| Código del monolito | **Sin cambios**; queda archivado en `Arquitectura-Clean/` |

**Reversa, durante el periodo de seguridad:** `docker compose --profile monolito up -d mongodb backend` y apuntar la URL del servicio en el gateway a `http://backend:8080`. Las escrituras posteriores al corte habría que copiarlas antes al monolito.

### 9.2 Paso 6.11 — exchange heredado retirado

- notification, claims y quotation ya no declaran ni enlazan `andina.insurance.events`. La propiedad correspondiente se eliminó de los tres servicios: `legacy-exchange` en notification e `insurance-exchange` en claims y quotation.
- Con el monolito apagado nadie publicaba ahí; se borró el exchange (y con él sus 4 enlaces). `policy.*` llega solo por `andina.events`.
- **Se conserva `andina.insurance.events.dlx`:** es la DLX de `andina.policy.notification.queue`, y RabbitMQ no permite cambiar los argumentos de una cola existente. Renombrarla implica recrear la cola vacía (fase 7).
- La cola `andina.policy.audit.queue` la declaraba el monolito y no tenía consumidor. Ver 9.4.

### 9.3 Observabilidad de los servicios nuevos (criterio 3)

- `docker-compose.observability.yml`: customer, claims, quotation y policy exportan trazas al Collector; RabbitMQ entra en la red de observabilidad; el backend sale.
- `prometheus.yml`: jobs de los 4 servicios y `rabbitmq` (plugin `rabbitmq_prometheus`, familia `queue_coarse_metrics`); sin job `backend`.
- La alerta `DlqConMensajes` y el panel "Mensajes en DLQ" usan `rabbitmq_detailed_queue_messages{queue=~".+[.]dlq"}`: cubren las 8 DLQ de todos los servicios, no solo las de notification.
- Los servicios ya tenían métricas, trazas (con `traceparent` guardado en el Outbox) y logs JSON: no hizo falta cambiar su código.

Verificación (stack de observabilidad levantado, sin monolito, WhatsApp simulado y luego restaurado):

| Prueba | Resultado |
|---|---|
| Prometheus | 9 *targets* `up`: gateway, identity, notification, customer, claims, quotation, policy, rabbitmq y el propio Prometheus. Las 6 reglas cargadas sin error; las 8 DLQ medidas (0 mensajes) |
| Flujo completo por el gateway (`fase6-1790542992`) | Cliente → vehículo → cotizar → aceptar → emitir `POL-2026-02EA10F9` → segunda emisión 422 → cotización EMITIDA → siniestro → evaluación 422 `SINIESTROS_PENDIENTES` → liquidar → evaluar, aprobar y generar `POL-REN-2027-72DC140B` → original RENOVADA. WhatsApp enviado (simulado) |
| Grafana / Loki | `{service=~".+"} \|= "fase6-1790542992"`: gateway 15 líneas, quotation 4, policy 3, notification 3, claims 2, identity 1 |
| Jaeger | La emisión es una sola traza de 16 spans: `api-gateway` → `policy-service` → RabbitMQ → `notification-service`, `quotation-service` y `claims-service`. Aceptar la cotización: una traza `api-gateway` → `quotation-service` → `policy-service`. Jaeger lista los 7 servicios |
| Evento después de los cambios de RabbitMQ | Alta de un cliente: `customer.registered` aplicado en notification, quotation e identity |
| Ruta sin dueño | `GET /api/no-existe` → 404; `/api/mi-cuenta` con ADMIN → 403; `/api/polizas` → 200 |
| Pruebas automáticas | gateway 1, notification 27, claims 25, quotation 37 (todas pasan) |

### 9.4 Problemas encontrados

| Problema | Qué pasó | Acción |
|---|---|---|
| **RabbitMQ sin nombre de host fijo** | Al recrear el contenedor para sumarlo a la red de observabilidad, arrancó un nodo nuevo (`rabbit@<id del contenedor>`) con estado vacío. Ya había otro nodo huérfano de una recreación anterior. Los servicios redeclararon sus colas, que estaban vacías, así que **no se perdió ningún evento de negocio**. La cola `andina.policy.audit.queue`, que nadie más declara, quedó con sus **6 mensajes** en el directorio del nodo anterior (`/var/lib/rabbitmq/mnesia/rabbit@dbed063fcf4d`, en el volumen) | `hostname: rabbitmq` en el Compose. Probado: un mensaje persistente en una cola de prueba sobrevivió a `--force-recreate`. Esos 6 mensajes eran copias de `policy.issued` sin consumidor; se decide qué hacer con ellos en la fase 7 (auditoría) |
| **Healthcheck de RabbitMQ con 5 s** | Con el equipo cargado, `rabbitmq-diagnostics ping` tardaba 12 s: RabbitMQ figuraba *unhealthy* y bloqueaba el arranque de quien depende de él | `timeout: 15s`, como en MongoDB |
| **Equipo sobrecargado** | El clúster kind (`andina-seguros-control-plane`) sigue corriendo el despliegue de la fase 0, con *load average* ~40 y usando swap. Con el stack recién recreado, la primera emisión tardó 5,1 s (límite del gateway 5 s → 503, aunque la póliza se creó) y un registro de siniestro superó los 3 s de claims | No se tocó el clúster. En caliente el flujo pasó completo. Queda pendiente decidir si se apaga el despliegue viejo o se aplican los manifiestos nuevos |
| **Id que no es UUID → 500** | Visto en el primer intento (ids vacíos) | Igual que el monolito; queda en la fase 7 (sección 5) |

## 6. Archivos

| Área | Archivos |
|---|---|
| Servicio | `services/policy-service/**` |
| Migración | `services/policy-service/migracion/migrar-polizas.sh` |
| Cambios en otros servicios | claims-service (`PolicyEventsListener`, `ActualizarEstadoPolizaUseCase`, `MongoPolizaRefRepository`, colas), quotation-service (`PolicyEventsListener`, `RegistrarRechazoEmisionUseCase`, `MongoRechazoEmisionRepository`, colas) |
| Contratos | `contracts/events/policy.issued.v1` (campos opcionales), `policy.renewed.v1`, `policy.issuance-rejected.v1`, `policy.expired.v1`, `policy.cancelled.v1`; `contracts/README.md` |
| Infraestructura | `Arquitectura-Clean/docker-compose.yml`, `Arquitectura-Clean/.env.example` |
| CI | `.github/workflows/policy-service.yml` |
| Cierre (sección 9) | `gateway/src/main/resources/application.yml`, `FallbackController`; `RabbitMqConfiguration` y `RabbitMqProperties` de notification, claims y quotation (y sus `application.yml`); `Arquitectura-Clean/docker-compose.yml` (perfil `monolito`, `hostname` de RabbitMQ); `infra/observability/**`; `k8s/11`, `k8s/40`, `k8s/archivo-monolito/`, `k8s/README.md`; `DOCKER-EJECUCION.md`; `.gitignore` (`respaldos/`) |
