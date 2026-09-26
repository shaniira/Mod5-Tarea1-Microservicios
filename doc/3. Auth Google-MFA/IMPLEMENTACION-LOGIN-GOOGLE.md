# Indicaciones de implementación: Login con Google (Backend Clean + Frontend Vue)

**Alcance:** `Arquitectura-Clean/` (backend) y `frontend/` (Vue 3 + Pinia + Vue Router)
**Objetivo:** agregar "Iniciar sesión con Google" como segunda vía de autenticación, sin romper el login local ni la Clean Architecture.
**Base de este documento:** verificación línea por línea del código actual (no solo el análisis teórico previo). Todo lo listado abajo como "estado actual" fue leído directamente de los archivos del repo en esta fecha (2026-09-07).

---

## 0. Resumen para exposición

Hoy el login es `username/password → BCrypt → JwtTokenAdapter → JWT propio`. Vamos a añadir una segunda puerta: `Google ID Token → verificación criptográfica → mismo JwtTokenAdapter → mismo JWT propio`. Google solo participa en el instante del login; todo lo que ya existe (filtro JWT, roles, `@PreAuthorize`, MongoDB) permanece intacto. Esto se logra con un **puerto de salida nuevo** (`GoogleIdentityVerifierPort`) que aísla el SDK/librería de Google detrás de la capa de casos de uso, tal como ya se hace con `PasswordEncoderPort` o `VehicleInformationPort`.

---

## 1. Estado actual verificado (backend `Arquitectura-Clean`)

| Archivo                                                         | Contenido relevante confirmado                                                                                                                                                                                                             |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `entities/model/Usuario.java`                                 | Campos:`id, username, passwordHash, rol, activo`. **No tiene `email` ni `googleSubject`.**                                                                                                                                     |
| `interfaceadapters/.../document/UsuarioDocument.java`         | Ya tiene`email` (`@Indexed(unique, sparse)`) además de `username, passwordHash, rol, activo`.                                                                                                                                       |
| `interfaceadapters/.../mapper/UsuarioMongoMapper.java`        | `email` se **infiere** de `username.contains("@")`, no se persiste como campo propio del dominio. Hay que corregir esto.                                                                                                         |
| `usecases/port/out/repository/UsuarioRepository.java`         | Solo`guardar()` y `buscarPorUsername()`. Falta `buscarPorEmail` / `buscarPorGoogleSubject`.                                                                                                                                        |
| `usecases/service/auth/AutenticarUsuarioUseCase.java`         | Compara`passwordEncoder.coincide(password, usuario.getPasswordHash())` **sin verificar null** → si se permite `passwordHash = null` para usuarios Google, esto debe protegerse.                                                 |
| `usecases/service/auth/RegistrarUsuarioUseCase.java`          | Crea usuario con rol tomado del request.                                                                                                                                                                                                   |
| `interfaceadapters/in/rest/request/CrearUsuarioRequest.java`  | **El cliente puede enviar `rol` libremente** (`ADMIN`, etc.) — riesgo de seguridad preexistente, no causado por Google, pero relevante porque el nuevo endpoint de Google debe evitar el mismo error.                           |
| `interfaceadapters/in/rest/controller/AuthController.java`    | Solo`POST /register` y `POST /login`.                                                                                                                                                                                                  |
| `frameworksdrivers/configuration/spring/SecurityConfig.java`  | `/api/auth/**` ya es `permitAll()`, CORS restringido a `app.cors.allowed-origins` (default `http://localhost:5173`), `STATELESS`, CSRF deshabilitado. **No requiere cambios de reglas** para agregar `/api/auth/google`. |
| `interfaceadapters/out/security/JwtTokenAdapter.java`         | Genera JWT con`subject = usuario.username()` y claim `rol`. Reutilizable tal cual para Google.                                                                                                                                         |
| `interfaceadapters/out/security/JwtAuthenticationFilter.java` | Valida JWT y busca al usuario por`buscarPorUsername(claims.username())`. **No se toca.**                                                                                                                                           |
| `pom.xml`                                                     | Spring Boot 3.3.5, JJWT 0.12.6,`spring-boot-starter-security`, ArchUnit 1.3.0 en test. **No hay ninguna dependencia de Google ni de `spring-security-oauth2-*` todavía.**                                                       |
| `application.yml`                                             | No existe ninguna clave`app.google.*`. Hay que añadirla.                                                                                                                                                                                |
| `src/test/.../architecture/CleanArchitectureTest.java`        | Existen pruebas ArchUnit reales — cualquier fuga de una clase de Google/Spring Security hacia`usecases` romperá el build.                                                                                                              |
| `RolUsuario`                                                  | Enum:`ADMIN, ACTUARIO, AGENTE, CLIENTE`.                                                                                                                                                                                                 |

