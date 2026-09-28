# Backend Seguros — Implementación del API Gateway (Fase 0 de la migración a microservicios)

> **Nota (2026-09-28):** el monolito se retiró del repositorio. Las rutas `Arquitectura-Clean/...`, el contenedor `andina-clean-mongodb` y los comandos `cd Arquitectura-Clean` de este documento describen el estado de su momento: hoy el Compose y el `.env` están en la raíz y el código del monolito queda en la etiqueta de git `monolito-final`. Ver [q_RETIRO-DEL-MONOLITO.md](q_RETIRO-DEL-MONOLITO.md).

Este documento describe **lo que ya está implementado y verificado en el repositorio**, no una
propuesta. Es la fase 0 del plan de migración
([d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md), paso 0.10):
poner un **API Gateway** (Spring Cloud Gateway) delante del backend monolítico actual, sin tocar
todavía su arquitectura interna ni extraer ningún microservicio. El objetivo es habilitar el
patrón **Strangler Fig**: todo el tráfico ya pasa por un único punto de entrada, desde el cual se
irán extrayendo `identity-service`, `customer-service`, `quotation-service`, `policy-service`,
`claims-service` y `notification-service` uno por uno (fases 2 a 6 del mismo documento).

```
Cliente Web
   │
   ▼
Ingress (NGINX)      ← nuevo, Kubernetes (objetivo de despliegue)
   │
   ▼
API Gateway           ← nuevo, este documento
   │
   ▼
Backend actual (Arquitectura-Clean, Clean Architecture, sin cambios de dominio)
```

No se implementó todavía: `identity-service`, `customer-service`, `quotation-service`,
`policy-service`, `claims-service`, `notification-service` como servicios separados, ni Outbox,
ni Saga, ni DLQ nuevas, ni JWT RS256/JWKS. Esos son objeto de las fases 1 a 7 y ya están
detallados en [d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md).
Este documento **no repite** el análisis de arquitectura actual, de riesgos ni la propuesta de
descomposición — están en
[a_ARQUITECTURA-ACTUAL.md](a_ARQUITECTURA-ACTUAL.md),
[b_ANALISIS-RIESGOS-ARQUITECTURA.md](b_ANALISIS-RIESGOS-ARQUITECTURA.md) y
[c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md](c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md).

---

## 1. Alcance exacto de esta entrega

Del checklist de la "primera fase a implementar", esto es lo que se construyó:

| # | Punto pedido | Estado | Dónde |
|---|---|---|---|
| 1 | API Gateway | ✅ Hecho | `gateway/` (módulo Maven nuevo) |
| 2–4 | Kubernetes: Ingress, Service, ConfigMap, Secret | ✅ Manifiestos escritos y **desplegados y probados en un clúster real** (kind + NGINX Ingress), ver sección 7 | `k8s/` |
| 5 | Service DNS | ✅ Diseñado (`*.svc.cluster.local`) | `k8s/11-configmap-gateway.yaml`, `k8s/20-configmap-backend.yaml` |
| 7 | Health Checks | ✅ Hecho en el gateway y **agregado también al backend** (le faltaba) | Actuator en ambos |
| 8 | Correlation ID | ✅ Hecho | `CorrelationIdGlobalFilter` |
| 9 | Rate Limiting con Redis | ✅ Hecho (estricto en login/MFA, general en el resto) | rutas en `application.yml` |
| 10 | Circuit Breaker | ✅ Hecho (Resilience4j, reactivo) | ruta `backendCB` |
| 11 | Timeout | ✅ Hecho (conexión, respuesta y `TimeLimiter`) | `application.yml` |
| 12 | Retry seguro | ✅ Hecho (solo `GET`, nunca `POST`/`PATCH`) | ruta `backend` |
| 13 | Métricas Actuator/Micrometer/Prometheus | ✅ Hecho en el gateway y en el backend | `/actuator/prometheus` en ambos |

Lo que el prompt original pedía pero **no se implementó en esta entrega**, con la razón:

