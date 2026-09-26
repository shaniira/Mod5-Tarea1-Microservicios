# Andina Seguros — Ruta de implementación de la migración a microservicios

Este documento convierte la propuesta ([c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md](c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md)) en una secuencia de pasos ejecutables. Sigue el diagrama objetivo ([c_DIAGRAMA-ARQUITECTURA-MICROSERVICIOS.svg](c_DIAGRAMA-ARQUITECTURA-MICROSERVICIOS.svg)) y resuelve los riesgos de [b_ANALISIS-RIESGOS-ARQUITECTURA.md](b_ANALISIS-RIESGOS-ARQUITECTURA.md).

Decisiones de partida: **6 microservicios** (identity, customer, quotation, policy, claims, notification) + API Gateway, **RabbitMQ** y **MongoDB** se mantienen, se añade **Redis**, observabilidad con **logs con correlationId, métricas y trazas OpenTelemetry**, y **Docker Compose** como orquestador.

> Los tamaños de esfuerzo (**S** pequeño, **M** mediano, **L** grande) son relativos, no fechas. Ajusta el calendario al tamaño del equipo.

---

## 1. Vista general de la ruta

| Fase | Nombre | Qué se logra al terminar | Esfuerzo |
|---|---|---|---|
| **0** | Preparación y seguridad base | El monolito es seguro, publica eventos sin perderlos, es observable y ya se accede por el gateway | L |
| **1** | notification-service | El consumer deja de leer la base del backend y tolera la caída de WhatsApp | M |
| **2** | identity-service | Login, MFA y sesión en un servicio propio, con JWT RS256 y estado en Redis | L |
| **3** | customer-service | Clientes, vehículos y consulta de placas fuera del monolito | M |
| **4** | claims-service | Siniestros independientes | M |
| **5** | quotation-service | Tarifas y cotizaciones independientes | L |
| **6** | policy-service | Pólizas y renovaciones independientes; **se apaga el monolito** | L |
| **7** | Endurecimiento | Pruebas de caos, alertas, carga y revisión de seguridad completa | M |

Dependencias entre fases (cada flecha significa "debe estar terminada antes"):

```
Fase 0 ──► Fase 1 ──► Fase 2 ──► Fase 3 ──► Fase 4 ──► Fase 5 ──► Fase 6 ──► Fase 7
              │                    │           ▲
              └─ eventos customer.* publicados por el monolito (se añaden en la fase 1)
                                   └─ customer-service pasa a publicarlos
```

Regla general: **no se empieza una fase hasta que la anterior cumple sus criterios de salida**, y en cada fase el sistema completo sigue funcionando.

---

## 2. Reglas de trabajo para toda la migración

1. **Una fase = una rama y un conjunto de pull requests pequeños**, nunca un cambio gigante.
2. **Antes de mover un dominio, escribir pruebas de caracterización** sobre el comportamiento actual (qué responde cada endpoint), para comprobar que el servicio nuevo hace lo mismo.
3. **El monolito sigue siendo la fuente de verdad** de un dominio hasta que la ruta del gateway cambia. Se migra por copia y comparación, no por corte brusco.
4. **Todo servicio nuevo nace con la plantilla de la sección 3.** No hay excepciones "por ahora".
5. **Ningún servicio accede a la base de otro**, ni siquiera de forma temporal en desarrollo.
6. **Los cambios de contrato son compatibles hacia atrás** (solo campos opcionales nuevos); un cambio incompatible crea una versión nueva.
7. **Cada fase termina con un plan de reversa probado**: poder volver la ruta del gateway al monolito.

---

## 3. Plantilla de un microservicio (checklist de creación)

Cada uno de los 6 servicios se crea con esta lista. Es lo que se repite en las fases 1 a 6.