**Conclusión:** el análisis previo (`analisisv1.md`) describe correctamente el estado del proyecto; se usa como base de diseño y aquí se traduce a instrucciones accionables y verificadas.

---

## 2. Estado actual verificado (frontend)

| Archivo                     | Contenido relevante                                                                                                                                                                                                                                   |
| --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `package.json`            | Vue 3.5, Pinia 2.3, vue-router 4.5, axios 1.7.**Sin ningún paquete de Google** (`@react-oauth/google` no aplica; para Vue no hay dependencia instalada).                                                                                     |
| `src/stores/auth.ts`      | Pinia store con`login(username, password)` que llama `api.post('/auth/login', ...)`, decodifica el JWT (payload) para extraer `role`, y persiste `token/role/username` en `localStorage`.                                                   |
| `src/views/LoginView.vue` | Formulario username/password minimalista (código en una sola línea, patrón usado en todo el proyecto). No hay botón social.                                                                                                                       |
| `src/services/api.ts`     | Instancia axios con interceptor que agrega`Authorization: Bearer <token>` y maneja expiración (401/403 `TOKEN_INVALIDO` → redirige a `/login`). Mapea códigos de error de negocio (`CREDENCIALES_INVALIDAS`, etc.) a mensajes en español. |
| `src/router/index.ts`     | Guard global: rutas no públicas requieren`isAuthenticated`; hay control de roles por ruta (`meta.roles`).                                                                                                                                        |
| `frontend/.env.example`   | Solo`VITE_API_URL`. Falta `VITE_GOOGLE_CLIENT_ID`.                                                                                                                                                                                                |
| `index.html`              | HTML mínimo, sin scripts externos cargados. Aquí debe ir el`<script src="https://accounts.google.com/gsi/client">`.                                                                                                                               |

**Conclusión:** el frontend no tiene ninguna pieza de Google todavía; todo es trabajo nuevo, pero se integra limpiamente en el store y el servicio de API existentes sin rediseñar nada.

---

## 3. Decisión de arquitectura

**Google Identity Services (GIS) + intercambio de ID Token**, no `oauth2Login()` de Spring Security ni Authorization Code Flow gestionado por backend. Motivo:

- La API es `STATELESS` + JWT propio; introducir sesiones/redirects de OAuth2 sería más invasivo y luego igual habría que convertir la identidad Google en JWT propio.
- El frontend es una SPA con Vite; GIS entrega un ID Token vía callback JS, ideal para un simple `POST /api/auth/google`.
- El detalle de verificación queda encapsulado detrás de un puerto (`GoogleIdentityVerifierPort`), así que si en el futuro se requiere Authorization Code Flow, el cambio queda aislado al adaptador.

```
Frontend (botón "Continuar con Google")
   → Google Identity Services (JS) autentica al usuario
   → credential (Google ID Token, JWT firmado por Google)
   → POST /api/auth/google { idToken }
   → AuthController → AutenticarConGoogleUseCase
   → GoogleIdentityVerifierPort.verificar(idToken) → GoogleIdentity(sub, email, emailVerified, name)
   → UsuarioRepository (buscar por googleSubject / email; crear si no existe, rol=CLIENTE)
   → TokenGeneratorPort (JwtTokenAdapter, sin cambios) → JWT Andina
   → TokenResponse { token, tipo, expiraEnSegundos } — mismo contrato que /login
```

Tres tokens, no confundir: **Google ID Token** (prueba de identidad, solo en el login) vs **Google Access Token** (no se usa, no consumimos APIs de Google) vs **JWT Andina** (el único que viaja en `Authorization` en el resto de la API).

---

## 4. Cambios en el backend (`Arquitectura-Clean`)

### 4.1 `entities/model/Usuario.java` — MODIFICAR

Agregar `email` (String, nullable hasta migrar todos los usuarios) y `googleSubject` (String, nullable). `passwordHash` pasa a ser nullable (usuario 100% Google). Mantener el objeto inmutable como está hoy (constructor + getters, sin setters).

