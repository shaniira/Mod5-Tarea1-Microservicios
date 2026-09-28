# Backend Seguros — Implementación de identity-service (Fase 2 de la migración a microservicios)

> **Nota (2026-09-28):** el monolito se retiró del repositorio. Las rutas `Arquitectura-Clean/...`, el contenedor `andina-clean-mongodb` y los comandos `cd Arquitectura-Clean` de este documento describen el estado de su momento: hoy el Compose y el `.env` están en la raíz y el código del monolito queda en la etiqueta de git `monolito-final`. Ver [q_RETIRO-DEL-MONOLITO.md](q_RETIRO-DEL-MONOLITO.md).

Este documento registra lo que se implementó en la fase 2 de la [ruta de implementación](d_RUTA-IMPLEMENTACION-MICROSERVICIOS.md#6-fase-2--identity-service), las decisiones tomadas y la verificación de los criterios de aceptación contra el stack de Docker.

**Resultado:** los usuarios, el login (contraseña, Google, Facebook), el MFA y la emisión del JWT viven en `services/identity-service`. Los tokens se firman con **RS256** y la clave privada solo existe en ese servicio. El gateway y el backend verifican con la clave pública publicada en `/.well-known/jwks.json`. El estado efímero está en **Redis**, así que identity-service corre con **dos réplicas** sin fallos. El secreto simétrico `JWT_SECRET` y la firma HS256 ya no existen.

---

## 1. Qué se hizo, paso por paso

| Paso de la ruta | Implementación |
|---|---|
| 2.1 Redis | El Redis del gateway se conecta también a la red `andina_identity_data_network`; identity-service usa la base 1 (la 0 es del rate limiter). Sin puerto en el host |
| 2.2 JWT RS256 | `RsaJwtTokenAdapter` firma con RS256 y `kid` igual a la huella de la clave. Claims: `sub`, `rol` (lo lee el frontend), `roles`, `customerId` (solo CLIENTE), `iss=andina-identity`, `iat`, `exp`, `jti`. La clave privada la genera una vez el contenedor `identity-keygen` en el volumen `andina_identity_keys`, que solo monta identity-service (usuario 1001, permiso 400). `JwksController` publica solo la parte pública |
| 2.3 Validación por JWKS con ventana | El backend pasó a ser *resource server* (`JwtDecoderConfig`) y el gateway valida con `NimbusReactiveJwtDecoder`. Durante la ventana ambos aceptaron también HS256 (commits `8e2c927` y `f2ba312`); al cerrar la fase se retiró (`71a66ac` y `b0af45d`) |
| 2.4 identity-service con la plantilla | Se movieron `auth/*`, `mfa/*` y los adaptadores de Google, Facebook, TOTP, QR, BCrypt y AES-GCM, con la estructura Clean y las reglas ArchUnit. Las rutas y respuestas son las mismas, así que el frontend no cambió |
| 2.5 Estado efímero en Redis | `RedisOAuthStateAdapter` (300 s), `RedisLoginTicketAdapter` (60 s) y `RedisMfaChallengeAdapter` (300 s), con TTL y consumo atómico `GETDEL` (un solo uso aunque dos réplicas lo pidan a la vez). Reemplazan a los adaptadores `InMemory*` |
| 2.6 Índice de correos | `customer_email_index` en `identity_db`, alimentado por `customer.registered.v1` y `customer.updated.v1` (cola `identity.customer.events`, con DLQ). Resuelve dos cosas sin consultar la base de clientes: quién puede crear cuenta con Google (`CLIENTE_NO_REGISTRADO`) y el `customerId` del token |
| 2.7 Migración de `usuarios` | `migracion/migrar-usuarios.sh` copia la colección a `identity_db` con `mongodump`/`mongorestore` (hashes BCrypt y secretos MFA tal cual) y compara cantidad y huella SHA-256. La colección del backend no se toca (plan de reversa) |
| 2.8 Resiliencia hacia Google y Facebook | Google: timeout 3 s, circuit breaker `google`, bulkhead de 20 llamadas y caché de sus claves públicas. Facebook: timeout 5 s, circuit breaker `facebook`, bulkhead de 10 llamadas y **sin reintentos** (el código OAuth es de un solo uso). Solo las caídas del proveedor cuentan para el circuito, no los tokens o códigos inválidos |
| 2.9 Rutas en el gateway | `/api/auth/**` y `/api/mfa/**` van a identity-service con su propio circuit breaker (`identityCB`) y timeout de 12 s (el callback de Facebook hace hasta tres llamadas). Se mantienen los límites de login y verificación MFA |
| 2.10 Prueba con 2 réplicas | `identity-service` sin `container_name`; se escala con `docker compose up -d --scale identity-service=2`. Ver sección 3 |

Además:

- **Backend:** ya no consulta la colección `usuarios` en cada petición (el filtro JWT anterior lo hacía). El rol sale del token y "Mi cuenta" usa el `customerId` del token. Se eliminaron el login, MFA, Google, Facebook y sus 35 pruebas (viven ahora en identity-service), además de las dependencias jjwt y zxing.
- **Gateway:** además del cambio de rutas, se corrigió un error existente: la respuesta llevaba `Access-Control-Allow-Origin` dos veces (gateway y backend) y los navegadores la rechazan. Ahora el gateway deja una sola (`DedupeResponseHeader`). También se fijó la imagen de build a `maven:3.9.9-eclipse-temurin-21`.
- **Kubernetes:** manifiestos de identity-service (2 réplicas, probes, rolling update, clave privada desde el Secret `identity-jwt-key`). Gateway y backend reciben solo la URL del JWKS; se eliminó `andina-jwt-secret`. Validados con `kubectl apply --dry-run=client`.
- **Contratos:** OpenAPI de identity-service en `contracts/openapi/identity-service.json`.

## 2. Decisiones

| Decisión | Motivo |
|---|---|
| **Clave en un volumen generado por `identity-keygen`**, no en variables de entorno ni en el repositorio | Con dos réplicas la clave tiene que ser la misma; generarla en cada arranque rompería los tokens. Un contenedor de un solo uso la crea si falta y solo identity-service la monta, en modo lectura |
| **`customerId` en el token** | Permite al backend (y luego a policy o claims) filtrar por propietario sin consultar a identity. Es lo que propone la sección 7.1 |
| **El backend confía en el rol del token** | Consultar `usuarios` en cada petición obligaría a compartir la base. El costo: un usuario desactivado conserva acceso hasta que vence su token (ver limitaciones) |
| **Ventana de transición y luego retiro, en commits separados** | Es el paso 2.3: primero conviven HS256 y RS256 para no cerrar sesiones abiertas; al cerrar la fase se elimina el secreto. Si hiciera falta volver atrás, se revierten los commits de retiro |
| **Mismo comportamiento que el monolito**, incluido `POST /api/auth/register` | Regla de la migración: el servicio nuevo debe hacer lo mismo que el anterior. La corrección de S1 queda en su paso propio (0.2) |
| **Redis compartido con el gateway, en otra base y otra red** | Evita otra instancia en local; la separación por base (0 y 1) y por red mantiene aislados los datos |

## 3. Verificación de los criterios de aceptación

Pruebas hechas el 2026-09-25 contra el stack de `Arquitectura-Clean/docker-compose.yml` con **dos réplicas** de identity-service. Google y Facebook se simularon con WireMock (JWKS de Google y Graph API de Facebook) para no depender de cuentas reales. Se usaron las mismas credenciales y la misma configuración que en producción local, cambiando solo esas dos URLs.

| # | Criterio de salida (ruta, sección 6) | Cómo se probó | Resultado |
|---|---|---|---|
| 1 | Login por contraseña, Google, Facebook y MFA funcionan desde el servicio nuevo | **Contraseña:** `admin2` por el gateway devuelve un token RS256 con `kid`, `rol` y `roles`; `/api/auth/me` (identity) y `/api/clientes` (backend) responden 200. **CLIENTE:** el token de `ramirezlisset361@gmail.com` trae su `customerId` y "Mi cuenta" devuelve su cliente y su póliza. **Google:** un cliente nuevo registrado en el backend entra con Google; identity crea el usuario CLIENTE con el `customerId` del índice de correos. **Facebook:** inicio, callback y canje del ticket devuelven el token del usuario. **MFA:** configurar, activar con un código TOTP, desafío y verificación | ✅ Cumple |
| 2 | Con dos réplicas no hay fallos aleatorios de MFA ni de Facebook | Llamadas dirigidas a cada réplica: 3 veces seguidas el desafío MFA se creó en la réplica 1 y se verificó en la réplica 2; 2 veces el login de Facebook empezó en la réplica 1, el callback llegó a la réplica 2 y el ticket se canjeó en la réplica 1. **0 fallos** | ✅ Cumple |
| 3 | Los demás servicios validan con la clave pública; la privada solo existe en identity | `private.pem` existe solo en las dos réplicas de identity (`spring`, permiso 400); en backend y gateway no existe. Ningún contenedor tiene variables `JWT_SECRET` ni claves. El JWKS publicado no contiene ningún parámetro privado (`d`, `p`, `q`...). Una firma alterada da 401 | ✅ Cumple |
| 4 | HS256 retirado; el secreto simétrico ya no existe | Un token HS384 firmado con el secreto anterior: durante la ventana devolvía 200, al cerrarla devuelve **401** en el gateway y en el backend directo. `POST /api/auth/login` en el backend ya no existe. `git grep JWT_SECRET` fuera de `doc/` no encuentra nada (Compose, Kubernetes y código) | ✅ Cumple |
| — | Reversa | Mientras dure la fase, la colección `usuarios` del backend sigue intacta. Para volver atrás se revierten los commits de retiro y se apunta `IDENTITY_SERVICE_URL` al backend | ✅ Disponible |

Pruebas adicionales:

| Prueba | Resultado |
|---|---|
| Migración de `usuarios` (2.7) | 5 usuarios copiados; misma huella SHA-256 en las dos bases (`03396eb0…`) |
| Backfill del índice de correos | `POST /api/clientes/eventos/reenvio` → 7 clientes; 7 entradas en `customer_email_index` |
| Caché de claves de Google | Con el JWKS de Google "caído" (stub eliminado), el login con una clave ya conocida siguió respondiendo 200 **en las dos réplicas** |
| Circuit breaker y bulkhead de Google | Pruebas unitarias: 4 caídas abren el circuito y no se vuelve a llamar a Google; los tokens inválidos no lo abren; con el bulkhead lleno se responde "no disponible" sin llamar a Google |
| Pruebas automáticas | identity-service: 56 (incluye 8 reglas ArchUnit). Backend: 42 (las 35 de autenticación se movieron). notification-service: 27. Corren al construir las imágenes |
| Error encontrado y corregido | La primera versión del endpoint JWKS respondía 500 (Spring inyectaba un mapa de todos los beans en lugar del JWKS). Lo detectó la prueba de extremo a extremo; se corrigió y se agregó `JwksControllerTest` |

### 3.1 Errores encontrados y cómo se resolvieron

| # | Error o problema | Causa | Solución | Cómo se verificó |
|---|---|---|---|---|
| 1 | El gateway rechazaba tokens RS256 válidos y `/.well-known/jwks.json` respondía 500 | El controlador pedía un `Map<String, Object>` y Spring le inyectaba un mapa con **todos** los beans, no el JWKS | El controlador recibe la clave RSA y publica solo su parte pública. Nueva prueba `JwksControllerTest`: el JWKS no puede contener parámetros privados (`d`, `p`, `q`...) | JWKS con el mismo `kid`; el gateway y el backend aceptan los tokens |
| 2 | El navegador podía rechazar las respuestas del API (error que ya existía) | La cabecera `Access-Control-Allow-Origin` salía dos veces: la ponían el gateway y el backend | CORS solo en el gateway (commit `f2ba312`) | Una sola cabecera CORS en la respuesta |
| 3 | Los tokens del monolito no eran HS256 sino HS384 | jjwt elige el algoritmo según el largo del secreto | El decodificador de la ventana de transición replica esa regla en vez de fijar HS256 | Durante la ventana un token viejo daba 200; al cerrarla, 401 |
| 4 | Al quitar la configuración de autenticación del `application.yml` del backend se borró también el bloque `cors` | Error al editar un rango del archivo | Se restauró el bloque | El backend compila y sus pruebas pasan (42) |
| 5 | El backend no compilaba tras retirar la autenticación | Una clase de configuración de índices seguía usando el documento de usuarios | Esos índices viven ahora en `UsuarioDocument` de identity-service (`@Indexed`); se quitaron del backend | 42 pruebas del backend en verde |
| 6 | La prueba de caché de claves de Google no era determinista con dos réplicas | Cada réplica tiene su propia caché y el gateway reparte las llamadas | Se calienta cada réplica llamándola directamente antes de "apagar" Google | Login con Google en las dos réplicas con el JWKS caído |
| 7 | La imagen del gateway tardó más de 30 min en construirse | Red lenta al descargar dependencias de Maven | Problema del entorno, no del código: se esperó a que terminara (una construcción paralela con salida detallada confirmó que no estaba colgada) | Las tres imágenes quedaron listas |

## 4. Limitaciones conocidas

| Punto | Detalle |
|---|---|
| **Usuario desactivado** | ✅ Resuelto después (ver [h_CIERRE-PENDIENTES.md](h_CIERRE-PENDIENTES.md)): desactivar a un usuario revoca sus tokens al instante |
| **Registro con rol (S1)** | ✅ Resuelto después: el registro público solo crea CLIENTE |
| **Rotación de claves** | El JWKS publica una sola clave. Para rotar hay que publicar la nueva junto a la anterior hasta que venzan los tokens firmados con ella; no está automatizado |
| **MongoDB en Kubernetes** | ✅ Resuelto después: replica set con autenticación y un usuario por servicio |
| **Observabilidad** | ✅ Resuelto después: stack completo en `infra/observability` |

## 5. Commits de la fase

| Commit | Contenido |
|---|---|
| `a74da8c` feat(identity) | identity-service completo |
| `8e2c927` feat(backend) | El backend valida RS256 por JWKS (con ventana HS256) |
| `f2ba312` feat(gateway) | Rutas a identity, validación por JWKS (con ventana), CORS duplicado |
| `f1363fc` chore(docker) | identity-service, su MongoDB y la generación de claves |
| `71a66ac` refactor(backend) | Retiro del login y de HS256 del monolito |
| `b0af45d` refactor(gateway) | Retiro de HS256 del gateway |
| `71d50c1` chore(docker) | Retiro de `JWT_SECRET` |
| `a09ba2f` feat(k8s) | Manifiestos de identity-service y retiro del secreto compartido |

## 6. Impacto en el monolito

Detalle por commit en [k_IMPACTO-EN-EL-MONOLITO.md](k_IMPACTO-EN-EL-MONOLITO.md#4-fase-2--identity-service).

| Qué | Commit | Cambio en `Arquitectura-Clean` |
|---|---|---|
| Se cambió | `8e2c927` | El monolito pasa a *resource server*: valida RS256 con el JWKS de identity (`JwtDecoderConfig`), toma el rol del token y deja de consultar `usuarios` en cada petición; "Mi cuenta" usa el `customerId` del token |
| Se desacopló | `a74da8c`, `71a66ac` | Se eliminaron el filtro JWT propio, login (contraseña, Google, Facebook), MFA, `Usuario`, la colección `usuarios` (código), los adaptadores TOTP/QR/BCrypt/AES-GCM/`InMemory*`, la firma HS256 y las dependencias jjwt y zxing (78 archivos, 35 pruebas movidas a identity-service) |
| Infraestructura | `f1363fc`, `71d50c1` | El backend entra a la red de servicios (JWKS); se eliminó `JWT_SECRET` de su configuración |
| Se conservó | — | La colección `usuarios` en la base del backend (datos), como plan de reversa |

## 7. Próximo paso sugerido

Fase 3 (`customer-service`): clientes, vehículos y consulta de placas. identity-service y notification-service ya consumen `customer.*`, así que solo cambia quién publica esos eventos.