| Pedido | Por qué no | Cuándo |
|---|---|---|
| Extraer `identity-service`, `customer-service`, etc. | El propio plan de trabajo lo prohíbe expresamente en la fase 0 ("NO extraer todavía todos los microservicios") | Fases 2 a 6 |
| JWT RS256 + JWKS | Requiere que exista `identity-service` publicando `/.well-known/jwks.json`; hoy solo existe el backend con HS256 | Fase 2 |
| Outbox / Saga / DLQ nuevas | Son mecanismos de consistencia entre microservicios; con un solo backend detrás del gateway no aplican todavía | Fases 0.8 (outbox en el monolito) y 6 (saga) del plan |
| Bulkhead explícito | Ya existe timeout + circuit breaker hacia el único backend; un bulkhead por dependencia tiene sentido cuando haya varias llamadas salientes distintas (JSON.pe, Google, Facebook) con pools propios — eso vive en el *backend*, no en el gateway, y es paso 3.2/6.2 del plan | Fases 2–3 |
| Logs JSON estructurados con MDC | Ver la nota técnica en `AccessLogGlobalFilter` (sección 5.2) | Cuando se agregue `context-propagation` + Loki/Promtail (paso 0.9 del plan) |
| Despliegue real en Kubernetes | No hay clúster disponible en este entorno de desarrollo | Se deja documentado en `k8s/README.md` con el procedimiento exacto |

---

## 2. Decisión de versiones (Spring Boot / Spring Cloud / Resilience4j)

El backend actual (`Arquitectura-Clean/pom.xml`) usa **Spring Boot 3.3.5** y **Java 21**. Antes
de escribir código se validó la compatibilidad (no se supuso ninguna versión):

| Componente | Versión elegida | Por qué |
|---|---|---|
| Spring Boot (gateway) | **3.3.5** | La misma que el backend, para no manejar dos matrices de compatibilidad distintas |
| Spring Cloud BOM | **2023.0.3** ("Leyton") | Es el primer patch de la serie 2023.0 con soporte confirmado para la línea Spring Boot 3.3.x (Spring Cloud 2023.0.0 solo cubría hasta 3.2.x) |
| `spring-cloud-starter-gateway` | (gestionado por el BOM) | En la serie 2023.0 este artefacto **es** el gateway reactivo (WebFlux/Netty); el split en `-server-webflux` / `-server-webmvc` llegó después, en la serie 2024.0 — no aplica aquí |
| Resilience4j | **2.2.0** (`spring-cloud-starter-circuitbreaker-reactor-resilience4j`) | `resilience4j-spring-boot3` 2.2.0 está construido sobre `resilience4j-spring6`, compatible con Spring Boot 3.3 |
| JJWT | **0.12.6** | La misma versión que ya usa el backend (`Arquitectura-Clean/pom.xml`), para que ambos lean/firmen el token de la misma forma |

Verificado compilando y arrancando el contexto de Spring realmente (no solo leyendo
documentación): ver sección 7.

---

## 3. Por qué un Gateway Pattern / Edge Service (y no Clean Architecture)

El Gateway **no tiene dominio de negocio** — no sabe qué es una póliza ni una cotización, solo
enruta, valida sesión de forma superficial, limita tráfico y observa. Por eso:

- No tiene capas `entities`/`usecases`/`interfaceadapters`/`frameworksdrivers` como el backend.
- Su paquete se organiza por **responsabilidad técnica** (`filter`, `security`, `ratelimit`,
  `web`, `config`), no por caso de uso.
- Es reactivo (WebFlux/Netty), no MVC: un gateway hace *proxying* de muchas conexiones
  concurrentes de larga vida (esperando al backend), y el modelo no bloqueante evita agotar un
  pool de hilos por cada request en tránsito.

## 4. Estructura de archivos

### 4.1 Archivos nuevos