```java
public class Usuario {
    private final UUID id;
    private final String username;
    private final String email;          // nuevo
    private final String passwordHash;   // ahora puede ser null
    private final String googleSubject;  // nuevo, puede ser null
    private final RolUsuario rol;
    private final boolean activo;
    // constructor con los 7 campos + getters
}
```

> Cuidado: hoy `Usuario` se instancia en `RegistrarUsuarioUseCase`, `UsuarioMongoMapper.toDomain`, y en los tests (`AuthControllerTest`, `DemoCredentialsTest` si aplica). Al cambiar el constructor hay que actualizar **todos** los call sites — usar el compilador para encontrarlos todos, no solo grep. En este entorno de trabajo no hay Maven/JDK instalados localmente, solo Docker: la verificación de compilación se hace con `docker build` sobre `Arquitectura-Clean/Dockerfile` (que corre `mvn clean package` dentro del contenedor), no con `mvn -q compile` en el host.

### 4.2 `entities/enums/RolUsuario.java` — SIN CAMBIOS

`CLIENTE` ya existe y es el rol que se asignará a usuarios nuevos de Google.

### 4.3 `usecases/port/out/repository/UsuarioRepository.java` — MODIFICAR

Agregar:

```java
Optional<Usuario> buscarPorEmail(String email);
Optional<Usuario> buscarPorGoogleSubject(String googleSubject);
```

Sigue siendo un puerto de salida puro (sin imports de Mongo/Spring).

### 4.4 Persistencia Mongo — MODIFICAR (3 archivos)

- `UsuarioDocument.java`: agregar `@Indexed(unique = true, sparse = true) public String googleSubject;`. El campo `email` ya existe, no tocar su índice.
- `SpringDataUsuarioMongoRepository.java`: agregar `findByEmail(String email)` y `findByGoogleSubject(String googleSubject)`.
- `UsuarioMongoRepositoryAdapter.java`: implementar los dos nuevos métodos del puerto delegando al repositorio Spring Data.
- `UsuarioMongoMapper.java`: **corregir** — dejar de inferir `email` desde `username.contains("@")`; mapear `email` y `googleSubject` directamente desde/hacia el dominio en ambas direcciones (`toDocument`/`toDomain`).

### 4.5 Nuevo modelo de puerto — CREAR

`usecases/port/out/security/GoogleIdentity.java`

```java
public record GoogleIdentity(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String picture) {}
```

`usecases/port/out/security/GoogleIdentityVerifierPort.java`

```java
public interface GoogleIdentityVerifierPort {
    GoogleIdentity verificar(String idToken);
}
```

Este puerto no debe importar nada de Google SDK, Nimbus ni Spring Security — así ArchUnit (`usecases no dependen de Spring/Mongo`) sigue pasando.

### 4.6 DTOs del caso de uso — CREAR

`usecases/dto/GoogleLoginRequestModel.java`

```java
public record GoogleLoginRequestModel(String idToken) {}
```

### 4.7 Nuevo caso de uso — CREAR

`usecases/service/auth/AutenticarConGoogleUseCase.java`

Dependencias: `UsuarioRepository`, `GoogleIdentityVerifierPort`, `TokenGeneratorPort`, `IdGeneratorPort`.

Lógica (equivalente a la sección 18 del análisis previo, ya validada contra el código real):

1. `googleIdentityVerifier.verificar(idToken)` → si falla, lanzar `ReglaNegocioException("GOOGLE_TOKEN_INVALIDO", ...)` (mismo patrón de excepción que usa `AutenticarUsuarioUseCase` con `CREDENCIALES_INVALIDAS`, capturado por `GlobalExceptionHandler` existente).
2. Si `!identity.emailVerified()` → `ReglaNegocioException("GOOGLE_EMAIL_NO_VERIFICADO", ...)`.
3. Buscar por `googleSubject = identity.subject()`.
   - Si existe: verificar `activo` (si no, `ReglaNegocioException("USUARIO_INACTIVO", ...)`); generar JWT y retornar.