| # | Paso | Detalle |
|---|---|---|
| 1 | Estructura | Carpeta `services/<nombre>-service/` con Maven propio y los cuatro paquetes Clean: `entities`, `usecases`, `interfaceadapters`, `frameworksdrivers`. Copiar y adaptar la prueba **ArchUnit** del backend actual |
| 2 | Dominio | Mover las entidades, casos de uso y puertos de su contexto (tabla 3.2 de la propuesta). Duplicar a propósito los value objects compartidos |
| 3 | Base propia | Base y usuario propios en MongoDB (`<nombre>_db`); credenciales por variable de entorno |
| 4 | Configuración | Todo por variables de entorno (`SPRING_DATA_MONGODB_URI`, `SPRING_RABBITMQ_*`, secretos). Ningún token en `application.yml` |
| 5 | Salud | Spring Boot Actuator: *liveness* y *readiness* (`/actuator/health/liveness`, `/actuator/health/readiness`) que dependan de Mongo y RabbitMQ |
| 6 | Seguridad | Spring Security como *resource server*: valida el JWT con la clave pública de identity (JWKS), aplica **roles y propiedad del recurso** en cada endpoint |
| 7 | Logs | Salida JSON a stdout con `service`, `correlationId`, `traceId`, `spanId`, `userId`. Filtro que lee `X-Correlation-Id` y lo coloca en el MDC |
| 8 | Métricas | Micrometer con `/actuator/prometheus`; métricas de HTTP, circuit breaker, Outbox y negocio |
| 9 | Trazas | Agente o SDK de OpenTelemetry con exportación OTLP hacia el Collector; propagación de `traceparent` en HTTP y en mensajes |
| 10 | Resiliencia | Resilience4j: timeout, circuit breaker, retry y bulkhead en cada llamada saliente (tabla 6.2 de la propuesta) |
| 11 | Mensajería | **Outbox** para publicar y **Inbox** para consumir (secciones 5.2 y 5.3 de la propuesta), con cola propia por evento, cola de reintento y DLQ |
| 12 | Contratos | OpenAPI del servicio y esquemas de sus eventos en `contracts/`; pruebas de contrato |
| 13 | Imagen | `Dockerfile` multi-etapa, usuario no root, sin puertos en el host |
| 14 | Compose | Entrada en `infra/docker-compose.yml` con `depends_on` y *healthcheck*, redes según el diagrama |
| 15 | Gateway | Ruta en el gateway con timeout y circuit breaker propios |
| 16 | CI | Pipeline propio: compilar, pruebas, ArchUnit, pruebas de contrato, construir imagen |

---

## 4. Fase 0 — Preparación y seguridad base

**Objetivo:** dejar el monolito seguro, con eventos confiables y observable, y poner el gateway delante. Sin esta fase, se repartirían los problemas actuales en seis servicios.

### 4.1 Pasos

| Paso | Acción | Riesgo que cierra | Esfuerzo |
|---|---|---|---|
| 0.1 | **Estructura del repositorio.** Crear `contracts/`, `gateway/`, `services/`, `infra/` (ver sección 8 de la propuesta). El backend actual sigue en `Arquitectura-Clean/` | — | S |
| 0.2 | **Cerrar el registro con rol.** `POST /api/auth/register` no debe aceptar `rol` del cliente: el usuario nuevo queda con rol mínimo, y solo un ADMIN autenticado puede crear cuentas con más permisos | S1 | S |
| 0.3 | **Control de acceso completo.** `@PreAuthorize` por rol en todos los endpoints de cotizaciones, pólizas, siniestros, renovaciones y vehículos; y en los casos de uso, verificar que un CLIENTE solo accede a sus recursos (`customerId` en el token). Pruebas para cada caso de "CLIENTE intenta acceder a lo de otro" | S2 | M |
| 0.4 | **Secretos fuera del código.** Mover los tokens de JSON.pe, el `JWT_SECRET` y la contraseña de RabbitMQ a variables de entorno. **Rotar los tokens ya expuestos.** Hacer que la aplicación no arranque sin `JWT_SECRET`. Confirmar que ningún `.env` esté en el repositorio | S3 | S |
| 0.5 | **Quitar los datos demo del arranque normal.** El `MongoDemoDataInitializer` (usuario `admin` / `Admin123*`) solo con perfil `dev`. Retirar la pista de contraseña del login | S7 | S |
| 0.6 | **Autenticar MongoDB y RabbitMQ.** Usuario y contraseña de MongoDB; contraseña fuerte en RabbitMQ; dejar de publicar los puertos `27020`, `5672` y `15672` en el host | S4, S5 | S |
| 0.7 | **MongoDB como replica set** (un nodo basta en desarrollo). Necesario para el Outbox transaccional | — | S |
| 0.8 | **Outbox en el monolito.** Colección `outbox`; el caso de uso guarda el cambio y el evento en la misma transacción; un *relay* publica a RabbitMQ con confirmación y marca como enviado; métrica de antigüedad del evento pendiente más viejo. Aplicar primero a `EmitirPolizaUseCase` | A1 | M |
| 0.9 | **Observabilidad base.** (a) Filtro de `X-Correlation-Id` y MDC; (b) logs JSON; (c) Micrometer + `/actuator/prometheus`; (d) agente OpenTelemetry; (e) levantar `infra/docker-compose.observability.yml` con Promtail, Loki, Prometheus, OTel Collector, Jaeger y Grafana | Base de A5 y de depuración | M |
| 0.10 | **Gateway delante del monolito.** Spring Cloud Gateway con una sola ruta `/api/**` hacia el backend; genera `X-Correlation-Id`, aplica CORS único y (si aplica) rate limiting. El frontend pasa a llamar al gateway (`VITE_API_URL=http://localhost:8080/api`) | Base de S9, S10 | M |
| 0.11 | **Límites de intentos y Swagger.** Rate limiting en `login` y verificación MFA; Swagger solo en `dev` | S9, S10 | S |
| 0.12 | **Módulo `contracts/`.** Esquemas JSON de los eventos del catálogo (sección 4 de la propuesta) y OpenAPI del backend actual | — | S |
| 0.13 | **Red de seguridad de pruebas.** Pruebas de caracterización de los endpoints principales y una prueba de extremo a extremo con Testcontainers: emitir póliza → evento → notificación (con WhatsApp simulado) | — | M |
| 0.14 | **CI mínima.** Pipeline que compila, ejecuta pruebas y construye la imagen del backend | — | S |

