# Impacto de la migración en el monolito

Este documento registra, fase por fase y commit por commit, qué se cambió en el monolito (`Arquitectura-Clean/`): qué se le **agregó** para la transición, qué se **desacopló** (se movió a un microservicio y se eliminó del monolito) y qué cambió en su **infraestructura** (Compose). Complementa los documentos de cada fase ([f](f_IMPLEMENTACION-NOTIFICATION-SERVICE-FASE1.md), [g](g_IMPLEMENTACION-IDENTITY-SERVICE-FASE2.md), [h](h_CIERRE-PENDIENTES.md), [i](i_IMPLEMENTACION-CUSTOMER-SERVICE-FASE3.md), [j](j_IMPLEMENTACION-CLAIMS-SERVICE-FASE4.md), [l](l_IMPLEMENTACION-QUOTATION-SERVICE-FASE5.md), [m](m_IMPLEMENTACION-POLICY-SERVICE-FASE6.md)).

**Línea base:** el commit `ae91f98` (2026-09-25) incorporó al repositorio el monolito tal como estaba antes de la migración. Todo lo que sigue se mide contra ese estado.

---

## 1. Resumen

| Fase | ¿Se tocó el código del monolito? | Qué se desacopló (salió del monolito) | Qué se le agregó para la transición |
|---|---|---|---|
| 0. Preparación y seguridad | Sí | — | Control de acceso por rol y propietario, trazas y logs JSON, datos demo y Swagger solo en desarrollo |
| 1. notification-service | Sí | Las colas de notificación (ahora las declara notification-service) | Outbox transaccional, eventos `customer.*`, endpoint de contacto, backfill, `X-Correlation-Id` |
| 2. identity-service | Sí | Login (contraseña, Google, Facebook), MFA, colección `usuarios`, firma de tokens HS256 | Validación de JWT RS256 por JWKS (resource server) |
| 3. customer-service | **No** | Clientes, vehículos y placas: desde el corte, el gateway los envía a customer-service | — |
| 4. claims-service | **No** | Siniestros: desde el corte, a claims-service | — |
| 5. quotation-service | **No** | Tarifas y cotizaciones: desde el corte, a quotation-service | — |
| 6. policy-service | **No** | Pólizas, renovaciones y "Mi cuenta": desde el corte, a policy-service y al gateway | — |

Desde la fase 3 el código del monolito se mantiene **congelado** por decisión del equipo (2026-09-27): los servicios nuevos se construyeron al lado, con su base propia y copias de datos. El 2026-09-27 se hizo **el corte conjunto de las fases 3 a 6 sin tocar el monolito** (sección 6): el desacoplamiento se logró en el gateway, no borrando código. El monolito sigue en el Compose, sin tráfico de negocio, como reversa.

---

## 2. Fase 0 — Preparación y seguridad

| Commit | Qué cambió en el monolito | Tipo |
|---|---|---|
| `07d884f` feat(security) | Reglas `@PreAuthorize` por rol en todos los controladores (`Roles.java`) y control de propietario (`AccesoRecursos`: un CLIENTE solo ve su cliente, sus vehículos, cotizaciones, pólizas y siniestros, según el `customerId` del token). Prueba `ControlAccesoPolizasTest` | Agregado (seguridad, riesgo S2) |
| `08f6660` chore(config) | `MongoDemoDataInitializer` solo con `APP_DEMO_DATA_ENABLED=true`; Swagger solo con `SWAGGER_ENABLED=true` | Cambio de configuración (riesgos S7, S10) |
| `7257098` chore(docker) | Solo Compose: MongoDB del backend con usuario propio (`andina`), keyFile y replica set; sin puertos en el host (MongoDB, RabbitMQ, backend). `docker-compose.debug.yml` los abre para depurar | Infraestructura (riesgos S4, S5) |
| `cc2024a` feat(backend) | Micrometer Tracing con exportación OTLP, `logback-spring.xml` con logs JSON (`traceId`, `spanId`, `correlationId`); el Outbox guarda el `traceparent` del evento | Agregado (observabilidad) |

**Qué no cambió:** el dominio (entidades, casos de uso de negocio) y las respuestas de la API.

---

## 3. Fase 1 — notification-service

