# Implementación de quotation-service (fase 5 de la migración a microservicios)

> **Nota (2026-09-28):** el monolito se retiró del repositorio. Las rutas `Arquitectura-Clean/...`, el contenedor `andina-clean-mongodb` y los comandos `cd Arquitectura-Clean` de este documento describen el estado de su momento: hoy el Compose y el `.env` están en la raíz y el código del monolito queda en la etiqueta de git `monolito-final`. Ver [q_RETIRO-DEL-MONOLITO.md](q_RETIRO-DEL-MONOLITO.md).

Este documento registra lo que se implementó en la fase 5 de la [ruta de implementación](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#9-fase-5--quotation-service), las decisiones, el impacto en el monolito y la verificación contra el stack de Docker.

**Resultado:** las tablas tarifarias, el motor de tarificación y las cotizaciones viven en `services/quotation-service`, con base propia (`quotation_db`), las proyecciones `customer_ref` y `vehicle_ref`, lectura de refuerzo hacia customer-service con circuit breaker, y `quote.accepted.v1` por Outbox. Como en las fases 3 y 4, el servicio corre **en paralelo al monolito, sin enrutar**, y el código del monolito no se modificó. La tarificación da **exactamente el mismo resultado** que el monolito en los 39 casos de referencia probados.


> **Actualización (2026-09-27): corte hecho.** El gateway ya envía este dominio al servicio, que publica sus eventos; el monolito quedó sin tráfico de negocio y su código sin cambios. Detalle y verificación en [m_IMPLEMENTACION-POLICY-SERVICE-FASE6.md, sección 7](m_IMPLEMENTACION-POLICY-SERVICE-FASE6.md#7-corte-de-las-fases-3-a-6-2026-09-27). Lo que sigue describe el trabajo previo al corte.

---

## 1. Qué se hizo, paso por paso

| Paso de la ruta | Implementación | Estado |
|---|---|---|
| 5.1 Crear quotation-service con la plantilla | Estructura Clean con 8 reglas ArchUnit. Se trajeron `tarifa/*`, `cotizacion/*`, `MotorDeTarificacion`, `FactorRiesgo`, `TablaTarifaria`, `ResultadoTarificacion`, `Cotizacion`, `Dinero` y `PeriodoVigencia`. El motor es el mismo cálculo, paso por paso; solo cambia la entrada: recibe `ClienteRef` y `VehiculoRef` (edad y antigüedad calculadas igual que en el monolito). Mismas rutas, reglas de acceso, respuestas y errores | ✅ |
| 5.2 Proyecciones `customer_ref` y `vehicle_ref` con backfill | Cola propia `quotation.customer.events` (con DLQ) para `customer.registered/updated.v1` y `vehicle.registered.v1`, con *upsert* por versión. Guardan lo mínimo: fecha de nacimiento y estado del cliente; dueño, tipo, uso y año del vehículo. La carga inicial la hace `migracion/migrar-cotizaciones.sh` | ✅ |
| 5.3 Lectura de refuerzo (*read-through*) | Si el cliente o el vehículo no están en la proyección (o al cliente le falta la fecha de nacimiento), se pide a customer-service (`GET /api/clientes/{id}` y `GET /api/clientes/{id}/vehiculos/{vehiculoId}`, este último agregado en la fase 3) con el token del usuario: timeout de 2 s, un reintento y circuit breaker `customer`. Si no responde: **503** `CLIENTES_NO_DISPONIBLE` con `Retry-After`. Lo que trae se guarda en la proyección | ✅ |
| 5.4 Publicar `quote.accepted.v1` y consumir `policy.issued.v1` | Aceptar guarda la cotización y el evento en una transacción (Outbox). El evento lleva cliente, vehículo, prima, moneda, validez, tabla aplicada y desglose. Cola `quotation.policy.events` (enlazada a los dos exchanges, con DLQ): `policy.issued.v1` marca la cotización EMITIDA (idempotente). Publicación apagada hasta el corte (`QUOTATION_EVENTS_PUBLISH_ENABLED=false`) | ✅ (publicación apagada hasta el corte) |
| 5.5 Migrar `tablas_tarifarias` y `cotizaciones` | `migrar-cotizaciones.sh` copia y compara (cantidad y huella SHA-256) | ✅ |
| 5.6 Rutas en el gateway | No se cambiaron (sin enrutar). La ruta a agregar está en la sección 3.2 | ✅ Hecho en el corte (2026-09-27, `m_…` sección 7) |
| 5.7 Pruebas del motor con resultado idéntico | Prueba unitaria del monolito portada (1417.02) más casos de prima mínima y siniestros; y prueba contra el stack con las mismas entradas en los dos sistemas (sección 4) | ✅ |

Además: `quotation-mongodb` y `quotation-service` en `Arquitectura-Clean/docker-compose.yml` (sin puertos en el host), contrato `contracts/events/quote.accepted.v1.schema.json` (acuerdo pendiente con la fase 6) y CI `.github/workflows/quotation-service.yml`. No se escribieron manifiestos de Kubernetes.

## 2. Decisiones

| Decisión | Motivo |
|---|---|
| **Correr en paralelo, sin enrutar** | Misma decisión que las fases 3 y 4. Además, el monolito emite las pólizas leyendo su colección `cotizaciones` (`EmitirPolizaUseCase`): si las cotizaciones se cotizaran en quotation-service, el monolito no podría emitirlas sin un cambio (ver sección 3) |
| **Motor con `ClienteRef` y `VehiculoRef`** | El cálculo no cambia, pero ya no depende de las entidades de otro dominio. La edad y la antigüedad se calculan con las mismas fórmulas que `Cliente.edad()` y `Vehiculo.antiguedad()` |
| **La fecha de nacimiento puede faltar en la proyección** | Los eventos `customer.*` que publica el monolito no la traen (customer-service sí). Una referencia sin fecha se completa con la lectura de refuerzo y el dato se guarda sin cambiar la versión |
| **Lectura de refuerzo con versión 0** | Lo que trae customer-service se guarda con versión 0: cualquier evento posterior lo reemplaza |
| **Pendientes de emisión por estado** | El monolito consultaba la base de pólizas; quotation usa el estado ACEPTADA, porque `policy.issued.v1` marca la cotización EMITIDA |
| **`quote.accepted.v1` "gordo"** | policy-service podrá emitir sin llamar a quotation (prima, validez y desglose incluidos). `aggregateVersion` es siempre 1: una cotización se acepta una sola vez |
| **Factores solo dentro de la tabla** | El monolito también los copiaba a la colección `factores_riesgo`, que ningún código lee; no se migra |
| **Nombres sin prefijo de la empresa** | Imagen `quotation-service:1.0.0`, contenedores `quotation-service` y `quotation-mongodb`, volumen `quotation_mongo_data`, red `quotation_data_network` |

## 3. Impacto en el monolito

**No se modificó el código del monolito.** En `Arquitectura-Clean/docker-compose.yml` y `.env.example` solo se agregaron entradas nuevas. quotation-service consume `customer.*` y `policy.issued.v1` con colas propias, sin cambiar lo que publica el monolito. Detalle de todas las fases en [k_IMPACTO-EN-EL-MONOLITO.md](k_IMPACTO-EN-EL-MONOLITO.md).

### 3.1 Qué sigue haciendo el monolito

| Funcionalidad | Dónde está hoy |
|---|---|
| `/api/cotizaciones/**` y `/api/tablas-tarifarias/**` (lo que usa el frontend) | Monolito |
| Emitir la póliza de una cotización aceptada | Monolito, leyendo su colección `cotizaciones` |

### 3.2 Pasos del corte (acuerdo con la fase 6)

La ruta lo advierte: si quotation sale antes que policy-service, el monolito necesita saber qué cotizaciones se aceptaron. Hay dos caminos:

1. **Corte antes de la fase 6:** en el monolito, una proyección `accepted_quotes` alimentada por `quote.accepted.v1` y `EmitirPolizaUseCase` leyendo de ella (cambio en el monolito). Después, repetir `migrar-cotizaciones.sh`, agregar la ruta en el gateway y activar `QUOTATION_EVENTS_PUBLISH_ENABLED`.
2. **Corte junto con la fase 6 (recomendado con el monolito congelado):** policy-service nace consumiendo `quote.accepted.v1`; en un mismo corte se enrutan cotizaciones y pólizas a los servicios nuevos y el monolito deja de participar en la emisión.

Ruta del gateway, antes de la ruta `backend`:
```yaml
- id: quotation
  uri: ${QUOTATION_SERVICE_URL:http://localhost:8087}
  predicates:
    - Path=/api/cotizaciones/**,/api/tablas-tarifarias/**
  filters:
    - name: CircuitBreaker
      args: { name: quotationCB, fallbackUri: forward:/fallback/backend }
```

**Reversa:** volver la ruta al monolito; sus colecciones no se borran.

## 4. Verificación

Pruebas hechas el 2026-09-27 contra el stack de Compose. quotation-service se llamó por la red interna (no está enrutado) con el token RS256 de identity-service; el monolito, por el gateway.

| # | Criterio de salida (ruta, sección 9) | Cómo se probó | Resultado |
|---|---|---|---|
| 1 | Se puede cotizar con customer-service caído si el cliente y el vehículo ya están en las proyecciones | Con customer-service **detenido**: cotizar a Ana con DEM-001 (de la carga inicial) respondió **201**. Un cliente recién creado en el monolito llegó a `customer_ref` en vivo por `customer.registered.v1`, pero sin fecha de nacimiento; con customer-service caído respondió **503** `CLIENTES_NO_DISPONIBLE` con `Retry-After`, sin error interno. Con customer-service arriba, la lectura de refuerzo completó la fecha y trajo el vehículo (201); al volver a detener customer-service, ese mismo cliente se cotizó desde la proyección (201) | ✅ Cumple |
| 2 | El resultado de la tarificación es idéntico al del monolito para un conjunto de casos de referencia | Para cada uno de los 13 vehículos del monolito y 3 escenarios (sin siniestros; 1 siniestro con 10 % de descuento; 3 siniestros con gastos y recargo distintos, que activan la prima mínima) se cotizó con las mismas entradas en el monolito y en quotation-service y se comparó prima, moneda, estado y **desglose completo** (primas, gastos, recargos, descuentos y factores aplicados): **39 de 39 idénticas** | ✅ Cumple |
| 3 | Aceptar una cotización genera `quote.accepted.v1` en el Outbox | Aceptar respondió 200 (ACEPTADA) y el Outbox quedó con `quote.accepted.v1` `PENDING` para esa cotización (la publicación está apagada hasta el corte) | ✅ Cumple |

Pruebas adicionales:

| Prueba | Resultado |
|---|---|
| Migración (`migrar-cotizaciones.sh`) | 3 tablas y 18 cotizaciones con la misma huella SHA-256; `customer_ref` y `vehicle_ref` con 13 entradas cada una |
| Paridad de lecturas | `GET /api/cotizaciones`, `/api/tablas-tarifarias`, `/api/cotizaciones/pendientes-emision`, una tabla con sus factores y una cotización con su desglose: **idénticos** en el monolito y en quotation-service |
| `policy.issued.v1` marca EMITIDA | Se publicó un `policy.issued.v1` de prueba solo en la cola de quotation (para no afectar a otros consumidores): la cotización pasó a EMITIDA; el mismo evento repetido se registró como `YA_EMITIDA`. Colas y DLQ en 0 |
| Pruebas automáticas | 36 (8 reglas ArchUnit, motor con el caso del monolito y casos de prima mínima, proyecciones y lectura de refuerzo, 503 controlado, `quote.accepted.v1`, idempotencia de EMITIDA, control de acceso). Corren al construir la imagen |
| Error encontrado y corregido | Una referencia que llegaba sin fecha de nacimiento se completaba con customer-service pero el dato no se guardaba (la versión 0 era menor que la 1 del evento), así que cada cotización de ese cliente volvía a llamar a customer-service. Se agregó `completarFechaNacimiento`, que guarda solo ese dato sin tocar la versión |

### 4.1 Errores encontrados y cómo se resolvieron

| # | Error o problema | Causa | Solución | Cómo se verificó |
|---|---|---|---|---|
| 1 | Cada cotización de un cliente sin fecha de nacimiento volvía a llamar a customer-service | La lectura de refuerzo traía la fecha, pero el dato no se guardaba: la versión de la referencia (0) era menor que la del evento (1) y el guardado condicional lo descartaba | `completarFechaNacimiento` guarda solo ese dato, sin tocar la versión | Con customer-service detenido, ese mismo cliente se cotizó desde la proyección (201) |
| 2 | Una prueba del motor de tarificación fallaba | El valor esperado de la prueba estaba mal calculado (1413); lo correcto es 1250 + 10 % de gastos + 3 % de recargo = **1412.50** | Se corrigió el valor esperado de la prueba; el motor estaba bien | 39 de 39 cotizaciones idénticas al monolito |
| 3 | (Descubierto en la fase 7) `quote.accepted.v1` salía con fechas sin zona horaria | El contrato pide RFC 3339 y el servicio serializaba `LocalDateTime` | Las fechas se publican en UTC (`...Z`); policy-service las sigue leyendo (`b90436f`) | Pruebas de contrato con JSON Schema, obligatorias en CI |
| 4 | (Retiro del monolito, 2026-09-28) Una instalación nueva no podía cotizar | Las tablas tarifarias solo llegaban con la migración desde la base del monolito | `DemoDataInitializer` siembra las 3 tablas demo (mismos ids) si no existen, con `APP_DEMO_DATA_ENABLED=true` en Compose (`q_…`) | `DemoDataInitializerTest` (2 pruebas) y el log "Tablas tarifarias demo listas" al arrancar |

## 5. Limitaciones conocidas

| Punto | Detalle |
|---|---|
| **Fecha de nacimiento en eventos del monolito** | Mientras el monolito publique `customer.*`, los clientes nuevos llegan sin fecha de nacimiento y la primera cotización necesita a customer-service (lectura de refuerzo) |
| **Vehículos nuevos del monolito** | El monolito no publica `vehicle.registered.v1`; un vehículo nuevo se obtiene por la lectura de refuerzo o repitiendo la migración |
| **Datos en paralelo** | Una cotización creada en quotation-service no existe en el monolito, y viceversa, hasta el corte |
| **Emisión** | Depende del camino elegido en la sección 3.2 |

## 6. Archivos

| Área | Archivos |
|---|---|
| Servicio | `services/quotation-service/**` |
| Migración | `services/quotation-service/migracion/migrar-cotizaciones.sh` |
| Contratos | `contracts/events/quote.accepted.v1.schema.json`, `contracts/README.md` |
| Infraestructura | `Arquitectura-Clean/docker-compose.yml`, `Arquitectura-Clean/.env.example` |
| CI | `.github/workflows/quotation-service.yml` |