4. Si no existe por `googleSubject`, buscar por `email = identity.email()`.
   - Si existe una cuenta local sin `googleSubject`: **no vincular automáticamente** en el diseño recomendado → `ReglaNegocioException("CUENTA_EXISTENTE_REQUIERE_VINCULACION", ...)`. (Ver §6 para la alternativa simplificada de MVP académico.)
   - Si no existe ninguna cuenta: crear `Usuario` nuevo con `rol = RolUsuario.CLIENTE`, `activo = true`, `passwordHash = null`, `googleSubject = identity.subject()`, `email = identity.email()`, `username = identity.email()` (reutiliza `buscarPorUsername` y el filtro JWT sin tocarlos).
5. Construir `AuthenticatedUser(usuario.getUsername(), usuario.getRol().name())` y llamar `tokenGenerator.generar(...)` — **idéntico a `AutenticarUsuarioUseCase`**, no se toca `JwtTokenAdapter`.
6. Retornar `TokenResponse` (misma clase de `usecases/dto/Responses.java` ya usada por login local).

### 4.8 Ajuste en `AutenticarUsuarioUseCase.java` — MODIFICAR

Si `passwordHash` puede ser `null` (usuario solo-Google que intenta loguearse por el flujo local), evitar NPE en `passwordEncoder.coincide(...)`:

```java
if (!usuario.isActivo() || usuario.getPasswordHash() == null
        || !passwordEncoder.coincide(solicitud.password(), usuario.getPasswordHash())) {
    throw credencialesInvalidas();
}
```

### 4.9 Adaptador de verificación Google — CREAR

`interfaceadapters/out/security/google/GoogleIdentityVerifierAdapter.java`, implementa `GoogleIdentityVerifierPort`.

Responsabilidad: validar firma (claves públicas JWKS de Google), `iss` (`https://accounts.google.com` o `accounts.google.com`), `aud` (= `GOOGLE_CLIENT_ID`), `exp`, y extraer `sub/email/email_verified/name/picture`.

**Opción de librería:** usar `spring-security-oauth2-jose` (`NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")` + `JwtValidators` para `iss`/`aud`/`exp`) para no reinventar la verificación JWKS. Alternativa: cliente oficial `google-api-client` (`GoogleIdTokenVerifier`), que internamente hace lo mismo. Cualquiera de las dos queda encerrada en este único archivo — el use case no sabrá cuál se usó.

Errores de verificación (firma inválida, `aud`/`iss` incorrectos, expirado) deben capturarse aquí y traducirse a una excepción de dominio simple (o dejar que el use case las traduzca), **nunca** dejar escapar el stacktrace de Nimbus al cliente.

### 4.10 Nuevo DTO REST — CREAR

`interfaceadapters/in/rest/request/GoogleLoginRequest.java`

```java
public record GoogleLoginRequest(@NotBlank String idToken) {}
```

Actualizar `RestRequestMapper` con `toCore(GoogleLoginRequest)` → `GoogleLoginRequestModel`, siguiendo el mismo patrón que ya existe para `LoginRequest`.

### 4.11 `AuthController.java` — MODIFICAR

Inyectar `AutenticarConGoogleUseCase` y agregar:

```java
@PostMapping("/google")
public TokenResponse google(@Valid @RequestBody GoogleLoginRequest solicitud) {
    return autenticarConGoogle.execute(toCore(solicitud));
}
```

Sin lógica adicional en el controller (mismo estilo que `login`/`register`).

### 4.12 `UseCaseConfig.java` — MODIFICAR

Registrar el bean del adaptador Google (con `@Value` para `GOOGLE_CLIENT_ID`/issuer) y el bean del nuevo caso de uso, siguiendo exactamente el patrón ya usado para `autenticarUsuario(...)`.

### 4.13 `application.yml` y `docker-compose.yml` — MODIFICAR

```yaml
app:
  google:
    client-id: ${GOOGLE_CLIENT_ID:}
    issuer: https://accounts.google.com
```

Y propagar `GOOGLE_CLIENT_ID` como variable de entorno en `Arquitectura-Clean/docker-compose.yml` (revisar cómo se pasa hoy `JWT_SECRET`/`CORS_ALLOWED_ORIGINS` y replicar el mismo mecanismo).

### 4.14 `pom.xml` — MODIFICAR

Agregar (si se opta por la ruta Spring):

```xml
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>
```

Spring Boot 3.3.5 gestiona la versión (BOM del parent), no hace falta fijarla.

### 4.15 `SecurityConfig.java` — SIN CAMBIOS FUNCIONALES

