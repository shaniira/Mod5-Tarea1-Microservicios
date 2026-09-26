# identity-service

Segundo microservicio de la migración (fase 2). Es el dueño de los **usuarios** y de todo lo que tiene que ver con entrar al sistema: login con contraseña, Google y Facebook, MFA (TOTP) y la emisión del JWT. Los demás componentes (gateway, backend) solo **verifican** el token con la clave pública.

Diseño, decisiones y verificación: [doc/5. Microservicios/g_IMPLEMENTACION-IDENTITY-SERVICE-FASE2.md](../../doc/5.%20Microservicios/g_IMPLEMENTACION-IDENTITY-SERVICE-FASE2.md).

## Endpoints

Los mismos que tenía el monolito (el frontend no cambia). Entran por el gateway (`http://localhost:8080`).

| Método y ruta | Uso |
|---|---|
| `POST /api/auth/login` | Usuario y contraseña. Si el usuario tiene MFA, devuelve un desafío en vez del token |
| `POST /api/auth/mfa/verificar` | Canjea el desafío + código TOTP por el token |
| `POST /api/auth/google` | Login con el ID token de Google |
| `GET /api/auth/facebook` → `GET /api/auth/facebook/callback` → `POST /api/auth/facebook/session` | Login con Facebook (OAuth2 con ticket de un solo uso) |
| `DELETE /api/auth/facebook` | Desvincula Facebook |
| `GET /api/auth/me` | Perfil del usuario del token |
| `POST /api/auth/register` | Alta de usuario (igual que antes; ver "Pendiente") |
| `POST /api/mfa/configurar`, `POST /api/mfa/activar`, `DELETE /api/mfa`, `GET /api/mfa/estado` | Gestión de MFA |
| `GET /.well-known/jwks.json` | Claves públicas para verificar los tokens (solo parte pública) |
| `GET /v3/api-docs` | Contrato OpenAPI del servicio |

## El token

- Firmado con **RS256**. La clave privada está en un archivo PEM que genera una sola vez el contenedor `identity-keygen`, en el volumen `andina_identity_keys`, que solo monta este servicio (usuario 1001, permiso 400).
- Claims: `sub` (usuario), `rol` (lo lee el frontend), `roles`, `customerId` (solo usuarios CLIENTE que son clientes registrados), `iss=andina-identity`, `iat`, `exp`, `jti`. El header lleva `kid`.
- `customerId` se obtiene del índice `customer_email_index`, que este servicio mantiene con los eventos `customer.registered.v1` y `customer.updated.v1` (cola `identity.customer.events`). Así ni identity consulta la base de clientes ni el backend consulta la de usuarios.

## Estado compartido entre réplicas

El `state` OAuth de Facebook (300 s), los tickets de login (60 s) y los desafíos MFA (300 s) viven en **Redis** (base 1) con TTL y se consumen con `GETDEL` (un solo uso, atómico). Por eso el servicio se puede escalar:

```bash
cd Arquitectura-Clean
docker compose up -d --scale identity-service=2
```

## Resiliencia hacia Google y Facebook

| Proveedor | Timeout | Circuit breaker | Reintentos | Bulkhead | Si falla |
|---|---|---|---|---|---|
| Google (claves públicas) | 3 s | `google` | No (las claves quedan en caché) | 20 llamadas | `503 GOOGLE_NO_DISPONIBLE`: entrar con contraseña |
| Facebook (canje del código) | 5 s | `facebook` | **No**: el código OAuth es de un solo uso | 10 llamadas | `503 FACEBOOK_NO_DISPONIBLE` |

Solo las caídas del proveedor cuentan para el circuito; un token o un código inválido no lo abre.

## Configuración

| Variable | Por defecto | Uso |
|---|---|---|
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/identity_db` | Base propia `identity_db` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_DATABASE` | `localhost` / `6379` / `1` | Estado efímero |
| `SPRING_RABBITMQ_*` | `localhost`, `andina` | Eventos `customer.*` |
| `JWT_PRIVATE_KEY_FILE` | `/run/keys/identity/private.pem` | Clave privada RS256 (PKCS#8) |
| `JWT_GENERATE_KEY_IF_MISSING` | `false` | Solo pruebas locales: genera una clave en memoria |
| `JWT_ISSUER` / `JWT_EXPIRATION_SECONDS` | `andina-identity` / `28800` | Emisor y duración del token |
| `APP_DEMO_DATA_ENABLED` | `false` | Crea el usuario demo `admin` si no existe (en Compose: `true`) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_JWK_SET_URI`, `GOOGLE_ISSUER` | — | Login con Google |
| `FACEBOOK_*` | — | Login con Facebook (mismas variables que tenía el backend) |

## Migración de la colección `usuarios` (paso 2.7)

Copia la colección del backend a `identity_db` sin tocar la original y compara las dos copias (cantidad y huella SHA-256). Se puede repetir:

```bash
sh services/identity-service/migracion/migrar-usuarios.sh
# 3/3 OK: mismo numero de usuarios y mismo contenido. Se puede hacer el corte.
```

Los hashes BCrypt y los secretos MFA se copian tal cual: nadie cambia su contraseña ni reconfigura MFA.

## Pruebas

```bash
mvn test   # también corren al construir la imagen
```

Incluyen las reglas ArchUnit, la firma RS256, el `customerId`, el estado en Redis entre "réplicas", el circuit breaker y el bulkhead de Google.

## Pendiente (heredado del monolito)

- `POST /api/auth/register` sigue aceptando el rol desde el cliente (riesgo **S1** del análisis). Se mantuvo igual a propósito para que el servicio nuevo se comporte como el anterior; se corrige en el paso 0.2.
- Un usuario desactivado conserva acceso hasta que su token vence (el backend ya no consulta `usuarios` en cada petición). Mitigación futura: tokens más cortos o lista de revocación en Redis.
