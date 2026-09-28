# Backend Seguros — de monolito a microservicios

Sistema de seguros vehiculares (clientes, cotizaciones, pólizas, siniestros y renovaciones) migrado de un **monolito Clean Architecture** a **microservicios**, con el patrón **Strangler Fig**: el API Gateway se puso delante del monolito y cada capacidad del negocio se fue extrayendo a su propio servicio, con su propia base de datos, hasta que el monolito dejó de recibir tráfico (2026-09-27) y se retiró del repositorio (2026-09-28).

![Arquitectura de microservicios](doc/5.%20Microservicios/c_DIAGRAMA-ARQUITECTURA-MICROSERVICIOS.png)

## Qué hay en el sistema

| Pieza | Responsabilidad | Base propia | Eventos que publica |
|---|---|---|---|
| **frontend** (Vue 3.5 + TypeScript) | Interfaz web; llama solo al gateway | — | — |
| **gateway** (Spring Cloud Gateway) | Único punto de entrada: rutas, JWT RS256 por JWKS, revocación (Redis + copia local), límite de peticiones, circuit breaker y timeout por servicio, "Mi cuenta" (composición) | Redis | — |
| **identity-service** | Usuarios, login con contraseña, Google y Facebook, MFA, emisión de JWT RS256 | `identity_db` | — |
| **customer-service** | Clientes, vehículos, consulta de placas (JSON.pe con caché) | `customer_db` | `customer.registered`, `customer.updated`, `vehicle.registered` |
| **quotation-service** | Tablas tarifarias, motor de tarificación, cotizaciones | `quotation_db` | `quote.accepted` |
| **policy-service** | Pólizas, **Saga de emisión** con compensación, renovaciones | `policy_db` | `policy.issued`, `policy.renewed`, `policy.issuance-rejected` |
| **claims-service** | Siniestros | `claims_db` | `claim.registered`, `claim.status-changed` |
| **notification-service** | WhatsApp cuando se emite una póliza | `notification_db` | — |

Los servicios se comunican por **eventos** en RabbitMQ (exchange `andina.events`, una cola y una DLQ por consumidor) y, solo donde el negocio exige el dato al día, por **HTTP con resiliencia** (quotation → customer, policy → claims al renovar).

## Patrones aplicados

- **Database per service:** ningún servicio lee la base de otro; los datos ajenos se copian con eventos (proyecciones con `aggregateVersion`).
- **Transactional Outbox** para publicar e **inbox/idempotencia** para consumir; relay con turno entre réplicas (lease en MongoDB).
- **Saga** de emisión de póliza (coreografía): `quote.accepted` → `policy.issued` o `policy.issuance-rejected` (compensación), con índice único para que una cotización genere una sola póliza.
- **Resiliencia:** circuit breaker, reintentos, timeouts y bulkhead (Resilience4j); degradación planificada con 503 y `Retry-After`.
- **Consistencia elegida por operación (CAP):** renovar es CP (se confirma con claims); la revocación de tokens es AP (copia local si Redis cae).
- **Seguridad:** JWT RS256 firmado solo por identity, validado en cada servicio con rol y propietario; secretos por variables de entorno.
- **Observabilidad:** `correlationId` y trazas de punta a punta (OpenTelemetry, Jaeger), logs JSON (Loki), métricas y alertas (Prometheus, Grafana, Alertmanager por correo).
- **Contratos primero:** 11 esquemas de eventos y 5 OpenAPI en [contracts/](contracts/), verificados en CI.

## Cómo levantarlo

Requisitos: Docker con Compose. Todo se ejecuta desde la raíz del repositorio.

```bash
cp .env.example .env                 # completar las claves (nunca se versiona)
docker compose up -d --build         # frontend :5173, gateway :8080
docker compose -f docker-compose.yml -f infra/observability/docker-compose.observability.yml up -d
                                     # + Grafana :3000, Jaeger :16686, Prometheus :9090, Alertmanager :9093
```

Una instalación nueva arranca con datos demo (usuario `admin` / `Admin123*`, clientes, vehículos y tablas tarifarias). Detalle, perfiles de prueba (WhatsApp y JSON.pe simulados, HTTPS local) y operación: [DOCKER-EJECUCION.md](DOCKER-EJECUCION.md) y la [guía de operación](doc/5.%20Microservicios/o_GUIA-OPERACION.md).

## Cómo se comprobó

- **272 pruebas automáticas** (identity 62, policy 52, quotation 48, customer 43, claims 33, notification 30, gateway 4), incluidas reglas de arquitectura (ArchUnit) y pruebas de contrato; corren en CI (8 pipelines) y al construir cada imagen.
- **Pruebas de caos** (`infra/operacion/caos.sh`): se apaga cada servicio, RabbitMQ, Redis o una base, y se comprueba la degradación y que no se pierde ningún evento.
- **Reconciliación** (`infra/operacion/reconciliar.sh`): cada proyección coincide con su fuente.
- **Carga** (k6), **respaldo y restauración** de las bases, manifiestos de **Kubernetes** validados con kubeconform.

## Documentación

| Documento | Contenido |
|---|---|
| [a_ Arquitectura actual](doc/5.%20Microservicios/a_ARQUITECTURA-ACTUAL.md) y [b_ Riesgos](doc/5.%20Microservicios/b_ANALISIS-RIESGOS-ARQUITECTURA.md) | El monolito de partida y sus riesgos |
| [c_ Propuesta](doc/5.%20Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md) | Arquitectura objetivo y cómo quedó implementada |
| [d_ Ruta](doc/5.%20Microservicios/d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md) | Fases 0 a 7 con pasos y criterios de salida |
| `e_` a `n_` | Una por fase: qué se hizo, por qué, errores encontrados y cómo se resolvieron, verificación |
| [k_ Impacto en el monolito](doc/5.%20Microservicios/k_IMPACTO-EN-EL-MONOLITO.md) | Qué se cambió en el monolito en cada fase |
| [o_ Guía de operación](doc/5.%20Microservicios/o_GUIA-OPERACION.md) | Alertas, reconciliación, respaldos, DLQ |
| [q_ Retiro del monolito](doc/5.%20Microservicios/q_RETIRO-DEL-MONOLITO.md) | Cómo se retiró el monolito del repositorio |
| [k8s/README.md](k8s/README.md) | Despliegue en Kubernetes |

El código del monolito original queda en la etiqueta de git `monolito-final`.

## Integrantes

- Shanira Ramirez
- Erick Soto