`/api/auth/**` ya es público; `/api/auth/google` queda cubierto automáticamente. No agregar `oauth2Login()` ni `oauth2ResourceServer()` — no se necesitan para este flujo (solo se usa el decoder JWT de Google dentro del adaptador, no como resource server de Spring Security).

### 4.16 `JwtTokenAdapter.java`, `JwtAuthenticationFilter.java`, `TokenGeneratorPort`, `TokenValidationPort`, `TokenResponse` — SIN CAMBIOS

Confirmado por lectura del código: ambos flujos (local y Google) convergen en el mismo `AuthenticatedUser` → `TokenGeneratorPort.generar(...)`.

### 4.17 Riesgo preexistente a documentar (no bloqueante para Google)

`CrearUsuarioRequest.rol` permite que cualquier cliente público elija su rol en `/api/auth/register` (incluido `ADMIN`). No es causado por esta funcionalidad, pero **la regla que se aplicará al usuario nuevo de Google (`rol` fijo en `CLIENTE`, decidido por el backend, nunca por el request)** deja en evidencia esta inconsistencia. Se recomienda corregirla en un cambio aparte (registro público → siempre `CLIENTE`; crear `ADMIN/AGENTE/ACTUARIO` vía endpoint administrativo protegido con `@PreAuthorize`).

---

## 5. Cambios en el frontend (`frontend/`)

### 5.1 `index.html` — MODIFICAR

Cargar el script de Google Identity Services:

```html
<script src="https://accounts.google.com/gsi/client" async defer></script>
```

### 5.2 `.env.example` y `.env` — MODIFICAR

```
VITE_API_URL=http://localhost:8080/api
VITE_GOOGLE_CLIENT_ID=xxxxxxxxxxxx.apps.googleusercontent.com
```

### 5.3 `src/types/index.ts` — MODIFICAR (opcional pero recomendado)

Agregar un tipo mínimo para el callback de GIS, ya que el proyecto es TypeScript estricto (`vue-tsc -b` en `build`):

```ts
export interface GoogleCredentialResponse { credential: string }
```

### 5.4 `src/stores/auth.ts` — MODIFICAR

Añadir una acción hermana de `login`, reutilizando la misma lógica de decodificación de payload y persistencia ya existente (no duplicar `parseJwt`):

```ts
async loginWithGoogle(idToken: string) {
  const { data } = await api.post('/auth/google', { idToken });
  const payload = parseJwt(data.token);
  this.token = data.token;
  this.role = (payload.rol || payload.role || '') as Role;
  this.username = payload.sub || '';
  localStorage.setItem('token', this.token);
  localStorage.setItem('role', this.role);
  localStorage.setItem('username', this.username);
}
```

Nota: como el backend firma el JWT con `subject = username` (= email para usuarios Google), `payload.sub` ya trae el username correcto — no hace falta pedir nada extra al backend.

### 5.5 `src/views/LoginView.vue` — MODIFICAR

Agregar el botón de Google e inicializar GIS en `onMounted`, manteniendo el estilo compacto ya usado en el archivo (single-file, sin comentarios):

```html
<script setup lang="ts">
import {ref, onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {useAuthStore} from '@/stores/auth';
import {errorMessage} from '@/services/api';

const u = ref('admin'), p = ref('Admin123*'), loading = ref(false), error = ref('');
const a = useAuthStore(), r = useRouter();

async function go() {
  loading.value = true; error.value = '';
  try { await a.login(u.value.trim(), p.value); r.push('/dashboard'); }
  catch (e) { error.value = errorMessage(e); }
  finally { loading.value = false; }
}

async function handleGoogleCredential(response: { credential: string }) {
  loading.value = true; error.value = '';
  try { await a.loginWithGoogle(response.credential); r.push('/dashboard'); }
  catch (e) { error.value = errorMessage(e); }
  finally { loading.value = false; }
}

onMounted(() => {
  const g = (window as any).google;
  if (!g) return;
  g.accounts.id.initialize({
    client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
    callback: handleGoogleCredential
  });
  g.accounts.id.renderButton(document.getElementById('google-btn'), { theme: 'outline', size: 'large', width: 320 });
});
</script>
```

Y en el `<template>`, dentro del `login-card`, agregar un contenedor `<div id="google-btn"></div>` (por ejemplo entre el botón "Iniciar sesión" y el texto de credenciales de prueba).