Nota técnica sobre Spring Cloud Gateway: es reactivo (WebFlux). Va en su propio proyecto, no dentro del backend actual (que usa Spring MVC).

### 4.2 Criterios de salida de la fase 0

Verificados el 2026-09-26; evidencias en [h_CIERRE-PENDIENTES.md](h_CIERRE-PENDIENTES.md).

- [x] Un usuario sin sesión no puede crear cuentas con rol ADMIN.
- [x] Un CLIENTE no puede leer ni modificar recursos de otro cliente (prueba automatizada).
- [x] No queda ningún secreto en el repositorio. *Rotar en el panel de JSON.pe los tokens que estuvieron versionados es una acción manual del dueño de la cuenta.*
- [x] MongoDB y RabbitMQ exigen credenciales y no son accesibles desde fuera de Docker.
- [x] Apagar RabbitMQ durante una emisión de póliza **no pierde el evento**; se publica al volver.
- [x] Una petición se puede seguir en Grafana y Jaeger por su `correlationId`.
- [x] El frontend funciona a través del gateway.

---

## 5. Fase 1 — notification-service

**Por qué primero:** es el que menos depende del resto y ya está separado. Resuelve además el acoplamiento por base de datos compartida.

| Paso | Acción | Esfuerzo |
|---|---|---|
| 1.1 | **Publicar eventos de cliente desde el monolito.** Añadir `customer.registered.v1` y `customer.updated.v1` (con Outbox) al alta y modificación de clientes | M |
| 1.2 | **Backfill.** Endpoint interno o proceso único que emite un evento por cada cliente existente, para poblar la proyección | S |
| 1.3 | **Base propia** `notification_db` con las colecciones `customer_contacts` (proyección) e `inbox` (reemplaza a `processed_notification_events`) | S |
| 1.4 | **Consumidor de `customer.*`** que mantiene `customer_contacts`, descartando versiones más viejas (campo de versión del agregado) | M |
| 1.5 | **Cambiar la búsqueda del teléfono:** `PolicyNotificationService` lee de `customer_contacts`, ya no de `clientes`. Quitar del consumer la dependencia de la entidad `ClienteDocument` del backend | S |
| 1.6 | **Circuit breaker y pausa del listener** para WhatsApp: si el circuito se abre, se detiene el consumo (o se envía a la cola de reintento con espera) en vez de agotar los 4 reintentos y llenar la DLQ | M |
| 1.7 | **DLQ operativa:** alerta por tamaño y procedimiento documentado de reproceso | S |
| 1.8 | **Plantilla de la sección 3** aplicada (salud, logs, métricas, trazas, imagen, Compose) y el consumo enlazado a **ambos exchanges** (`andina.insurance.events` y `andina.events`) | M |
| 1.9 | **Quitar el acceso a la base del backend:** credenciales de `notification_db` solamente | S |

