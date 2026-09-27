# Implementación de policy-service (fase 6 de la migración a microservicios)

Este documento registra lo que se implementó en la fase 6 de la [ruta de implementación](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#10-fase-6--policy-service-y-apagado-del-monolito), las decisiones, el impacto en el monolito y la verificación contra el stack de Docker.

**Resultado:** pólizas, la saga de emisión y las renovaciones viven en `services/policy-service`, con base propia (`policy_db`), las proyecciones `accepted_quotes` y `claim_ref`, y `policy.*` por Outbox. La saga completa (cotizar → aceptar → emitir → notificar → siniestro → renovar) funcionó de extremo a extremo **solo con eventos y proyecciones**, y la doble emisión en paralelo nunca generó más de una póliza. El 2026-09-27 se hizo **el corte de las fases 3 a 6** (sección 7): el gateway envía todo el negocio a los microservicios y el monolito quedó sin tráfico, con su código sin modificar. Retirarlo del Compose (pasos 6.10 y 6.11) queda pendiente.

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
| 6.10 Apagar el monolito | **No se hizo**: el equipo decidió mantener el monolito congelado y corriendo. Ver sección 3.2 | ⏳ Pendiente del corte |
| 6.11 Retirar `andina.insurance.events` | **No se hizo**: el monolito sigue publicando `policy.issued.v1` ahí | ⏳ Pendiente del corte |

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

| Funcionalidad | Dónde está hoy |
|---|---|
| Todas las rutas de la API (clientes, cotizaciones, pólizas, siniestros, renovaciones, "Mi cuenta") | Monolito |
| Publicar `customer.*` y `policy.issued.v1` | Monolito |

### 3.2 El corte (fases 3 a 6 juntas) y el apagado del monolito

Con las fases 3 a 6 implementadas, el corte puede hacerse de una vez y **sin modificar el monolito**, porque ya no queda ninguna función que dependa de él:

1. Detener la escritura en el monolito (ventana de mantenimiento) y repetir, en orden, `migrar-clientes.sh`, `migrar-siniestros.sh`, `migrar-cotizaciones.sh` y `migrar-polizas.sh`.
2. Descartar los eventos `PENDING` de pruebas que haya en los Outbox de los servicios nuevos.
3. Activar la publicación en los cuatro servicios: `CUSTOMER_EVENTS_PUBLISH_ENABLED`, `CLAIMS_EVENTS_PUBLISH_ENABLED`, `QUOTATION_EVENTS_PUBLISH_ENABLED` y `POLICY_EVENTS_PUBLISH_ENABLED` en `true`.
4. En el gateway, reemplazar la ruta `backend` por las rutas de cada servicio. Las de siniestros van **antes** que `/api/polizas/**`. `/api/mi-cuenta` pasa a ser una composición de customer-service (`/api/clientes/{customerId}`) y policy-service (`/api/mi-cuenta/polizas`), con respuesta parcial si uno no responde.
5. Ejecutar los backfill: `POST /api/clientes/eventos/reenvio` y `POST /api/siniestros/eventos/reenvio`.
6. **Paso 6.10:** retirar `backend` (y su dependencia del gateway) de `docker-compose.yml`. Respaldar `andina_clean_mongo_data` y conservarlo un tiempo antes de borrarlo. El código del monolito se conserva en el repositorio, sin cambios.
7. **Paso 6.11:** cuando nadie publique en `andina.insurance.events`, quitar los enlaces a ese exchange en notification, claims y quotation.

**Reversa (antes del paso 6):** volver la ruta `backend` al gateway y apagar los interruptores.

## 4. Verificación

Pruebas hechas el 2026-09-27 contra el stack de Compose, por la red interna (los servicios no están enrutados), con el token RS256 de identity-service. Para probar la saga, la publicación se activó **temporalmente** en quotation, claims y policy (customer-service no), con WhatsApp simulado. Al terminar se volvió a apagar.

| # | Criterio de salida (ruta, sección 10) | Cómo se probó | Resultado |
|---|---|---|---|
| 1 | Emitir y renovar pólizas funciona solo con eventos y proyecciones | Cotizar y aceptar en quotation → `quote.accepted.v1` llegó a `accepted_quotes`; emitir en policy → 201; `policy.issued.v1` dejó la cotización EMITIDA en quotation, la póliza VIGENTE en `policy_ref` de claims y el WhatsApp enviado por notification. Un siniestro abierto registrado en claims llegó a `claim_ref` y la evaluación respondió **422 `SINIESTROS_PENDIENTES`**; al liquidarlo, la evaluación dio 201 (REQUIERE_RECALCULO, prima 1525.50), se aprobó y se generó la póliza renovada; `policy.renewed.v1` dejó en claims la anterior RENOVADA (v2) y la nueva VIGENTE. Colas y DLQ en 0 | ✅ Cumple |
| 2 | Emitir la misma cotización dos veces en paralelo genera una sola póliza | 5 rondas de 3 solicitudes simultáneas por cotización: **exactamente 1 póliza en las 5**; las perdedoras respondieron 422 `COTIZACION_YA_EMITIDA` y quotation registró cada compensación. Ver el error corregido abajo | ✅ Cumple |
| 3 | El flujo cotizar → aceptar → emitir → WhatsApp con un solo `correlationId` visible en Grafana y Jaeger | Después del corte (sección 7), el flujo completo por el gateway con un `X-Correlation-Id` fijo: el mismo id aparece en los logs JSON del gateway, quotation, policy, claims, notification e identity, y el WhatsApp se envió. **No se levantó el stack de observabilidad** y los 4 servicios nuevos todavía no están en Prometheus/Grafana/Jaeger | ⏳ Parcial (logs sí; Grafana y Jaeger pendientes) |
| 4 | El monolito ya no está en el Compose | Contradice la decisión de mantener el monolito congelado y en paralelo; es el paso 6 del corte (sección 3.2) | ⏳ Pendiente del corte |

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
| **Monolito en el Compose** | Sigue corriendo, sin tráfico de negocio desde el corte. Retirarlo es el paso 6.10 |
| **Vencer y cancelar pólizas** | No existe la operación en el sistema; los contratos están listos para cuando exista |
| **Observabilidad** | Los servicios nuevos no están en Prometheus ni en el Compose de observabilidad (fase 7) |
| **Kubernetes** | Manifiestos escritos y validados con `--dry-run=server`; no aplicados |

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

## 6. Archivos

| Área | Archivos |
|---|---|
| Servicio | `services/policy-service/**` |
| Migración | `services/policy-service/migracion/migrar-polizas.sh` |
| Cambios en otros servicios | claims-service (`PolicyEventsListener`, `ActualizarEstadoPolizaUseCase`, `MongoPolizaRefRepository`, colas), quotation-service (`PolicyEventsListener`, `RegistrarRechazoEmisionUseCase`, `MongoRechazoEmisionRepository`, colas) |
| Contratos | `contracts/events/policy.issued.v1` (campos opcionales), `policy.renewed.v1`, `policy.issuance-rejected.v1`, `policy.expired.v1`, `policy.cancelled.v1`; `contracts/README.md` |
| Infraestructura | `Arquitectura-Clean/docker-compose.yml`, `Arquitectura-Clean/.env.example` |
| CI | `.github/workflows/policy-service.yml` |