Como el archivo actual usa TypeScript con tipos estrictos y `window.google` no tiene tipos oficiales sin instalar `@types/google.accounts`, usar `(window as any).google` es aceptable aquí (patrón común para GIS) o declarar un `d.ts` mínimo si se prefiere evitar `any`.

### 5.6 `src/services/api.ts` — SIN CAMBIOS

El interceptor de `Authorization` y el manejo de errores por `codigo` ya funcionan igual para la respuesta de `/auth/google` (mismo contrato `TokenResponse`). Solo hay que añadir los nuevos códigos de error al mapa `messagesByCode`:

```ts
GOOGLE_TOKEN_INVALIDO: 'No se pudo validar tu cuenta de Google. Intenta nuevamente.',
GOOGLE_EMAIL_NO_VERIFICADO: 'Tu correo de Google no está verificado.',
USUARIO_INACTIVO: 'Tu cuenta está inactiva. Contacta al administrador.',
CUENTA_EXISTENTE_REQUIERE_VINCULACION: 'Ya existe una cuenta con este correo. Inicia sesión con tu contraseña para vincular Google.',
```

### 5.7 `src/router/index.ts` — SIN CAMBIOS

El guard ya funciona por `isAuthenticated`/`role` sin importar el método de login usado.

---

## 6. Decisión pendiente que el usuario debe confirmar

El documento base propone, para el caso "existe una cuenta local con el mismo email pero sin Google vinculado", **no vincular automáticamente** (`CUENTA_EXISTENTE_REQUIERE_VINCULACION`), por ser la opción segura frente a account takeover. Para un entregable académico esto puede simplificarse a vinculación automática por email, pero debe quedar documentado explícitamente como simplificación, no como práctica de producción.

---

## 7. Google Cloud Console — configuración necesaria (una sola vez, no es código)

1. Crear/seleccionar proyecto (ej. `andina-seguros-dev`).
2. **Google Auth Platform → Branding**: nombre de app, correo de soporte.
3. **Google Auth Platform → Audience**: modo pruebas + cuentas autorizadas durante desarrollo.
4. **Google Auth Platform → Clients → Create Client**: tipo `Web application`.
5. **Authorized JavaScript origins**: `http://localhost:5173` (origen exacto, sin ruta). En producción, el dominio real.
6. Redirect URI: no aplica al flujo elegido (GIS + POST al backend).
7. Guardar el `Client ID` generado → usarlo como `GOOGLE_CLIENT_ID` (backend) y `VITE_GOOGLE_CLIENT_ID` (frontend). **No se necesita `Client Secret`** para este flujo.

---

## 8. Seguridad — reglas que debe cumplir la implementación

- Nunca confiar en datos enviados directamente por el frontend distintos del `idToken` (ni email, ni rol, ni sub).
- `googleSubject` (claim `sub`), no el email, es el identificador estable de la cuenta Google.
- Validar siempre `aud`, `iss`, `exp` y firma del ID Token — nunca solo decodificar Base64.
- Usuario nuevo por Google → rol `CLIENTE` fijo, decidido por el backend.
- No loguear el `idToken` completo (`log.info(... idToken ...)` está prohibido).
- HTTPS obligatorio en producción; `http://localhost` solo para desarrollo.
- CORS: mantener el whitelist actual (`app.cors.allowed-origins`), no usar `*`.

---

## 9. Impacto en Clean Architecture / ArchUnit

- **Entities** (`Usuario`): solo gana `email`/`googleSubject` como datos; sigue sin conocer Spring/Google/HTTP.
- **Use Cases**: conocen únicamente `GoogleIdentityVerifierPort`, `UsuarioRepository`, `TokenGeneratorPort`, `IdGeneratorPort` — nunca `NimbusJwtDecoder`, `google-api-client` ni clases de Spring Security.
- **Interface Adapters**: aquí vive `GoogleIdentityVerifierAdapter` (out/security/google), los cambios de Mongo, y el controller.
- **Frameworks & Drivers**: `UseCaseConfig`, `SecurityConfig`, `application.yml`.
- Tras implementar, correr `mvn test` (o, sin Maven/JDK local, `docker build` sobre `Arquitectura-Clean/Dockerfile`, que ejecuta `mvn clean package` dentro del contenedor) — `CleanArchitectureTest` debe seguir en verde. Si alguna clase de Google/Nimbus termina importada en `usecases`, la prueba arquitectónica debe fallar y así detectarlo.

---