```
gateway/
├── pom.xml
├── Dockerfile
├── .dockerignore
└── src/
    ├── main/
    │   ├── java/com/andinaseguros/gateway/
    │   │   ├── ApiGatewayApplication.java
    │   │   ├── config/
    │   │   │   ├── GatewayConfig.java
    │   │   │   └── GatewaySecurityProperties.java
    │   │   ├── security/
    │   │   │   └── JwtValidator.java
    │   │   ├── filter/
    │   │   │   ├── CorrelationIdGlobalFilter.java
    │   │   │   ├── AccessLogGlobalFilter.java
    │   │   │   └── JwtAuthenticationGlobalFilter.java
    │   │   ├── ratelimit/
    │   │   │   └── ClientIpKeyResolver.java
    │   │   └── web/
    │   │       ├── ErrorResponse.java
    │   │       ├── FallbackController.java
    │   │       └── GatewayErrorAttributes.java
    │   └── resources/application.yml
    └── test/java/com/andinaseguros/gateway/ApiGatewayApplicationTests.java

k8s/
├── 00-namespace.yaml
├── 10-secret-jwt.example.yaml
├── 11-configmap-gateway.yaml
├── 12-deployment-gateway.yaml
├── 13-service-gateway.yaml
├── 14-hpa-gateway.yaml
├── 20-configmap-backend.yaml
├── 21-secret-backend.example.yaml
├── 22-deployment-backend.yaml
├── 23-service-backend.yaml
├── 30-mongodb.yaml
├── 31-rabbitmq.yaml
├── 31-secret-rabbitmq.example.yaml
├── 32-redis.yaml
├── 40-ingress.yaml
└── README.md

doc/5. Microservicios/e_IMPLEMENTACION-API-GATEWAY-FASE0.md   (este archivo)
```

### 4.2 Archivos existentes modificados

| Archivo | Cambio | Por qué |
|---|---|---|
| `Arquitectura-Clean/pom.xml` | + `spring-boot-starter-actuator`, + `micrometer-registry-prometheus` | `SecurityConfig` ya permitía `/actuator/health` pero la dependencia no existía (daba 404). Es el paso 0.9 del plan de migración ("Observabilidad base... Micrometer + `/actuator/prometheus`") |
| `Arquitectura-Clean/src/main/resources/application.yml` | + sección `management` (health con grupos liveness/readiness, `/actuator/prometheus`); `app.whatsapp.token` y `app.jsonpe.token` pasan de estar escritos en el archivo a `${WHATSAPP_TOKEN:}` / `${JSONPE_TOKEN:}` | Habilita los *probes* de Kubernetes; corrige el riesgo **S3** (secretos en el código) para estos dos tokens, ya identificado en [b_ANALISIS-RIESGOS-ARQUITECTURA.md](b_ANALISIS-RIESGOS-ARQUITECTURA.md) |
| `Arquitectura-Clean/src/main/java/.../SecurityConfig.java` | La lista de rutas públicas pasa de `"/actuator/health"` a incluir también `"/actuator/health/**"` y `"/actuator/prometheus"` | Sin el `/**`, Spring Security no dejaba pasar `/actuator/health/liveness` ni `/actuator/health/readiness` (los que usan los *probes* de Kubernetes) |
| `Arquitectura-Clean/docker-compose.yml` | + servicio `redis`, + servicio `gateway`, + red `gateway_network`; `frontend` cambia su `VITE_API_URL` de `:8083` a `:8080`; `backend` gana `JSONPE_TOKEN`/`WHATSAPP_TOKEN` | Cablea el gateway en el entorno de desarrollo local (paso 0.10 del plan) |
| `Arquitectura-Clean/.env.example` | + `JSONPE_TOKEN`, `WHATSAPP_TOKEN`; `FACEBOOK_REDIRECT_URI` pasa de `:8083` a `:8080` | Consistente con los dos cambios anteriores |
| `frontend/.env`, `frontend/.env.example` | `VITE_API_URL` y `VITE_FACEBOOK_API_URL` pasan de `:8083` a `:8080` | El frontend deja de llamar directo al backend; llama al gateway |
| `DOCKER-EJECUCION.md` | Reescrito: ya no describe Onion/Hexagonal (se habían retirado en un commit previo) ni ignora el gateway | Estaba desactualizado |
| `.gitignore` | + patrón para no versionar los `Secret` reales de `k8s/` (solo los `*.example.yaml`) | Evita commitear credenciales |