| Commit | Qué cambió en el monolito | Tipo |
|---|---|---|
| `f7ad09c` feat(backend) | **Outbox transaccional:** colección `outbox`, `OutboxDomainEventPublisherAdapter`, `OutboxRelay` (publisher confirms, métricas de pendientes), `MongoTransaccionAdapter` y puerto `TransaccionPort`. `EmitirPolizaUseCase` guarda cotización, póliza y `policy.issued.v1` en una sola transacción (riesgo A1) | Agregado |
| `f7ad09c` | **Eventos de cliente:** `ClienteRegistradoEvent` y `ClienteActualizadoEvent` → `customer.registered.v1` y `customer.updated.v1` en el exchange nuevo `andina.events`, con `aggregateVersion` (campo `version` nuevo en `Cliente` y `ClienteDocument`) | Agregado (para alimentar proyecciones de otros servicios) |
| `f7ad09c` | Endpoints nuevos: `PATCH /api/clientes/{id}/contacto` (ADMIN, AGENTE) y backfill `POST /api/clientes/eventos/reenvio` (ADMIN) | Agregado |
| `f7ad09c` | `CorrelationIdFilter`: toma el `X-Correlation-Id` del gateway y el Outbox lo copia al evento | Agregado |
| `f7ad09c` | `RabbitMqConfiguration` deja de declarar las colas de notificación y su DLQ; `RabbitMqDomainEventPublisherAdapter`, `PolicyIssuedMessage` y su mapper se reemplazan por el Outbox | **Desacoplado** (las colas son de notification-service) |
| `f7d3b94` chore(docker) | Solo Compose: MongoDB del backend pasa a replica set de un nodo (necesario para las transacciones); el consumer ya no está en la red del backend | Infraestructura |

**Desacoplamiento logrado:** notification-service dejó de leer la colección `clientes` del monolito; ahora usa su proyección `customer_contacts`, alimentada por los eventos que el monolito publica.

---

## 4. Fase 2 — identity-service

| Commit | Qué cambió en el monolito | Tipo |
|---|---|---|
| `a74da8c` feat(identity) | Se elimina `JwtAuthenticationFilter` (lo reemplaza la validación por JWKS del commit siguiente) | Desacoplado |
| `8e2c927` feat(backend) | El monolito pasa a *resource server*: `JwtDecoderConfig` valida RS256 con el JWKS de identity-service (con ventana HS256 temporal). `SecurityConfig` toma el rol del claim `rol`. Ya no consulta `usuarios` en cada petición. `ObtenerMiCuentaUseCase` y `MiCuentaController` usan el `customerId` del token | Cambio |
| `71a66ac` refactor(backend) | **Se elimina del monolito:** `AuthController`, `MfaController`, los casos de uso `auth/*` y `mfa/*`, `Usuario` y `RolUsuario`, la colección `usuarios` (documento, repositorio, mapper), los adaptadores de Google, Facebook, TOTP, QR, BCrypt, AES-GCM y los `InMemory*`, el firmado HS256 (`JwtTokenAdapter`), las dependencias jjwt y zxing, y 35 pruebas. `MongoDemoDataInitializer` ya no crea el usuario demo | **Desacoplado** (78 archivos) |
| `f1363fc`, `71d50c1` chore(docker) | Solo Compose: el backend entra a la red de servicios (descarga del JWKS) y se elimina `JWT_SECRET` y la configuración de login del backend | Infraestructura |

**Desacoplamiento logrado:** los usuarios, el login y el MFA viven solo en identity-service. La colección `usuarios` del monolito quedó intacta como plan de reversa, pero ningún código la usa.

---

## 5. Fases 3 a 6 — sin cambios en el código del monolito

En estas fases **no se modificó ningún archivo de `Arquitectura-Clean/src`, `pom.xml` ni `Dockerfile`**. Solo se agregaron entradas nuevas a `Arquitectura-Clean/docker-compose.yml` y a `.env.example` (customer-service, claims-service, quotation-service, sus MongoDB y el simulador `jsonpe-mock`), sin cambiar las del monolito.

| Fase | Situación actual | Qué lee el servicio nuevo del monolito |
|---|---|---|
| 3. customer-service | Corre en paralelo. El gateway sigue enviando `/api/clientes/**` y `/api/vehiculos/**` al monolito, que sigue publicando `customer.*` | Nada en tiempo de ejecución. El script `migrar-clientes.sh` copia `clientes` y `vehiculos` a `customer_db` (solo lectura sobre la base del monolito) |
| 4. claims-service | Corre en paralelo. El gateway sigue enviando `/api/polizas/{id}/siniestros/**` al monolito | Consume `policy.issued.v1`, que el monolito ya publicaba. El script `migrar-siniestros.sh` copia `siniestros` y hace la carga inicial de `policy_ref` desde `polizas` (solo lectura) |
| 5. quotation-service | Corre en paralelo. El gateway sigue enviando `/api/cotizaciones/**` y `/api/tablas-tarifarias/**` al monolito | Consume `customer.*` y `policy.issued.v1`, que el monolito ya publicaba. El script `migrar-cotizaciones.sh` copia `tablas_tarifarias` y `cotizaciones` y hace la carga inicial de `customer_ref` y `vehicle_ref` desde `clientes` y `vehiculos` (solo lectura) |

