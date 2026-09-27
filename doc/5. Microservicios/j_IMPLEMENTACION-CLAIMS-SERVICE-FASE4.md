# Implementación de claims-service (fase 4 de la migración a microservicios)

Este documento registra lo que se implementó en la fase 4 de la [ruta de implementación](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#8-fase-4--claims-service), las decisiones, el impacto en el monolito y la verificación contra el stack de Docker.

**Resultado:** los siniestros viven en `services/claims-service`, con base propia (`claims_db`), Outbox para `claim.*` y la proyección `policy_ref`, que reemplaza la consulta a la base de pólizas. Como en la fase 3, el servicio corre **en paralelo al monolito, sin enrutar**. El monolito sigue atendiendo `/api/polizas/{id}/siniestros/**` y su código no se modificó.


> **Actualización (2026-09-27): corte hecho.** El gateway ya envía este dominio al servicio, que publica sus eventos; el monolito quedó sin tráfico de negocio y su código sin cambios. Detalle y verificación en [m_IMPLEMENTACION-POLICY-SERVICE-FASE6.md, sección 7](m_IMPLEMENTACION-POLICY-SERVICE-FASE6.md#7-corte-de-las-fases-3-a-6-2026-09-27). Lo que sigue describe el trabajo previo al corte.

---

## 1. Qué se hizo, paso por paso

| Paso de la ruta | Implementación | Estado |
|---|---|---|
| 4.1 Publicar `policy.renewed/expired/cancelled` desde el monolito | No se hizo: requiere cambiar el código del monolito. `policy_ref` se mantiene con `policy.issued.v1` (que el monolito ya publica) y con la carga inicial | ⏳ Pendiente (o lo resuelve policy-service en la fase 6) |
| 4.2 Crear claims-service con la plantilla | Estructura Clean con 8 reglas ArchUnit. `Siniestro` (misma regla: un siniestro cerrado no cambia) con **versión del agregado**, `Dinero`, los casos de uso `siniestro/*`. Mismas rutas, reglas por rol y propietario, respuestas y formato de error que el monolito | ✅ |
| 4.3 Proyección `policy_ref` con backfill | Colección `policy_ref` (id, cliente, número, estado). Se alimenta con `policy.issued.v1` (cola propia `claims.policy.events`, enlazada a `andina.insurance.events` y a `andina.events`, con DLQ) mediante un *upsert* que solo inserta si no existe. La carga inicial la hace `migracion/migrar-siniestros.sh` desde la colección `polizas` (solo lectura) | ✅ |
| 4.4 Validar la póliza contra `policy_ref` | Registrar un siniestro exige que la póliza esté en la proyección y `VIGENTE`; si no está, responde 404 sin consultar a nadie más. El control de propietario del CLIENTE también usa `policy_ref` | ✅ |
| 4.5 Publicar `claim.registered/status-changed.v1` (Outbox) | Siniestro y evento en una sola transacción (replica set). Los eventos llevan el estado, si está abierto y si el asegurado fue responsable (lo que la renovación necesita). Interruptor `CLAIMS_EVENTS_PUBLISH_ENABLED` (en Compose: `false`). Backfill nuevo: `POST /api/siniestros/eventos/reenvio` (ADMIN) | ✅ (publicación apagada hasta el corte) |
| 4.6 Migrar `siniestros` a `claims_db` | `migrar-siniestros.sh` copia la colección y compara cantidad y huella SHA-256 | ✅ |
| 4.7 Rutas en el gateway | No se cambiaron (sin enrutar). La ruta a agregar está en la sección 3.2 | ⏳ Pendiente del corte |
| 4.8 El monolito consume `claim.*` | No se hizo: requiere cambiar el código del monolito | ⏳ Pendiente (o fase 6) |

Además: `claims-mongodb` y `claims-service` en `Arquitectura-Clean/docker-compose.yml` (sin puertos en el host, sin acceso a la red del backend), contratos `claim.*.schema.json` y CI `.github/workflows/claims-service.yml`. No se escribieron manifiestos de Kubernetes.

## 2. Decisiones

| Decisión | Motivo |
|---|---|
| **Correr en paralelo, sin enrutar** | Misma decisión que la fase 3: el código del monolito no se modifica. Si el gateway enviara los siniestros a claims-service, la renovación del monolito seguiría leyendo su colección `siniestros` y no vería los nuevos |
| **`policy_ref` con *insert-if-absent* para `policy.issued.v1`** | La emisión es el primer estado de una póliza; un evento repetido o atrasado nunca debe pisar un estado posterior. `policy.issued.v1` no trae versión (`aggregateVersion` es null) |
| **Carga inicial por script y resincronización** | El monolito no publica los cambios de estado de las pólizas (renovada, vencida, cancelada). Repetir `migrar-siniestros.sh` actualiza `policy_ref`. En la fase 6, policy-service publicará `policy.*` con versión y el script dejará de hacer falta |
| **Versión en `Siniestro`** | Sube en cada cambio de estado y viaja en `claim.*`: policy-service (fase 6) podrá descartar eventos viejos al mantener `open_claims_by_policy` |
| **Campos `open` e `insuredResponsible` en `claim.*`** | Son exactamente lo que usa `EvaluarRenovacionUseCase`: bloquear si hay siniestros abiertos y contar los responsables para la prima |
| **Póliza fuera de la proyección = 404 (y 403 para un CLIENTE)** | Paso 4.4: rechazar con un mensaje claro en vez de consultar la base de pólizas. Para el CLIENTE se mantiene el 403 del monolito, que no revela qué identificadores existen |
| **Nombres sin prefijo de la empresa** | Imagen `claims-service:1.0.0`, contenedores `claims-service` y `claims-mongodb`, volumen `claims_mongo_data`, red `claims_data_network` |

## 3. Impacto en el monolito

**No se modificó el código del monolito.** En `Arquitectura-Clean/docker-compose.yml` y `.env.example` solo se agregaron entradas nuevas. claims-service consume `policy.issued.v1` con una cola propia, sin cambiar lo que publica el monolito. Detalle de todas las fases en [k_IMPACTO-EN-EL-MONOLITO.md](k_IMPACTO-EN-EL-MONOLITO.md).

### 3.1 Qué sigue haciendo el monolito

| Funcionalidad | Dónde está hoy |
|---|---|
| `/api/polizas/{id}/siniestros/**` (lo que usa el frontend) | Monolito |
| Bloquear la renovación si hay siniestros abiertos | Monolito, leyendo su colección `siniestros` |
| Publicar `policy.issued.v1` | Monolito (claims-service lo consume) |

### 3.2 Pasos del corte

1. Repetir `sh services/claims-service/migracion/migrar-siniestros.sh`.
2. En el monolito: consumir `claim.*` y mantener el contador de siniestros abiertos por póliza (paso 4.8), y publicar `policy.renewed/expired/cancelled` (paso 4.1). Alternativa: esperar a la fase 6, donde policy-service asume las dos cosas.
3. En el gateway, **antes** de la ruta `backend` (que incluye `/api/polizas/**`):
   ```yaml
   - id: claims
     uri: ${CLAIMS_SERVICE_URL:http://localhost:8086}
     predicates:
       - Path=/api/polizas/*/siniestros/**,/api/siniestros/**
     filters:
       - name: CircuitBreaker
         args: { name: claimsCB, fallbackUri: forward:/fallback/backend }
   ```
4. `CLAIMS_EVENTS_PUBLISH_ENABLED=true`, reiniciar claims-service y ejecutar `POST /api/siniestros/eventos/reenvio`.

**Reversa:** volver la ruta al monolito; la colección `siniestros` del monolito no se borra.

## 4. Verificación

Pruebas hechas el 2026-09-27 contra el stack de Compose. claims-service se llamó por la red interna (no está enrutado); el monolito, por el gateway. WhatsApp se simuló (`--profile whatsapp-mock`) para no dejar mensajes fallidos en la DLQ al emitir pólizas.

| # | Criterio de salida (ruta, sección 8) | Cómo se probó | Resultado |
|---|---|---|---|
| 1 | Registrar y cambiar el estado de un siniestro funciona sin acceso a la base de pólizas | Se emitió una póliza en el monolito; `policy.issued.v1` la dejó en `policy_ref` (VIGENTE, `origen: POLICY_ISSUED`) en menos de 1 s. En claims-service: registrar siniestro **201**, cambiar a LIQUIDADO **200**, cambiar un cerrado **422** `SINIESTRO_CERRADO`, póliza fuera de la proyección **404**, póliza VENCIDA (carga inicial) **422** `POLIZA_NO_VIGENTE`. Desde el contenedor, `andina-clean-mongodb` y `mongodb` no resuelven: no hay ruta a la base del backend | ✅ Cumple |
| 2 | Una renovación se bloquea si hay siniestros abiertos, usando el contador actualizado por eventos | La renovación sí se bloquea (`422 SINIESTROS_PENDIENTES`), pero el monolito usa su propia colección `siniestros`, no un contador por eventos. Requiere el paso 4.8 o la fase 6 | ⏳ Parcial |
| 3 | Con claims-service caído, la emisión y renovación de pólizas siguen funcionando | Con claims-service detenido: cotizar, aceptar y emitir póliza por el gateway respondieron bien; evaluar una renovación respondió (bloqueada por un siniestro abierto). El `policy.issued.v1` quedó en la cola `claims.policy.events` (1 mensaje); al levantar claims-service se procesó (cola en 0, DLQ en 0) y la póliza apareció en `policy_ref` | ✅ Cumple |

Pruebas adicionales:

| Prueba | Resultado |
|---|---|
| Migración (`migrar-siniestros.sh`) | Siniestros con la misma huella SHA-256 en las dos bases; `policy_ref` con las 16 pólizas del monolito |
| Paridad con el monolito | Se registró un siniestro en el monolito, se repitió la migración y `GET /api/polizas/{id}/siniestros` devolvió **exactamente la misma respuesta** en el monolito y en claims-service |
| Outbox con la publicación apagada | `claim.registered.v1` y `claim.status-changed.v1` quedaron `PENDING` |
| Pruebas automáticas | 23 (8 reglas ArchUnit, casos de uso, control de acceso con `policy_ref`, formato de `claim.*`, relay del Outbox). Corren al construir la imagen |

Observación ajena a esta fase: la primera emisión de póliza por el gateway respondió **503** (el límite de 3 s del gateway hacia el backend), aunque el monolito sí la emitió. Las siguientes respondieron a tiempo. Conviene revisar ese límite para `POST /api/polizas` en la fase 6.

## 5. Limitaciones conocidas

| Punto | Detalle |
|---|---|
| **Estados de póliza** | `policy_ref` conoce las pólizas emitidas en vivo, pero una renovación, un vencimiento o una cancelación solo llegan al repetir el script, hasta que exista `policy.*` (paso 4.1 o fase 6) |
| **Criterio "la renovación se bloquea con el contador actualizado por eventos"** | Depende del paso 4.8 (cambio en el monolito) o de la fase 6 |
| **Datos en paralelo** | Lo que se registre directamente en claims-service no llega al monolito, y viceversa, hasta el corte |

## 6. Archivos

| Área | Archivos |
|---|---|
| Servicio | `services/claims-service/**` |
| Migración | `services/claims-service/migracion/migrar-siniestros.sh` |
| Contratos | `contracts/events/claim.registered.v1.schema.json`, `claim.status-changed.v1.schema.json`, `contracts/README.md` |
| Infraestructura | `Arquitectura-Clean/docker-compose.yml`, `Arquitectura-Clean/.env.example` |
| CI | `.github/workflows/claims-service.yml` |