**Nota de transparencia sobre secretos ya expuestos:** los valores que antes estaban escritos en
`application.yml` (`app.whatsapp.token`, `app.jsonpe.token`) ya estaban commiteados en el
historial de Git. Externalizarlos ahora no los invalida retroactivamente — hay que **rotarlos en
el panel de JSON.pe** (acción externa, fuera del alcance de este repositorio). Esto ya estaba
señalado como pendiente en el paso 0.4 del plan de migración.

---

## 5. Decisiones de diseño del Gateway (con su justificación)

### 5.1 Autenticación: JWT HS256 compartido, no OAuth2/JWKS

El backend firma hoy con **HS256** y un secreto simétrico
(`Arquitectura-Clean/.../JwtTokenAdapter.java`). El plan de migración solo introduce RS256 +
JWKS cuando exista `identity-service` (fase 2). Como todavía no existe, el Gateway **no** usa
`spring-boot-starter-oauth2-resource-server` (que espera un JWK Set o una clave pública): validar
así habría significado inventar un mecanismo de claves que el sistema real no tiene.

En su lugar, `JwtValidator` (`gateway/.../security/JwtValidator.java`) valida el mismo HS256 con
el mismo secreto (`JWT_SECRET`, compartido por variable de entorno / `Secret` de Kubernetes) que
usa `JwtTokenAdapter` en el backend: mismo algoritmo (`Keys.hmacShaKeyFor`), mismo *claim* de rol
(`"rol"`, no `"roles"` — se verificó el código real del backend antes de asumir el nombre). El
Gateway rechaza temprano las peticiones sin sesión (menos carga innecesaria en el backend) y
propaga `X-User-Id` / `X-User-Rol`, pero **el backend sigue validando el token de forma
independiente** — el Gateway nunca es la única barrera (regla del prompt: "no confiar
exclusivamente en que el Gateway proteja toda la seguridad").

Rutas públicas (no exigen JWT en el Gateway) — son exactamente las mismas que ya declara
`SecurityConfig` del backend, para no crear una segunda fuente de verdad divergente:
`/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/actuator/**` (del
propio gateway), `/fallback/**`.

### 5.2 Correlation ID y logging (sin prometer MDC reactivo que no existe)

`CorrelationIdGlobalFilter` acepta `X-Correlation-Id` si llega, o genera un UUID; lo agrega al
request saliente, a la respuesta y a un atributo del `exchange`. `AccessLogGlobalFilter` loguea
una línea por petición con ese id.

Decisión explícita: **no se usa MDC de Logback** para esto. En WebFlux, una misma petición puede
saltar entre varios hilos del *event loop*, y el MDC está basado en `ThreadLocal` — sin cablear
`io.micrometer:context-propagation` (Reactor Context Propagation), el MDC quedaría vacío o
mezclado entre peticiones de forma intermitente. Documentar eso como si funcionara sería
entregar una observabilidad que parece correcta pero falla bajo carga concurrente real. En su
lugar, el `correlationId` se imprime explícitamente en cada línea de log
(`correlationId=... method=... path=... status=... durationMs=...`). Los microservicios de
negocio que se extraigan más adelante (Spring MVC, *thread-per-request*) sí pueden usar MDC de
forma segura — así lo describe el paso 7 de la plantilla en
[d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#3-plantilla-de-un-microservicio-checklist-de-creación).

Logs JSON con Loki/Promtail (sección 7.2 de la propuesta) quedan pendientes: requieren agregar
`logstash-logback-encoder` (o similar) y el stack de observabilidad, fuera del alcance mínimo de
esta fase.

### 5.3 Rate limiting: por IP, no por usuario

`RequestRateLimiter` (Redis) necesita una clave. El *key resolver* por defecto de Spring Cloud
Gateway usa el principal autenticado — pero login y verificación MFA ocurren **antes** de tener
un JWT. `ClientIpKeyResolver` resuelve por IP (leyendo `X-Forwarded-For` cuando existe, porque
detrás de un Ingress la conexión TCP real es la del *load balancer*, no la del cliente).

Dos configuraciones distintas:

| Ruta | `replenishRate` | `burstCapacity` | Por qué |
|---|---|---|---|
| `POST /api/auth/login` | 1 req/s | 5 | Cierra el riesgo **S9** (sin límite de intentos) sin bloquear a un usuario que escribe mal la contraseña dos veces |
| `POST /api/auth/mfa/verificar` | 1 req/s | 5 | Mismo riesgo, para el código TOTP |
| Resto de `/api/**` | 50 req/s | 100 | Generoso a propósito: en la fase 0 protege contra un pico anómalo, no reemplaza al *rate limiting* de negocio que cada microservicio deberá afinar cuando se extraiga |

### 5.4 Resiliencia: Circuit Breaker + Timeout + Retry (solo GET)

Un único circuito lógico `backendCB` (hay un solo servicio detrás del gateway en esta fase):

- **Timeout** 3 s (`resilience4j.timelimiter.instances.backendCB`), igual al valor de la tabla
  6.2 de la propuesta de migración ("Gateway → servicio interno: 3 s").
- **Circuit breaker**: ventana de 20 llamadas, mínimo 10 para evaluar, abre con 50% de fallos o
  80% de llamadas lentas (>3 s), medio-abierto tras 30 s. Los mismos parámetros que la tabla 6.2.
- **Fallback**: `FallbackController` responde siempre `503` + `Retry-After: 10`, nunca cuelga la
  conexión ni devuelve un error crudo de Netty.
- **Retry**: **solo en `GET`**, 1 reintento, backoff 100 ms → 500 ms. Nunca en `POST`/`PATCH` —
  regla explícita del prompt y del análisis: reintentar "emitir póliza" o "registrar siniestro"
  sin una clave de idempotencia podría duplicar la operación.

### 5.5 CORS centralizado

`spring.cloud.gateway.globalcors` reemplaza al CORS que hoy configura el backend
(`SecurityConfig.corsConfigurationSource()`). El backend **no se modificó** para quitar su CORS
propio: mientras el puerto `:8083` siga publicado (ver sección 6), su CORS sigue siendo necesario
para quien lo llame directo. Cuando se cierre ese puerto (ver "deuda técnica" más abajo), el CORS
del backend deja de ser necesario y se puede retirar.

### 5.6 Manejo centralizado de errores

Toda respuesta de error tiene la misma forma (`timestamp`, `status`, `error`, `message`, `path`,
`correlationId`): `JwtAuthenticationGlobalFilter` arma el `401` a mano (necesita cortar el flujo
antes de que exista una excepción que capturar); `FallbackController` arma el `503`;
`GatewayErrorAttributes` (extiende `DefaultErrorAttributes`) captura cualquier otro error
(`404`, `500`, etc.) con el mismo formato, en vez del cuerpo por defecto de Spring Boot.

---

## 6. Deuda técnica reconocida (para no generar falsas expectativas)

| Punto | Estado actual | Consecuencia |
|---|---|---|
| El backend sigue publicando `:8083` directo al host | Se dejó así a propósito, para no romper la colección Bruno (`ColeccionBruno/`) ni las pruebas manuales existentes mientras se valida el gateway | Viola la regla "solo el gateway y el frontend exponen puertos"; hay que migrar esas pruebas al `:8080` y cerrar `:8083` en un cambio siguiente |
| `backend` sigue en `replicas: 1` (Docker Compose y Kubernetes) | El estado OAuth de Facebook, los tickets de login y los desafíos MFA siguen en memoria del proceso (riesgo **A3**, sin resolver) | No se puede escalar el backend horizontalmente todavía; por eso **no se le puso HPA** (ver `k8s/README.md`) |
| Logs JSON correlacionados en Loki | No implementado | Se sigue leyendo el log de texto plano de cada contenedor |
| MongoDB/RabbitMQ de un solo nodo en Kubernetes | Documentado como limitación en `k8s/README.md` | Sin alta disponibilidad de datos; ver fase 7 del plan |

---

## 7. Verificación realizada en este entorno

No hay Maven, Docker daemon activo ni clúster de Kubernetes preinstalados en esta máquina de
desarrollo. Esto es lo que sí se pudo verificar de forma real (no solo leer código):

1. **Se descargó Maven 3.9.9 de forma portátil** (`archive.apache.org`) y se compiló el gateway:
   ```bash
   mvn -B -DskipTests clean package   # gateway/
   ```
   Resultado: `gateway/target/api-gateway-1.0.0.jar` generado (47 MB, *fat jar*).

2. **Se corrigió un error real de compilación** encontrado en esa build:
   `HttpHeaders.FORWARDED` no existe como constante en `org.springframework.http.HttpHeaders`
   (`ClientIpKeyResolver.java` usaba un nombre de campo inventado); se corrigió usando el nombre
   de cabecera literal `"Forwarded"`.

3. **Se ejecutó una prueba de arranque real del contexto de Spring**
   (`ApiGatewayApplicationTests`, `@SpringBootTest(webEnvironment = RANDOM_PORT)`):
   ```bash
   mvn test   # gateway/
   ```
   Resultado: `Tests run: 1, Failures: 0, Errors: 0` — Netty arrancó, todas las rutas y filtros
   se cablearon sin errores de configuración (`RequestRateLimiter`, `CircuitBreaker`, `Retry`,
   `GatewaySecurityProperties`, `JwtValidator`, actuator). No requiere Redis corriendo porque la
   conexión reactiva es *lazy* (no se prueba al arrancar, solo al usarse).

4. **Se recompiló el backend** después de agregarle Actuator, para confirmar que no se rompió
   nada existente:
   ```bash
   mvn -B -DskipTests clean package   # Arquitectura-Clean/
   ```
   Resultado: `andina-seguros-clean-1.0.0.jar` generado correctamente.

5. **Se construyeron las imágenes Docker reales** (`docker build`) una vez que se pudo iniciar
   Docker Desktop, y se levantó el stack completo con `docker compose up -d --build`
   (`Arquitectura-Clean/docker-compose.yml`): los 7 contenedores (gateway, backend, consumer,
   frontend, MongoDB, RabbitMQ, Redis) llegaron a estado `healthy`/`Up`. Contra ese stack real se
   probó, con `curl`, todo lo descrito en la sección 9 de este documento: propagación de
   `X-Correlation-Id`, el `401` uniforme del Gateway sin token, un **login real** contra el
   backend (devolvió el desafío MFA del usuario demo), el **rate limiter de Redis** (con
   peticiones en paralelo se obtuvo `429`; en serie no, porque el hash de BCrypt del backend
   tarda lo suficiente para que el balde de tokens se recargue solo — ambos comportamientos son
   correctos) y el **circuit breaker** (se detuvo el contenedor del backend, el Gateway respondió
   `503` con `Retry-After: 10` en cada intento, y al reiniciar el backend y esperar
   `waitDurationInOpenState`, el tráfico volvió a fluir con un login real exitoso).

6. **Se desplegaron los 15 manifiestos de `k8s/` en un clúster real**, no solo se validó su
   sintaxis. Como esta máquina no traía ningún clúster, se creó uno local con **kind** (usa
   contenedores Docker; no requiere GUI ni licencias) y se instaló NGINX Ingress Controller. Se
   probó con tráfico real la cadena completa **Ingress (con TLS y `ssl-redirect`) → API Gateway
   (2 réplicas reales, balanceadas por el Service) → Backend → MongoDB**, incluyendo un login que
   devolvió un JWT válido. En el camino aparecieron y se corrigieron **dos errores reales que
   solo se manifiestan al desplegar** (no se detectan leyendo el YAML ni con `kubectl
   apply --dry-run`): RabbitMQ fallaba con `Error when reading .erlang.cookie: eacces` por un
   problema de permisos del *overlay* de solo-imagen en el runtime de kind/containerd (se
   corrigió montando un `emptyDir` en `/var/lib/rabbitmq`), y RabbitMQ/MongoDB entraban en bucle
   de reinicio porque sus *health probes* de tipo `exec` no tenían `timeoutSeconds` explícito — el
   valor por defecto de Kubernetes es 1 segundo, insuficiente para comandos que arrancan su
   propio proceso (`rabbitmq-diagnostics`, `mongosh`) bajo la CPU compartida de una máquina de
   desarrollo. El detalle completo, con los mensajes de error reales y el fix aplicado a cada
   archivo, está en `k8s/README.md`. También se detectó que `kind` no trae `metrics-server`
   instalado (el `HorizontalPodAutoscaler` del gateway se crea pero no puede leer el uso de CPU
   sin él) — documentado como prerrequisito de entorno, no como un defecto del manifiesto.

---

### 7.1 Errores encontrados y cómo se resolvieron

Resumen de lo que falló durante la fase, por qué y cómo se corrigió (el detalle técnico está en la lista de arriba).

| # | Error o problema | Causa | Solución | Cómo se verificó |
|---|---|---|---|---|
| 1 | El gateway no compilaba | Se usó `HttpHeaders.FORWARDED`, una constante que no existe en `org.springframework.http.HttpHeaders` | Se usa el nombre literal de la cabecera, `"Forwarded"` | `mvn package` compila y la prueba de contexto de Spring arranca |
| 2 | RabbitMQ no arrancaba en Kubernetes: `Error when reading .erlang.cookie: eacces` | El contenedor corre sin root y no podía escribir la cookie de Erlang en `/var/lib/rabbitmq` | Volumen `emptyDir` montado en `/var/lib/rabbitmq` (en la fase 7 pasó a StatefulSet con volumen persistente) | Pod `1/1 Running` en el clúster kind |
| 3 | RabbitMQ y MongoDB se reiniciaban en bucle en Kubernetes | Sus *probes* `exec` no tenían `timeoutSeconds`: el valor por defecto es 1 s y `rabbitmq-diagnostics` o `mongosh` tardan más; el *liveness* mataba el contenedor justo al terminar de arrancar | `timeoutSeconds: 10` en los *probes* `exec` (5 en los `httpGet` del gateway y el backend) y un `startupProbe` propio para RabbitMQ y MongoDB | Todos los Pods estables; login de punta a punta Ingress → gateway (2 réplicas) → backend → MongoDB |
| 4 | El HPA del gateway mostraba `<unknown>` | kind no trae `metrics-server` | No es un defecto del manifiesto: se documentó como prerrequisito del clúster (`k8s/README.md`) | `kubectl get hpa` muestra las métricas una vez instalado `metrics-server` |
| 5 | El límite de peticiones del login no cortaba en una prueba en serie | Cada login tarda lo que tarda BCrypt, y en ese tiempo el balde de *tokens* se recargaba | No era un defecto: la prueba se repitió con una ráfaga en paralelo | La ráfaga paralela recibe `429` como está diseñado |

## 8. Cómo compilar, construir y probar

### 8.1 Compilar

```bash
cd gateway
mvn clean package            # genera target/api-gateway-1.0.0.jar
mvn test                     # levanta el contexto de Spring (sin red externa)
```

### 8.2 Construir las imágenes Docker

```bash
docker build -t andina-api-gateway:1.0.0 ./gateway
docker build -t andina-seguros-clean:1.0.0 ./Arquitectura-Clean
```

### 8.3 Levantar todo con Docker Compose (desarrollo local)

```bash
cd Arquitectura-Clean
cp .env.example .env    # completar JSONPE_TOKEN, WHATSAPP_TOKEN, etc.
docker compose up -d --build
docker compose ps
```

El frontend queda en `http://localhost:5173`, el gateway en `http://localhost:8080/api`.

### 8.4 Desplegar en un clúster Kubernetes real

Ver el procedimiento completo, con el orden exacto de `kubectl apply` y la creación de los
`Secret` reales, en [`k8s/README.md`](../../k8s/README.md).

---

## 9. Pruebas manuales (curl)

Asumiendo el stack de Docker Compose levantado (gateway en `:8080`, backend accesible solo a
través de él):

```bash
# 1) Correlation ID: si no se manda, el gateway genera uno y lo devuelve
curl -i http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"incorrecta"}'
# -> la respuesta trae el header X-Correlation-Id aunque el login falle

# 2) Correlation ID: si se manda, el gateway lo respeta
curl -i http://localhost:8080/api/auth/login \
  -H "X-Correlation-Id: mi-id-de-prueba-123" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"incorrecta"}'
# -> el header de respuesta X-Correlation-Id es exactamente "mi-id-de-prueba-123"

# 3) Rate limiting de login: 6 intentos seguidos, el 6to debe dar 429
for i in 1 2 3 4 5 6; do
  curl -s -o /dev/null -w "intento $i -> %{http_code}\n" http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" -d '{"username":"x","password":"y"}'
done

# 4) Ruta protegida sin token -> 401 con el cuerpo uniforme de error
curl -i http://localhost:8080/api/clientes
# -> 401, body: {"timestamp":...,"status":401,"error":"UNAUTHORIZED",...,"correlationId":"..."}

# 5) Ruta protegida con token -> el backend responde (o su propio 401/403 si el rol no alcanza)
TOKEN="<pegar un JWT valido emitido por POST /api/auth/login>"
curl -i http://localhost:8080/api/clientes -H "Authorization: Bearer $TOKEN"

# 6) Circuit breaker / fallback: apagar el backend y volver a pedir
docker compose stop backend
curl -i http://localhost:8080/api/clientes -H "Authorization: Bearer $TOKEN"
# -> tras algunas fallas, 503 con Retry-After: 10 y el mismo cuerpo de error uniforme
docker compose start backend

# 7) Métricas y salud del propio gateway
curl -s http://localhost:8080/actuator/health | jq
curl -s http://localhost:8080/actuator/prometheus | head -30
```

---

## 10. Criterios de aceptación de esta fase

- [x] El gateway compila y su contexto de Spring arranca sin errores de configuración.
- [x] El backend sigue compilando después de agregarle Actuator.
- [x] Toda petición recibe un `X-Correlation-Id` en la respuesta (propio o el que mandó el
      cliente).
- [x] `/api/auth/login` y `/api/auth/mfa/verificar` tienen un límite de intentos propio, más
      estricto que el resto de la API.
- [x] Una ruta protegida sin `Authorization: Bearer` responde `401` con un cuerpo JSON uniforme,
      sin llegar al backend.
- [x] Si el backend no responde, el gateway responde `503` con `Retry-After` en vez de colgar la
      conexión o propagar un error crudo.
- [x] Ninguna llamada `POST`/`PATCH` de negocio se reintenta automáticamente.
- [x] La URL del backend, la de Redis y los orígenes de CORS se leen de variables de entorno /
      `ConfigMap`; no hay ninguna URL ni IP escrita en el código Java.
- [x] Ningún secreto (JWT, credenciales de RabbitMQ, `FACEBOOK_APP_SECRET`, tokens de JSON.pe)
      quedó escrito en un archivo versionado; los `Secret` de Kubernetes solo existen como
      `*.example.yaml` con valores de relleno.
- [x] El gateway y el backend exponen `/actuator/health` (con grupos *liveness*/*readiness*) y
      `/actuator/prometheus`.
- [x] Desplegado en un Kubernetes real (clúster `kind` local + NGINX Ingress Controller):
      `Ingress → api-gateway (2 réplicas) → backend → MongoDB` responde con un JWT real.
- [ ] *(pendiente, requiere `metrics-server`, no instalado por defecto en kind — ver
      `k8s/README.md`)* Confirmar que el `HorizontalPodAutoscaler` del gateway escala bajo carga
      real, no solo que el objeto se crea.
- [ ] *(pendiente, deuda técnica reconocida en la sección 6)* Cerrar el puerto `:8083` del
      backend una vez migradas las pruebas manuales al gateway.

---

## 11. Próximo paso sugerido

Seguir con la **fase 1** del plan (`notification-service`), que es la que menos depende del
resto del sistema: agregar los eventos `customer.registered.v1` / `customer.updated.v1` al
monolito y hacer que el consumer deje de leer la colección `clientes` directo de Mongo. Está
detallada en la sección 5 de
[d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#5-fase-1--notification-service).