### 5.1 Qué habría que cambiar en el monolito para hacer el corte

Estos cambios siguen la ruta ([d_RUTA](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md)) y **no se han hecho**. Se listan para que el equipo decida cuándo aplicarlos.

| Fase | Cambio en el monolito | Por qué hace falta |
|---|---|---|
| 3 (paso 3.5) | Dejar de publicar `customer.*`: quitar `CrearClienteUseCase`, `ActualizarContactoClienteUseCase`, `PublicarClientesExistentesUseCase`, `ClienteController`, `VehicleInformationController` y los adaptadores `jsonpe` | Que haya una sola fuente de los eventos de cliente (customer-service) |
| 3 | Cambiar `ClienteRepository` y `VehiculoRepository` para que lean proyecciones `customer_ref` y `vehicle_ref` alimentadas por `customer.*` y `vehicle.registered.v1` (con un consumidor y su DLQ) | El monolito todavía cotiza (`CrearCotizacionUseCase` necesita edad del cliente y tipo, uso y año del vehículo) y arma "Mi cuenta". Sin esto no conocería los clientes creados en customer-service |
| 3 | Quitar la carga demo de clientes y vehículos de `MongoDemoDataInitializer` | La hace customer-service |
| 4 (paso 4.1) | Publicar `policy.renewed.v1`, `policy.expired.v1` y `policy.cancelled.v1` | Hoy `policy_ref` solo se entera de las pólizas nuevas; los cambios de estado se resincronizan repitiendo `migrar-siniestros.sh` |
| 4 (paso 4.8) | Consumir `claim.*` y mantener un contador de siniestros abiertos por póliza; `EvaluarRenovacionUseCase` lo usaría en lugar de `SiniestroRepository` | La renovación bloquea si hay siniestros pendientes; sin esto seguiría leyendo `siniestros` de su propia base |
| 4 | Quitar `SiniestroController`, los casos de uso `siniestro/*` y la colección `siniestros` | Que claims-service sea el único dueño de los siniestros |
| 5 (acuerdo con la fase 6) | Cambiar `EmitirPolizaUseCase` para que lea una proyección `accepted_quotes` alimentada por `quote.accepted.v1`, en lugar de la colección `cotizaciones` | Si las cotizaciones se mueven antes que las pólizas, el monolito necesita saber qué cotización se aceptó para emitir. La alternativa es mover cotizaciones y pólizas en el mismo corte (fase 6) |
| 5 | Quitar `CotizacionController`, `TablaTarifariaController`, los casos de uso `cotizacion/*` y `tarifa/*`, `MotorDeTarificacion` y sus colecciones | Que quotation-service sea el único dueño de tarifas y cotizaciones |

**Alternativa sin tocar el monolito (la que se eligió):** esperar a la fase 6. Cuando policy-service reemplace pólizas y renovaciones, publicará `policy.*` y consumirá `claim.*` él mismo, y el monolito se apaga sin haber necesitado estos cambios intermedios.

---

## 6. Corte de las fases 3 a 6 (2026-09-27) — sin cambios en el monolito

Con las cuatro fases implementadas, ninguna función del negocio depende ya del monolito, así que el corte se hizo **solo en el gateway y en la configuración** (detalle en [m, sección 7](m_IMPLEMENTACION-POLICY-SERVICE-FASE6.md#7-corte-de-las-fases-3-a-6-2026-09-27)):

| Qué | Efecto sobre el monolito |
|---|---|
| El gateway envía clientes, vehículos, cotizaciones, tarifas, siniestros, pólizas y renovaciones a los servicios, y compone "Mi cuenta" | El monolito deja de recibir tráfico de negocio (verificado: 0 líneas de log con el `correlationId` de un flujo completo) |
| Los servicios publican `customer.*`, `vehicle.registered`, `claim.*`, `quote.accepted` y `policy.*` | El monolito ya no es la fuente de ningún evento en la práctica: no recibe las peticiones que los generaban |
| Código (`src`, `pom.xml`, `Dockerfile`) | **Sin cambios** |
| Base del monolito | Intacta; se leyó con los scripts de migración justo antes del corte |

Pendiente (paso 6.10): retirar `backend` del Compose y respaldar `andina_clean_mongo_data`. Mientras siga, sirve de reversa: basta con volver a apuntar las URL de los servicios del gateway a `http://backend:8080` (las escrituras hechas después del corte habría que copiarlas al monolito).