**Criterios de salida** (verificados el 2026-09-25; evidencias en [f_IMPLEMENTACION-NOTIFICATION-SERVICE-FASE1.md](f_IMPLEMENTACION-NOTIFICATION-SERVICE-FASE1.md#3-verificación-de-los-criterios-de-aceptación))
- [x] Con MongoDB del backend inaccesible para notification-service, las notificaciones siguen funcionando.
- [x] Un cliente nuevo aparece en `customer_contacts` en segundos; un cliente existente aparece tras el backfill.
- [x] Con WhatsApp caído, los mensajes se retienen (no van a la DLQ) y se envían al recuperarse.
- [x] Un evento repetido no envía el WhatsApp dos veces.

**Reversa:** volver a la versión anterior del consumer (que lee `clientes`); no se ha borrado nada.

---

## 6. Fase 2 — identity-service

| Paso | Acción | Esfuerzo |
|---|---|---|
| 2.1 | **Redis** en Compose (red `data`, sin puerto en el host) | S |
| 2.2 | **JWT RS256:** generar el par de claves (la privada solo en identity, por secreto); exponer las claves públicas en `/.well-known/jwks.json`; incluir en el token `sub`, `roles` y `customerId` | M |
| 2.3 | **El monolito valida por JWKS** (Spring Security *resource server*) y acepta durante una ventana ambos formatos (HS256 antiguo y RS256), para no cerrar sesiones abiertas. Después se retira HS256 | M |
| 2.4 | **Crear identity-service** con la plantilla: mover `auth/*`, `mfa/*` y los adaptadores de Google, Facebook, TOTP, QR, BCrypt y AES-GCM | L |
| 2.5 | **Estado efímero a Redis:** state OAuth de Facebook (300 s), tickets de login (60 s) y desafíos MFA (300 s), con TTL. Reemplaza los adaptadores `InMemory*` | M |
| 2.6 | **Índice de correos de clientes:** proyección `customer_email_index` alimentada por `customer.*` (ya publicados en la fase 1), para decidir el acceso de un CLIENTE sin consultar la base de clientes | M |
| 2.7 | **Migrar la colección `usuarios`** a `identity_db` (copia, comparación, corte); conservar los hashes BCrypt tal cual | M |
| 2.8 | **Circuit breaker y caché de claves de Google;** timeout y circuit breaker hacia Facebook (sin reintentos: el código OAuth es de un solo uso) | M |
| 2.9 | **Rutas en el gateway:** `/api/auth/**` y `/api/mfa/**` hacia identity-service; el gateway valida el JWT en el resto | S |
| 2.10 | **Prueba con 2 réplicas** de identity-service: login con Facebook y MFA deben funcionar sin fallos intermitentes | S |

**Criterios de salida** (verificados el 2026-09-25; evidencias en [g_IMPLEMENTACION-IDENTITY-SERVICE-FASE2.md](g_IMPLEMENTACION-IDENTITY-SERVICE-FASE2.md#3-verificación-de-los-criterios-de-aceptación))
- [x] Login por contraseña, Google, Facebook y MFA funcionan desde el servicio nuevo.
- [x] Con dos réplicas no hay fallos aleatorios de MFA ni de Facebook.
- [x] Los demás servicios validan tokens con la clave pública; la clave privada solo existe en identity.
- [x] HS256 retirado; el secreto simétrico ya no existe.

**Reversa:** apuntar `/api/auth/**` de nuevo al monolito (los usuarios no se borran del monolito hasta el final de la fase).

---

## 7. Fase 3 — customer-service

| Paso | Acción | Esfuerzo |
|---|---|---|
| 3.1 | **Crear customer-service** con la plantilla: mover `cliente/*` (crear, listar, obtener), `vehiculo/*`, `ConsultarInformacionVehiculoService` y los adaptadores `jsonpe` | L |
| 3.2 | **Consulta de placas resiliente:** timeout de 5 s, circuit breaker, un reintento con espera aleatoria, *bulkhead* y **caché en Redis** (TTL 24 h). Alternativa cuando falla: permitir ingresar los datos del vehículo manualmente | M |
| 3.3 | **Publicar** `customer.registered/updated` y `vehicle.registered` (Outbox) desde el servicio nuevo | M |
| 3.4 | **Migrar `clientes` y `vehiculos`** a `customer_db`; hacer *backfill* de las proyecciones de notification e identity | M |
| 3.5 | **Cambio de fuente de los eventos:** el monolito deja de publicar `customer.*` y lo hace customer-service (sin duplicados: el interruptor se activa a la vez que se cambia la ruta) | S |
| 3.6 | **Rutas en el gateway:** `/api/clientes/**` y `/api/vehiculos/**` | S |
| 3.7 | **"Mi cuenta"** pasa al gateway como composición (identity + customer + policy + renovaciones) con respuesta parcial si un servicio no responde. Mientras policy siga en el monolito, se compone con este | M |

**Criterios de salida**
- [ ] Ningún servicio consulta `clientes` ni `vehiculos` fuera de customer-service.
- [ ] Con JSON.pe caído, se puede registrar un vehículo de forma manual y las placas ya consultadas siguen respondiendo desde la caché.
- [ ] Las proyecciones de notification e identity coinciden con los conteos de `customer_db`.

**Reversa:** ruta del gateway de vuelta al monolito y reactivar la publicación de eventos desde él.

---

## 8. Fase 4 — claims-service

| Paso | Acción | Esfuerzo |
|---|---|---|
| 4.1 | **Publicar desde el monolito** los eventos de póliza que aún no existen: `policy.renewed.v1`, `policy.expired.v1`, `policy.cancelled.v1` (junto con el `policy.issued.v1` actual) | M |
| 4.2 | **Crear claims-service** con la plantilla: mover `siniestro/*` y la entidad `Siniestro` | M |
| 4.3 | **Proyección `policy_ref`** (existencia, cliente y vigencia de cada póliza), alimentada por `policy.*` con *backfill* | M |
| 4.4 | **Validar la póliza al registrar un siniestro** contra `policy_ref`; si la proyección no la tiene todavía, rechazar con mensaje claro (no consultar la base de pólizas) | S |
| 4.5 | **Publicar** `claim.registered.v1` y `claim.status-changed.v1` (Outbox) | M |
| 4.6 | **Migrar `siniestros`** a `claims_db` | S |
| 4.7 | **Rutas en el gateway.** La ruta `/api/polizas/{id}/siniestros/**` debe declararse **antes** que `/api/polizas/**` | S |
| 4.8 | **El monolito consume `claim.*`** para bloquear renovaciones con siniestros pendientes (mantiene un contador de abiertos por póliza) | M |

**Criterios de salida**
- [ ] Registrar y cambiar el estado de un siniestro funciona sin acceso a la base de pólizas.
- [ ] Una renovación se bloquea si hay siniestros abiertos, usando el contador actualizado por eventos.
- [ ] Con claims-service caído, la emisión y renovación de pólizas siguen funcionando.

---

## 9. Fase 5 — quotation-service

| Paso | Acción | Esfuerzo |
|---|---|---|
| 5.1 | **Crear quotation-service** con la plantilla: mover `tarifa/*`, `cotizacion/*`, `MotorDeTarificacion`, `FactorRiesgo`, `TablaTarifaria` y `ResultadoTarificacion` | L |
| 5.2 | **Proyecciones `customer_ref` y `vehicle_ref`** alimentadas por `customer.*` y `vehicle.registered`, con *backfill*. Guardan lo mínimo que necesita la tarificación (tipo, uso, año, valor del vehículo; estado del cliente) | M |
| 5.3 | **Lectura de refuerzo (*read-through*):** si el cliente o vehículo aún no está en la proyección, consultar a customer-service con circuit breaker (timeout 2 s, un reintento en GET); si falla, responder `503` con mensaje claro | M |
| 5.4 | **Publicar** `quote.accepted.v1` (Outbox) con todos los datos que policy necesita (prima, cobertura, vigencia, tarifa aplicada), y consumir `policy.issued.v1` para marcar la cotización como emitida | M |
| 5.5 | **Migrar `tablas_tarifarias` y `cotizaciones`** a `quotation_db` | M |
| 5.6 | **Rutas en el gateway:** `/api/tablas-tarifarias/**` y `/api/cotizaciones/**` | S |
| 5.7 | **Pruebas del motor de tarificación** portadas y ejecutadas contra las mismas entradas que en el monolito (resultado idéntico) | M |

**Criterios de salida**
- [ ] Se puede cotizar con customer-service caído si el cliente y el vehículo ya están en las proyecciones.
- [ ] El resultado de la tarificación es idéntico al del monolito para un conjunto de casos de referencia.
- [ ] Aceptar una cotización genera `quote.accepted.v1` en el Outbox.

---

## 10. Fase 6 — policy-service y apagado del monolito

| Paso | Acción | Esfuerzo |
|---|---|---|
| 6.1 | **Crear policy-service** con la plantilla: mover `poliza/*`, `renovacion/*`, `EvaluadorRenovacion`, `CalculadorPrimaRenovacion`, `PoliticaVariacionPrima` y `PropuestaRenovacion` | L |
| 6.2 | **Proyecciones** `accepted_quotes` (de `quote.accepted`) y `open_claims_by_policy` (de `claim.*`) | M |
| 6.3 | **Saga de emisión de póliza** (coreografía): al recibir la solicitud de emisión, validar contra `accepted_quotes`, crear la póliza `VIGENTE`, publicar `policy.issued.v1`. **Índice único por `quoteId`** para impedir la doble emisión aunque lleguen dos solicitudes a la vez | L |
| 6.4 | **Compensación:** si la cotización ya no es válida (vencida o ya emitida), publicar `policy.issuance-rejected.v1`; quotation la devuelve a "aceptada" con el motivo | M |
| 6.5 | **Renovaciones dentro del servicio:** evaluar, aprobar, rechazar y generar la póliza renovada. Si el contador de siniestros no está al día, tratarlo como "hay pendientes" y reintentar | M |
| 6.6 | **Publicar** `policy.issued/renewed/expired/cancelled` (Outbox), ya con policy-service como fuente | M |
| 6.7 | **Migrar `polizas` y `propuestas_renovacion`** a `policy_db`; cambiar la fuente de los eventos a policy-service (interruptor único) | M |
| 6.8 | **Rutas en el gateway:** `/api/polizas/**` y `/api/renovaciones/**` | S |
| 6.9 | **"Mi cuenta"** pasa a usar policy-service | S |
| 6.10 | **Apagar el monolito:** retirar `Arquitectura-Clean` del Compose, conservar su repositorio archivado y su base **respaldada** durante un periodo de seguridad antes de borrarla | S |
| 6.11 | **Retirar el exchange antiguo** `andina.insurance.events` y los enlaces dobles | S |

**Criterios de salida**
- [ ] Emitir y renovar pólizas funciona solo con eventos y proyecciones.
- [ ] Emitir la misma cotización dos veces en paralelo genera una sola póliza.
- [ ] El flujo completo cotizar → aceptar → emitir → notificar por WhatsApp funciona de extremo a extremo con un solo `correlationId` visible en Grafana y Jaeger.
- [ ] El monolito ya no está en el Compose.

---

## 11. Fase 7 — Endurecimiento y cierre

| Paso | Acción | Esfuerzo |
|---|---|---|
| 7.1 | **Pruebas de caos:** apagar uno a uno MongoDB, RabbitMQ, Redis, cada servicio y los proveedores externos; verificar las degradaciones planificadas (sección 6.4 de la propuesta) | M |
| 7.2 | **Alertas** en Grafana: circuit breaker abierto más de 2 min, DLQ con mensajes, Outbox con pendientes de más de 5 min, errores 5xx superiores al 5 %, servicio sin *readiness* | S |
| 7.3 | **Reconciliación nocturna** de proyecciones contra sus fuentes (conteos o hashes) | M |
| 7.4 | **Prueba de carga** sobre el gateway y sobre la emisión de pólizas; ajustar timeouts, tamaños de pool y prefetch | M |
| 7.5 | **Revisión de seguridad completa:** repetir el análisis de [b_ANALISIS-RIESGOS-ARQUITECTURA.md](b_ANALISIS-RIESGOS-ARQUITECTURA.md) sobre el sistema final; TLS delante del gateway y del frontend | M |
| 7.6 | **Pruebas de contrato en CI** para cada API y evento; que un cambio incompatible rompa el build | M |
| 7.7 | **Respaldos de MongoDB** y prueba de restauración; documentar el reproceso de la DLQ | S |
| 7.8 | **Documentación operativa:** cómo levantar el stack, cómo leer los paneles, qué hacer ante cada alerta | S |
| 7.9 | **Consumidor de auditoría** (opcional): implementar el de `audit.queue` o eliminar la cola | S |

---

## 12. Procedimiento estándar para migrar los datos de un dominio

Se repite en las fases 2 a 6, sin variaciones:

1. **Crear la base vacía** del servicio nuevo y verificar que su usuario solo accede a ella.
2. **Backfill:** copiar los datos existentes desde el monolito (exportación puntual o un proceso que emita los eventos históricos).
3. **Sincronización continua:** mientras el monolito siga siendo la fuente, el servicio nuevo aplica sus eventos para mantenerse al día.
4. **Comparar:** conteos y verificación por muestreo entre ambos lados (hash de cada documento clave). Debe haber **cero diferencias** durante un periodo de prueba.
5. **Cortar:** cambiar la ruta del gateway al servicio nuevo y, en el mismo momento, mover la publicación de eventos a él.
6. **Observar:** vigilar errores, latencia y circuit breakers en Grafana durante el periodo de seguridad.
7. **Limpiar:** solo después, retirar el dominio del monolito y borrar sus datos (con respaldo previo).

**Reversa** en cualquier momento de los pasos 3 a 6: volver la ruta del gateway al monolito y reactivar en él la publicación de eventos. Los datos del monolito no se han tocado, por lo que no hay pérdida.

---

## 13. Infraestructura objetivo (esqueleto de Compose)

`infra/docker-compose.yml` (resumen; los servicios de negocio siguen todos el mismo patrón):

```yaml
name: andina-microservicios

networks:
  edge: {}
  services: {}
  data: {}
  messaging: {}
  observability: {}

services:
  frontend:      { networks: [edge], ports: ["5173:80"] }
  gateway:       { networks: [edge, services, observability], ports: ["8080:8080"], depends_on: [redis] }

  identity-service:
    networks: [services, data, messaging, observability]
    environment: [SPRING_DATA_MONGODB_URI, SPRING_RABBITMQ_HOST, REDIS_HOST, JWT_PRIVATE_KEY_FILE]
    depends_on: { mongodb: { condition: service_healthy }, rabbitmq: { condition: service_healthy }, redis: { condition: service_healthy } }
  # customer-service, quotation-service, policy-service, claims-service: igual patrón
  notification-service:
    networks: [data, messaging, observability]
    environment: [SPRING_DATA_MONGODB_URI, SPRING_RABBITMQ_HOST, WHATSAPP_TOKEN]

  mongodb:   { networks: [data], command: "--replSet rs0", volumes: ["./mongo-init:/docker-entrypoint-initdb.d"] }
  redis:     { networks: [data] }
  rabbitmq:  { networks: [messaging] }
```

Puntos clave:

- **Un solo puerto de API en el host:** `8080` (gateway), más `5173` (frontend) y `3000` (Grafana).
- **`mongo-init/`** crea una base y un usuario por servicio, y cada usuario tiene permisos solo sobre su base.
- **Orden de arranque** con `depends_on` y *healthchecks*: MongoDB, RabbitMQ y Redis → servicios → gateway → frontend.
- Los secretos llegan por variables de entorno o *Docker secrets*, nunca escritos en el archivo.

Ejemplo de rutas del gateway (la más específica va primero):

```yaml
spring.cloud.gateway.routes:
  - id: claims
    uri: http://claims-service:8080
    predicates: [ Path=/api/polizas/*/siniestros/** ]
  - id: policy
    uri: http://policy-service:8080
    predicates: [ Path=/api/polizas/**, Path=/api/renovaciones/** ]
  - id: identity
    uri: http://identity-service:8080
    predicates: [ Path=/api/auth/**, Path=/api/mfa/** ]
  # customer, quotation: igual patrón, cada una con timeout y circuit breaker
```

---

## 14. Riesgos de la implementación y cómo se mitigan

| Riesgo | Mitigación |
|---|---|
| Un servicio nuevo se comporta distinto al monolito | Pruebas de caracterización (paso 0.13) y comparación de resultados antes de cortar |
| Proyecciones desactualizadas o con huecos | *Backfill* controlado, versión de agregado, reconciliación nocturna (paso 7.3) |
| Eventos duplicados o desordenados | Inbox por `eventId` y descarte por versión |
| Pérdida de eventos al publicar | Outbox transaccional (paso 0.8) |
| Cascada de fallos por llamadas síncronas | Eventos por defecto; circuit breaker, timeout y bulkhead en las pocas llamadas síncronas |
| "Monolito distribuido" (servicios que se llaman en cadena) | Revisión de cada nueva dependencia síncrona contra la regla de la sección 5.4 de la propuesta |
| Depuración difícil | `correlationId` y trazas desde la fase 0 |
| Alcance que crece sin control | Una fase por vez con criterios de salida; no empezar la siguiente sin cumplirlos |
| Corte con datos inconsistentes | Comparación con cero diferencias antes de cortar y reversa probada |
| Sobrecarga operativa de muchos contenedores | Plantilla común (sección 3), Compose único y paneles compartidos |

---

## 15. Qué se logra con todo esto

### 15.1 Antes y después

| Aspecto | Hoy (monolito + consumer) | Al terminar la migración |
|---|---|---|
| **Despliegue** | Un cambio pequeño obliga a reconstruir y reiniciar todo el backend | Cada servicio se construye, prueba y despliega por separado |
| **Escalado** | Solo se puede escalar el backend completo (y no con varias réplicas: estado en memoria) | Se escala solo lo que hace falta; todos los servicios funcionan con 2 o más réplicas |
| **Aislamiento de fallos** | La caída del backend detiene todo | La caída de un servicio degrada solo su función (ejemplo: si claims cae, se siguen emitiendo pólizas) |
| **Datos** | Una base compartida; el consumer lee la colección de otro | Una base por servicio; ningún acceso cruzado; cada dato tiene un solo dueño |
| **Notificaciones** | Si RabbitMQ falla tras guardar la póliza, se pierde el aviso sin rastro | Outbox: el evento nunca se pierde; si WhatsApp cae, los mensajes esperan y se reenvían |
| **Autenticación** | Un secreto simétrico que firma y verifica | RS256: solo identity firma; el resto verifica con la clave pública |
| **Autorización** | Casi todo abierto a cualquier usuario autenticado | Roles y propiedad del recurso en cada servicio, más validación en el gateway |
| **Proveedores externos** | Su caída deja funcionalidades sin respuesta | Circuit breaker, caché y alternativas (ingreso manual de placa, login por contraseña) |
| **Observabilidad** | Solo logs sueltos | Logs, métricas y trazas correlacionadas; un solo `correlationId` sigue una operación de extremo a extremo |
| **Equipos** | Todos tocan el mismo código | Servicios con dueños claros y contratos versionados |

### 15.2 Riesgos del análisis que se resuelven

| Hallazgo de [b_ANALISIS-RIESGOS-ARQUITECTURA.md](b_ANALISIS-RIESGOS-ARQUITECTURA.md) | Cómo queda resuelto | Fase |
|---|---|---|
| S1 Registro público con rol | Registro sin rol; alta de roles solo por ADMIN | 0 |
| S2 Autorización rota | Roles y propiedad en cada servicio y en el gateway | 0 y en cada servicio |
| S3 Secretos en el código | Variables de entorno y rotación | 0 |
| S4 MongoDB sin autenticación | Usuario por servicio y sin puerto público | 0 |
| S5 RabbitMQ con credenciales por defecto | Contraseñas fuertes y sin puertos públicos | 0 |
| S6 Tráfico sin cifrar | TLS delante del gateway y del frontend | 7 |
| S7 Cuenta demo conocida | Datos demo solo con perfil `dev` | 0 |
| S8 Sesión débil | Tokens cortos con renovación y revocación (a completar en la fase 7) | 2 y 7 |
| S9 Sin límite de intentos | Rate limiting en el gateway y en el login | 0 |
| S10 Swagger público | Solo en `dev` | 0 |
| S11 Sin cabeceras de seguridad | Cabeceras en Nginx y en el gateway | 7 |
| A1 Sin Outbox | Outbox en todos los servicios que publican | 0 y por servicio |
| A2 Base compartida | Una base por servicio y proyecciones locales | 1 a 6 |
| A3 Estado en memoria | Redis (state OAuth, tickets, MFA) | 2 |
| A4 `audit.queue` sin consumidor | Consumidor implementado o cola retirada | 7 |
| A5 Puntos únicos de fallo | Aislamiento por servicio, respaldos y alertas (la réplica de MongoDB y el clúster de RabbitMQ quedan como mejora futura) | 7 |
| A6 Dependencia de terceros | Circuit breaker, caché, pausa de consumo y reproceso de DLQ | 1, 2, 3 |

### 15.3 Lo que no se logra automáticamente (para no crear falsas expectativas)

- **No elimina la consistencia eventual:** un dato puede tardar unos segundos en verse en otro servicio. La interfaz debe tolerarlo.
- **No reduce la complejidad operativa:** pasa de 3 a unos 9 contenedores de aplicación más la infraestructura y la observabilidad. La plantilla y los paneles la contienen, pero no la eliminan.
- **No es alta disponibilidad completa:** con Docker Compose en una sola máquina, seguirá habiendo un punto único de fallo (la máquina). Para eso haría falta un orquestador con varios nodos (Kubernetes), que queda como evolución futura; los servicios ya quedan preparados para ello (sin estado, con *readiness*, configuración por entorno).
- **No corrige por sí sola errores de negocio** ni mejora el rendimiento de las consultas: solo permite escalar y aislar.

---

## 16. Resumen de entregables al terminar

- 6 microservicios + API Gateway + frontend, cada uno con su imagen, su pipeline y su base de datos.
- Un `infra/docker-compose.yml` y un `infra/docker-compose.observability.yml` que levantan todo el sistema.
- Módulo `contracts/` con las APIs y los eventos versionados, y pruebas de contrato en CI.
- Paneles y alertas en Grafana; trazas en Jaeger; logs en Loki, todos por `correlationId`.
- Documentación operativa (arranque, paneles, reproceso de DLQ, procedimientos de reversa).
- Un análisis de seguridad final que confirma el cierre de los hallazgos S1 a S11 y A1 a A6.
