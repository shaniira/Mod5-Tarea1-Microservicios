# Implementación de customer-service (fase 3 de la migración a microservicios)

> **Nota (2026-09-28):** el monolito se retiró del repositorio. Las rutas `Arquitectura-Clean/...`, el contenedor `andina-clean-mongodb` y los comandos `cd Arquitectura-Clean` de este documento describen el estado de su momento: hoy el Compose y el `.env` están en la raíz y el código del monolito queda en la etiqueta de git `monolito-final`. Ver [q_RETIRO-DEL-MONOLITO.md](q_RETIRO-DEL-MONOLITO.md).

Este documento registra lo que se implementó en la fase 3 de la [ruta de implementación](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#7-fase-3--customer-service), las decisiones tomadas, el impacto en el monolito y la verificación contra el stack de Docker.

**Resultado:** clientes, vehículos y la consulta de placas viven en `services/customer-service`, con base propia (`customer_db`), Outbox, caché de placas en Redis y los patrones de resiliencia hacia JSON.pe. El servicio corre **en paralelo al monolito, sin enrutar**: el gateway sigue enviando `/api/clientes/**` y `/api/vehiculos/**` al monolito, que sigue siendo la fuente de verdad. El corte (cambiar la ruta y activar la publicación de eventos) queda preparado pero no se hizo, porque requiere cambiar el código del monolito y el equipo decidió mantenerlo congelado (ver sección 3).


> **Actualización (2026-09-27): corte hecho.** El gateway ya envía este dominio al servicio, que publica sus eventos; el monolito quedó sin tráfico de negocio y su código sin cambios. Detalle y verificación en [m_IMPLEMENTACION-POLICY-SERVICE-FASE6.md, sección 7](m_IMPLEMENTACION-POLICY-SERVICE-FASE6.md#7-corte-de-las-fases-3-a-6-2026-09-27). Lo que sigue describe el trabajo previo al corte.

---

## 1. Qué se hizo, paso por paso

| Paso de la ruta | Implementación | Estado |
|---|---|---|
| 3.1 Crear customer-service con la plantilla | Estructura Clean (`entities`, `usecases`, `interfaceadapters`, `frameworksdrivers`) con 8 reglas ArchUnit. Se trajeron `Cliente`, `Vehiculo`, `Placa`, los casos de uso `cliente/*` (crear, listar, obtener, actualizar contacto, backfill) y `vehiculo/*`, `ConsultarInformacionVehiculoService` y los adaptadores `jsonpe`. Mismas rutas, reglas de acceso por rol y propietario, respuestas y formato de error que el monolito, así que el frontend no cambiaría | ✅ |
| 3.2 Consulta de placas resiliente | Timeout de 5 s (RestClient), circuit breaker `jsonpe`, un reintento con espera aleatoria y exponencial, bulkhead de 10 llamadas y **caché en Redis** (base 2, clave `placa:<PLACA>`, TTL 24 h). Solo las caídas del proveedor cuentan para el circuito y se reintentan; un token rechazado no. Si falla y no hay caché, responde `SIN_DATOS` y el usuario completa los datos a mano (igual que el monolito). Si Redis falla, la caché se comporta como vacía | ✅ |
| 3.3 Publicar `customer.*` y `vehicle.registered` (Outbox) | Outbox transaccional (replica set de un nodo). `customer.registered/updated.v1` con el mismo formato que publicaba el monolito más el campo opcional `birthDate`; `vehicle.registered.v1` nuevo, con tipo, uso y año (lo que necesita la tarificación). Contratos en `contracts/events` | ✅ (publicación apagada hasta el corte) |
| 3.4 Migrar `clientes` y `vehiculos` y backfill | `migracion/migrar-clientes.sh` copia las dos colecciones a `customer_db` y compara cantidad y huella SHA-256. El backfill `POST /api/clientes/eventos/reenvio` ahora publica también los vehículos | ✅ |
| 3.5 Cambio de fuente de los eventos | Interruptor `CUSTOMER_EVENTS_PUBLISH_ENABLED` (en Compose: `false`). Con `false` los eventos quedan en el Outbox; se activa en el mismo momento que se cambia la ruta | ✅ Hecho en el corte (2026-09-27, `m_…` sección 7): interruptor en `true` |
| 3.6 Rutas en el gateway | No se cambiaron (decisión "sin enrutar"). La ruta a agregar está en la sección 3.2 | ✅ Hecho en el corte (2026-09-27, `m_…` sección 7) |
| 3.7 "Mi cuenta" como composición en el gateway | No se hizo: mientras clientes y pólizas sigan en el monolito, "Mi cuenta" sigue ahí. Se hará cuando exista policy-service (fase 6) | ✅ Hecho en el corte: el gateway compone customer y policy |

Además:

- **Lectura de refuerzo para quotation (sección 5.4 de la propuesta):** `GET /api/clientes/{id}/vehiculos/{vehiculoId}` (un vehículo de otro cliente responde 404).
- **Infraestructura:** `customer-mongodb` (replica set con autenticación, usuario `customer` solo sobre `customer_db`) y `customer-service` en `Arquitectura-Clean/docker-compose.yml`, sin puertos en el host. Redis se conecta a la red `customer_data_network`.
- **CI:** `.github/workflows/customer-service.yml`.
- **Kubernetes:** manifiestos `k8s/70` a `73` escritos (ConfigMap, Secret de ejemplo, Deployment, Service). No se aplicaron en un clúster.

## 2. Decisiones

| Decisión | Motivo |
|---|---|
| **Correr en paralelo, sin enrutar** | El equipo decidió no modificar el código del monolito. Si el gateway enviara los clientes a customer-service, el monolito (que todavía cotiza y arma "Mi cuenta") no conocería los clientes nuevos. Se construye y verifica todo lo que no depende del monolito y el corte se deja listo |
| **Interruptor de publicación** | Evita tener dos fuentes de `customer.*` mientras el monolito siga publicando. Es el "interruptor único" del paso 3.5 |
| **`birthDate` opcional en `customer.*`** | La tarificación calcula la edad del conductor; sin la fecha de nacimiento, quotation (o el monolito, si se hace el corte) no podría cotizar desde la proyección. Es un campo opcional nuevo: compatible hacia atrás |
| **`vehicle.registered.v1` sin `valor`** | La propuesta lo mencionaba, pero el vehículo no tiene ese dato en el dominio. Se publican los campos reales (placa, marca, modelo, año, tipo, uso, zona) |
| **Caché primero, luego proveedor** | Una placa ya consultada responde sin llamar a JSON.pe, aunque esté caído, y ahorra llamadas pagadas |
| **Replica set para `customer-mongodb`** | Mismo arranque que el MongoDB del backend (`infra/mongo/arranque.sh`): el cliente y su evento se guardan en una sola transacción |
| **Sin datos demo en Compose** | Los clientes demo vienen de la migración desde el monolito, con los mismos IDs. El inicializador existe (`APP_DEMO_DATA_ENABLED`) para un entorno sin monolito |
| **Nombres sin prefijo de la empresa** | Imagen `customer-service:1.0.0`, contenedores `customer-service` y `customer-mongodb`, volumen `customer_mongo_data`, red `customer_data_network` |

## 3. Impacto en el monolito

**No se modificó el código del monolito** (`Arquitectura-Clean/src`, `pom.xml`, `Dockerfile`). En `Arquitectura-Clean/docker-compose.yml` y `.env.example` solo se agregaron entradas nuevas. Detalle de todas las fases en [k_IMPACTO-EN-EL-MONOLITO.md](k_IMPACTO-EN-EL-MONOLITO.md).

### 3.1 Qué sigue haciendo el monolito

| Funcionalidad | Dónde está hoy |
|---|---|
| `/api/clientes/**` y `/api/vehiculos/**` (lo que usa el frontend) | Monolito |
| Publicar `customer.*` para notification e identity | Monolito |
| Cotizar (edad del cliente; tipo, uso y antigüedad del vehículo) y "Mi cuenta" | Monolito, leyendo sus colecciones `clientes` y `vehiculos` |

### 3.2 Pasos del corte

1. Repetir `sh services/customer-service/migracion/migrar-clientes.sh` (deja `customer_db` al día y compara).
2. En el monolito: agregar el consumidor de `customer.*` y `vehicle.registered.v1` con proyecciones `customer_ref` y `vehicle_ref`, y quitar la creación de clientes y vehículos y la publicación de `customer.*` (lista completa en [k, sección 5.1](k_IMPACTO-EN-EL-MONOLITO.md#51-qué-habría-que-cambiar-en-el-monolito-para-hacer-el-corte)).
3. En el gateway, antes de la ruta `backend`:
   ```yaml
   - id: customer
     uri: ${CUSTOMER_SERVICE_URL:http://localhost:8085}
     predicates:
       - Path=/api/clientes/**,/api/vehiculos/**
     filters:
       - name: CircuitBreaker
         args: { name: customerCB, fallbackUri: forward:/fallback/backend }
   ```
   con `customerCB` y un `timelimiter` de 12 s (la consulta de placas puede tardar hasta dos intentos de 5 s).
4. `CUSTOMER_EVENTS_PUBLISH_ENABLED=true` y reiniciar customer-service. Descartar antes los eventos PENDING del Outbox que se hayan generado en pruebas.
5. `POST /api/clientes/eventos/reenvio` (ADMIN) para poblar las proyecciones del monolito.

**Reversa:** volver la ruta al monolito y apagar la publicación en customer-service. Las colecciones del monolito no se borran.

## 4. Verificación

Pruebas hechas el 2026-09-27 contra el stack de `Arquitectura-Clean/docker-compose.yml`. Como el servicio no está enrutado, se le llamó por la red interna (`services_network`) con el mismo token RS256 que emite identity-service; al monolito, por el gateway. JSON.pe se simuló con WireMock (`--profile jsonpe-mock`), porque no hay un token real en `.env`.

| # | Criterio de salida (ruta, sección 7) | Cómo se probó | Resultado |
|---|---|---|---|
| 1 | Ningún servicio consulta `clientes` ni `vehiculos` fuera de customer-service | Requiere el corte: el monolito sigue siendo la fuente y lee sus colecciones | ✅ Hecho en el corte (2026-09-27, `m_…` sección 7) |
| 2 | Con JSON.pe caído, se puede registrar un vehículo de forma manual y las placas ya consultadas siguen respondiendo desde la caché | Con el simulador arriba, `ABC123` devolvió datos (`fuente: JSON_PE`) y quedó en Redis con TTL de 86 399 s. Con el simulador detenido: `abc-123` respondió los mismos datos desde la caché; `XYZ789` respondió `SIN_DATOS` ("complete el registro manualmente") y el log registró `JSON.pe falló al consultar la placa XYZ789: JSONPE_UNAVAILABLE`; el registro manual del vehículo `XYZ-789` respondió **201** | ✅ Cumple |
| 3 | Las proyecciones de notification e identity coinciden con los conteos de `customer_db` | Requiere el corte (hoy las alimenta el monolito) | ✅ Verificado en el corte (15 = 15) y por la reconciliación de la fase 7 |

Pruebas adicionales:

| Prueba | Resultado |
|---|---|
| Migración (`migrar-clientes.sh`) | 13 clientes y 13 vehículos; misma huella SHA-256 en las dos bases |
| Paridad con el monolito | `GET /api/clientes` por el gateway (monolito) y directo a customer-service: **idénticos** (13 clientes). `GET /api/clientes/{id}/vehiculos` para los 13 clientes: **0 diferencias** |
| Outbox con la publicación apagada | El vehículo registrado dejó `vehicle.registered.v1` en el Outbox como `PENDING`; no se publicó nada |
| Pruebas automáticas | 38 (8 reglas ArchUnit, control de acceso por rol y propietario, eventos, backfill, caché, resiliencia de JSON.pe con reintento, circuit breaker y bulkhead). Corren al construir la imagen |
| Error encontrado y corregido | Sobre `http://` el cliente HTTP del JDK pedía subir a HTTP/2 (`Upgrade: h2c`) y la respuesta del simulador se perdía, así que toda consulta terminaba en `SIN_DATOS` sin dejar rastro. Se fijó HTTP/1.1 y se agregó un `WARN` con el motivo cuando el proveedor falla |

### 4.1 Errores encontrados y cómo se resolvieron

| # | Error o problema | Causa | Solución | Cómo se verificó |
|---|---|---|---|---|
| 1 | Toda consulta de placa contra el JSON.pe simulado respondía `SIN_DATOS`, sin dejar rastro en el log | Sobre `http://` el cliente HTTP del JDK pedía pasar a HTTP/2 (`Upgrade: h2c`) y la respuesta del simulador se perdía; además, la falla del proveedor no se registraba | Cliente fijado en HTTP/1.1 y un `WARN` con el motivo cuando el proveedor falla | `ABC123` devolvió datos (`fuente: JSON_PE`) y quedó en la caché de Redis; con el simulador detenido, el log muestra `JSONPE_UNAVAILABLE` |
| 2 | Quedó un volumen huérfano `andina_customer_mongo_data` | Al quitar el prefijo de la empresa a los nombres nuevos, el volumen pasó a llamarse `customer_mongo_data` y el anterior quedó sin uso | No se borró: borrar datos lo decide el dueño del entorno. No afecta al stack, que usa `customer_mongo_data` | `docker volume ls` muestra los dos; el servicio usa el nuevo |
| 3 | (Descubierto en la fase 7) Un id que no es UUID o un JSON mal formado respondía 500 | El manejador de errores no distinguía los errores de formato de la petición | 400 `SOLICITUD_MAL_FORMADA` en todos los servicios (commit `3863bd9`, `n_…`) | Pruebas de los manejadores de errores y caso manual por el gateway |

## 5. Limitaciones conocidas

| Punto | Detalle |
|---|---|
| **Criterios de salida pendientes del corte** | "Ningún servicio consulta `clientes` ni `vehiculos` fuera de customer-service" y "las proyecciones de notification e identity coinciden con `customer_db`" dependen de que el monolito deje de ser la fuente (sección 3.2). **Resuelto:** se cumplieron con el corte (2026-09-27) |
| **Datos en paralelo** | Mientras no haya corte, lo que se cree directamente en customer-service no llega al monolito, y lo que se cree en el monolito no llega a customer-service hasta repetir la migración |
| **Kubernetes** | Manifiestos escritos pero no aplicados |
| **Relay del Outbox** | Pensado para una réplica, igual que el del backend |

## 6. Archivos

| Área | Archivos |
|---|---|
| Servicio | `services/customer-service/**` |
| Migración | `services/customer-service/migracion/migrar-clientes.sh` |
| Contratos | `contracts/events/vehicle.registered.v1.schema.json` (nuevo), `customer.registered/updated.v1.schema.json` (`birthDate`), `contracts/README.md` |
| Infraestructura | `Arquitectura-Clean/docker-compose.yml`, `Arquitectura-Clean/.env.example` |
| Kubernetes | `k8s/70-configmap-customer.yaml`, `71-secret-customer.example.yaml`, `72-deployment-customer.yaml`, `73-service-customer.yaml`, `33-secret-mongodb.example.yaml` (usuario `customer`) |
| CI | `.github/workflows/customer-service.yml` |