## 10. Plan de pruebas

**Backend — unitarias `AutenticarConGoogleUseCase`:**

1. Token inválido (verifier lanza excepción) → `GOOGLE_TOKEN_INVALIDO`.
2. Usuario Google existente y activo → JWT emitido.
3. Usuario Google existente pero inactivo → rechazo.
4. Usuario Google nuevo (sin `sub` ni `email` previos) → se crea con `rol=CLIENTE`.
5. Email existente sin `googleSubject` vinculado → `CUENTA_EXISTENTE_REQUIERE_VINCULACION` (o vinculación automática si se opta por la simplificación del §6).
6. `email_verified = false` → rechazo.

**Backend — `GoogleIdentityVerifierAdapter`:** firma correcta/incorrecta, `aud` correcto/incorrecto, `iss` correcto/incorrecto, token vigente/expirado, `sub`/`email` presentes — usando claves de prueba controladas, no llamadas reales a Google.

**Backend — regresión:** `AuthControllerTest` y `mvn test` completo deben seguir pasando; agregar caso para `/api/auth/google`. Sin Maven/JDK local, esto se verifica con `docker build` sobre `Arquitectura-Clean/Dockerfile` (el build de la imagen falla si algún test falla).

**Frontend:** probar manualmente el botón de Google en `http://localhost:5173` contra un `GOOGLE_CLIENT_ID` real de pruebas, verificar que el JWT resultante funcione igual que el login local contra un endpoint protegido, y que el login local (`admin`/`Admin123*`) siga funcionando sin regresión.

---

## 11. Orden recomendado de implementación

1. Backend: evolucionar `Usuario` (entity) y actualizar todos los call sites que rompan la compilación.
2. Backend: Mongo (`UsuarioDocument`, `SpringDataUsuarioMongoRepository`, `UsuarioMongoRepositoryAdapter`, corregir `UsuarioMongoMapper`).
3. Backend: `GoogleIdentity` + `GoogleIdentityVerifierPort`.
4. Backend: dependencia Maven + `GoogleIdentityVerifierAdapter` (con tests con claves controladas).
5. Backend: `GoogleLoginRequestModel`, `AutenticarConGoogleUseCase` + tests.
6. Backend: `GoogleLoginRequest`, `RestRequestMapper`, `AuthController`, `UseCaseConfig`, `application.yml`.
7. Backend: ajustar `AutenticarUsuarioUseCase` para `passwordHash == null`.
8. Backend: build de la imagen Docker (`docker build` sobre `Arquitectura-Clean/Dockerfile`), que corre `mvn clean package` — compilación + suite de tests completa + `CleanArchitectureTest` — dentro del contenedor. No requiere Maven/JDK instalados en la máquina host (entorno de trabajo real: solo Docker disponible, sin Maven local).
9. Google Cloud Console: crear proyecto, branding, cliente OAuth, obtener `Client ID`.
10. Frontend: `.env`, `index.html`, tipos, `stores/auth.ts`, `LoginView.vue`, mensajes de error en `api.ts`.
11. Prueba end-to-end manual: login Google real → endpoint protegido con el JWT resultante → regresión de login local.

---

## 12. Criterios de aceptación

- [ ] El login tradicional sigue funcionando sin cambios de contrato.
- [ ] `POST /api/auth/google` acepta un ID Token válido y devuelve el mismo contrato `TokenResponse` que `/login`.
- [ ] Un token falso, con `aud` incorrecto, o vencido, es rechazado con un código de error controlado (nunca un stacktrace).
- [ ] `googleSubject` queda asociado y se usa como identificador estable (no el email).
- [ ] Un usuario Google nuevo se crea con `rol = CLIENTE`, sin que el frontend pueda influir en el rol.
- [ ] Un usuario inactivo no puede autenticarse por ninguna de las dos vías.
- [ ] El JWT emitido por el flujo Google funciona sin cambios en `JwtAuthenticationFilter` y con `@PreAuthorize` existente.
- [ ] Google no es consultado en ningún endpoint fuera del login.
- [ ] Ningún `idToken` ni `Client ID/Secret` aparece en logs.
- [ ] `mvn test` y `CleanArchitectureTest` pasan (verificado en este proyecto vía `docker build`, al no contar con Maven/JDK local).
- [ ] `vue-tsc -b` (build del frontend) pasa sin errores de tipos por el nuevo código.
