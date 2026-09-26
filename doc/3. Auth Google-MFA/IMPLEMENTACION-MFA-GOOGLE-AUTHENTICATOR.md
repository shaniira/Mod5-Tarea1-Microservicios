# Autenticación en dos pasos con Google Authenticator (MFA)

**Proyecto:** `Arquitectura-Clean` (backend) + `frontend` (Vue 3)
**Fecha de este análisis:** 2026-09-07
**Este documento reemplaza** a la versión anterior de `IMPLEMENTACION-MFA-GOOGLE-AUTHENTICATOR.md`, fusionando lo mejor de ese primer borrador con una verificación línea por línea del código real del proyecto (incluye una sección de comparación en el §2).

---

# PARTE 1 — Para entender de qué se trata (sin conocimientos previos)

## 1.1 ¿Qué problema resolvemos?

Hoy, para entrar al sistema, basta con **una sola cosa**: tu contraseña, o tu cuenta de Google. Si alguien más consigue esa contraseña (te la roban, la adivinan, la reusaste en otro sitio que fue hackeado), esa persona puede entrar exactamente igual que tú.

**MFA** ("Multi-Factor Authentication", autenticación de múltiples factores) agrega un **segundo candado** que no depende de la contraseña: un código de 6 dígitos que solo tu teléfono puede generar en ese momento. Aunque alguien robe tu contraseña, **no puede entrar** sin tener también tu teléfono físico en la mano.

Es la misma idea que un cajero automático: necesitas la **tarjeta** (algo que tienes) *y* el **PIN** (algo que sabes). Con MFA aquí sería: tu **contraseña o cuenta de Google** (algo que sabes / algo que ya demostraste que controlas) *y* el **código de tu teléfono** (algo que tienes).

## 1.2 ¿Qué es "Google Authenticator" exactamente?

Es una aplicación gratuita de celular (hay equivalentes: Authy, Microsoft Authenticator, 1Password) que genera un número de 6 dígitos que **cambia cada 30 segundos**. La magia es que ese número no es aleatorio de verdad: se calcula con una fórmula matemática que combina dos cosas:

1. Un **secreto** — un código largo que solo el servidor y esa app conocen (se comparte una única vez, al momento de configurar el celular, normalmente escaneando un **código QR**).
2. La **hora actual** — como ambos (servidor y celular) usan la misma fórmula con el mismo secreto y saben qué hora es, ambos llegan al mismo número de 6 dígitos, sin necesidad de que el celular esté conectado a internet en ese instante.

A este mecanismo se le llama técnicamente **TOTP** ("Time-based One-Time Password" — contraseña de un solo uso basada en tiempo). Es un estándar abierto (documentado públicamente como RFC 6238), **no es una tecnología exclusiva de Google** — Google Authenticator es solo el nombre de una de las aplicaciones más populares que lo implementan. Cualquier app compatible funciona igual de bien.

> **Importante para no confundir dos cosas parecidas:** el "Login con Google" que ya implementamos en `IMPLEMENTACION-LOGIN-GOOGLE.md` es un mecanismo completamente distinto (usa tu cuenta de Google real, con internet, para probar quién eres). El "Google Authenticator" de este documento es solo una app que genera números, no necesita tu cuenta de Google en absoluto y funciona sin internet. Son dos capas independientes que en este plan se combinan: **primero demuestras quién eres (contraseña o cuenta de Google), después demuestras que tienes el teléfono (código TOTP)**.

## 1.3 Glosario rápido de términos técnicos usados en este documento

| Término | Qué significa en simple |
|---|---|
| **Backend** | El programa que corre en el servidor (`Arquitectura-Clean`, escrito en Java/Spring Boot) — el "cerebro" que decide si dejas entrar o no. |
| **Frontend** | La página web que ve el usuario en el navegador (`frontend`, escrito en Vue) — la "cara visible". |
| **JWT** | Un "boleto" digital firmado que el backend te entrega después de iniciar sesión correctamente. Lo guardas en el navegador y lo mandas en cada petición como prueba de que ya iniciaste sesión — así el backend no tiene que pedirte la contraseña en cada clic. |
| **TOTP** | El código de 6 dígitos que cambia cada 30 segundos, explicado en el §1.2. |
| **QR** | El cuadrito de pixeles blanco y negro que escaneas con la app — adentro solo tiene codificado el "secreto" y algunos datos, en un formato de texto que la app entiende. |
| **Clean Architecture** | El estilo de organización del código de este backend: separa "las reglas del negocio" (qué es válido, qué no) de "los detalles técnicos" (bases de datos, librerías externas), para poder cambiar los detalles técnicos sin tocar las reglas del negocio. |
| **Puerto / Adaptador** | En Clean Architecture, un "puerto" es un contrato (una interfaz de Java) que dice "necesito algo que sepa hacer X", sin decir cómo. Un "adaptador" es quien realmente sabe cómo (usando una librería concreta). Así, las reglas de negocio nunca dependen directamente de una librería externa. |
| **ArchUnit** | Un conjunto de pruebas automáticas que revisan que nadie haya roto las reglas de Clean Architecture (por ejemplo, que una librería externa no se haya "colado" donde no debería). |
| **BCrypt** | El algoritmo que ya usa este proyecto para no guardar contraseñas en texto plano — guarda una versión "revuelta" matemáticamente irreversible. |
| **Mongo / MongoDB** | La base de datos donde este proyecto guarda usuarios, clientes, pólizas, etc. |
| **Endpoint** | Una "dirección" específica a la que el frontend le puede pedir algo al backend, por ejemplo `POST /api/auth/login`. |

---

# PARTE 2 — Comparación con el borrador anterior

Ya existía un primer plan en `doc/Auth Google/IMPLEMENTACION-MFA-GOOGLE-AUTHENTICATOR.md` (enfocado en el requisito académico: *"Agregar autenticación con una red social y autenticación multifactor (MFA) mediante Google Authenticator"*). Es un plan sólido, con buenas ideas — algunas se incorporan aquí. Estas son las diferencias encontradas y por qué se resolvieron de una forma u otra:

| Punto | Borrador anterior | Este documento | Por qué |
|---|---|---|---|
| **Nombres de campos en las respuestas** | Mezcla inglés/español: `requiresMfa`, `challengeToken`, `tokenType`, `expiresIn` | Español, igual que el resto del código: `mfaRequerido`, `desafioId`, `tipo`, `expiraEnSegundos` | El `TokenResponse` que **ya existe** en el proyecto usa `tipo`/`expiraEnSegundos` en español. Mezclar convenciones en un mismo contrato de API genera inconsistencia y confusión para cualquiera que lea el código después. |
| **Diseño de los puertos de seguridad** | 4 puertos separados: `MfaSecretGeneratorPort`, `TotpVerifierPort`, `QrCodeGeneratorPort`, `MfaChallengePort` | 2 puertos (`TotpPort` agrupa generar+verificar, `QrCodePort` aparte) + una entidad de dominio (`DesafioMfa`) con su propio repositorio | Ambos enfoques son válidos en Clean Architecture. Se prefiere agrupar generación y verificación TOTP en un solo puerto porque son la misma responsabilidad conceptual (todo el ciclo de vida del código TOTP), reduciendo la cantidad de clases nuevas sin perder aislamiento. El desafío de MFA se modela como una **entidad de dominio con repositorio propio** (mismo patrón que `Cliente`, `Poliza`, etc. en este proyecto) en vez de como un "puerto de seguridad", porque conceptualmente es un dato de negocio con ciclo de vida (se crea, se consulta, expira, se borra), no un servicio técnico externo. |
| **Cómo implementar el "desafío" temporal** | Sugiere "tokens firmados de vida corta" con un identificador `jti` registrado y consumido, aclarando que no debe compartir el propósito del JWT de sesión | Un identificador aleatorio simple (UUID) guardado en Mongo, **que nunca pasa por el mecanismo de JWT** | Mismo objetivo (que el desafío nunca sirva como sesión completa), pero un id aleatorio guardado en base de datos es más simple de razonar y más difícil de implementar mal: no existe ninguna función que "decodifique" un UUID aleatorio como si fuera una sesión válida, mientras que con un token firmado siempre existe el riesgo de que alguien reutilice sin querer la misma función de validación de JWT y abra el hueco de seguridad que se quiere evitar (ver §5 de este documento). |
| **Ubicación de los endpoints de configuración de MFA** | Todo bajo `/api/auth/mfa/**`, lo que obliga a agregar una excepción explícita en `SecurityConfig` (porque `/api/auth/**` hoy es público) | Los endpoints de *configurar/activar/desactivar* MFA van bajo `/api/mfa/**` (fuera de `/api/auth/**`); solo *verificar* el código del desafío va en `/api/auth/mfa/verificar` | Al quedar fuera de `/api/auth/**`, los endpoints de configuración caen automáticamente bajo la regla ya existente `.anyRequest().authenticated()` — **no hace falta tocar `SecurityConfig` en absoluto** para eso, evitando el riesgo de configurar mal una excepción dentro de una regla pensada para ser pública. |
| **Endpoint de estado (`GET /mfa/status`)** | Sí lo incluye | Se incorpora aquí también (§6.12) | Buena adición del borrador anterior: permite que el frontend sepa si mostrar "Activar MFA" o "Desactivar MFA" sin exponer el secreto. |
| **Cifrado del secreto en la base de datos** | Menciona explícitamente que en producción debería cifrarse, y que para el entregable académico hay que documentar si se guarda sin cifrar | Se incorpora como recomendación explícita en §7.6 | Es una observación de seguridad válida que el primer borrador no debía perderse. |
| **Compatibilidad con documentos Mongo existentes** | Aclara que la ausencia de `mfaHabilitado` en documentos viejos debe leerse como `false` | Se confirma y se explica por qué eso ya funciona así automáticamente (§7.7) | Es correcto y vale la pena explicar la razón técnica exacta, no solo afirmarlo. |
| **Casos de prueba y guion de prueba manual** | Lista muy completa de 14 pruebas unitarias + guion de 16 pasos + lista de entregables para la entrega académica | Se incorporan casi íntegros en §9 y §11 | Es contenido de alta calidad que no tenía sentido reescribir — se conserva y se ajusta a los nombres de clases finales de este documento. |
| **Base del análisis** | Descripción general del proyecto, sin cifras ni líneas exactas | Verificación línea por línea del código actual el mismo día (§3), incluyendo el hallazgo de que hay **11 sitios de test** que construyen `Usuario` directamente y dejarán de compilar | Sin esta verificación puntual, es fácil subestimar cuánto código hay que tocar al agregar 2 campos a una entidad que ya se usa en 15+ lugares. |

**En resumen:** las diferencias son de *forma* (nombres, cuántos puertos, dónde viven los endpoints) más que de *fondo* — ambos documentos coinciden en la arquitectura general (factor 1 → ¿tiene MFA? → desafío → factor 2 → JWT). Este documento adopta las decisiones que reducen el riesgo de errores de seguridad y de inconsistencia con el código ya existente.

---

# PARTE 3 — Estado actual verificado del proyecto (2026-09-07)

Leído directamente del código, no de memoria — el proyecto tuvo cambios importantes en la sesión donde se implementó el login con Google (nuevos campos en `Usuario`, nuevo portal de cliente, nuevos roles).

| Archivo | Estado actual relevante |
|---|---|
| `entities/model/Usuario.java` | Constructor de **7 parámetros**: `(id, username, email, passwordHash, googleSubject, rol, activo)`. Sin campos de MFA. Es inmutable y no tiene builder — cualquier "copia con un campo distinto" se arma llamando de nuevo al constructor completo con los 7 valores. |
| `interfaceadapters/.../UsuarioDocument.java` | Mismos 7 campos, sin MFA. |
| `JwtTokenAdapter.java` | Genera JWT con `subject=username` y claim `rol`. **No existe ningún concepto de "token parcial" o "pendiente de segundo factor".** |
| `JwtAuthenticationFilter.java` | Trata **cualquier** JWT válido como sesión completa (ver por qué esto es crítico en la Parte 5). |
| `AutenticarUsuarioUseCase.java` | Login local: valida contraseña → genera JWT → responde. Sin punto de corte para MFA. |
| `AutenticarConGoogleUseCase.java` | Login Google: verifica identidad → busca/crea/vincula `Usuario` → genera JWT → responde. Tampoco tiene punto de corte para MFA. **Detalle importante:** en el método que vincula una cuenta existente (`vincularGoogleAUsuarioExistente`) se reconstruye el objeto `Usuario` copiando campo por campo — cualquier campo nuevo debe copiarse ahí explícitamente o se perderá en cada login con Google (ver riesgo detallado en §7.1). |
| `AuthController.java` | Solo `/register`, `/login`, `/google`. |
| `UseCaseConfig.java` / `SecurityConfig.java` | Sin nada relacionado a MFA. |
| `pom.xml` | Sin ninguna librería TOTP ni de generación de QR. |
| `usecases/dto/Responses.java` | `TokenResponse(token, tipo, expiraEnSegundos)` es la única respuesta de login. |
| `CleanArchitectureTest.java` | Reglas activas: `entities`/`usecases` no pueden depender de frameworks; todo lo que termine en `*Port`/`*Repository` dentro de `usecases.port.out` debe ser interfaz; los controladores deben vivir en `interfaceadapters.in.rest`. |
| Búsqueda de "mfa"/"totp"/"authenticator" | **Cero coincidencias** en backend y frontend — funcionalidad 100% nueva. |
| Sitios que construyen `new Usuario(...)` | **11 sitios en 3 archivos de test**, más 4 sitios en código de producción. Si el constructor de `Usuario` crece, los 15 dejan de compilar hasta actualizarlos. |
| `frontend/src/stores/auth.ts` | `login`/`loginWithGoogle` siempre terminan generando una sesión completa en una sola llamada. Sin estado intermedio "pendiente de MFA". |
| `frontend/src/views/LoginView.vue` | Tras login exitoso, navega directo al dashboard o a "Mi cuenta". Sin paso de código intermedio. |
| `frontend/package.json` | Sin ninguna librería de QR. |

---

# PARTE 4 — Diseño final del flujo

```text
Login con contraseña o con Google (sin cambios en cómo se valida el primer factor)
        │
        ▼
¿El usuario tiene MFA habilitado?
   │                              │
   NO                            SÍ
   │                              │
   ▼                              ▼
JWT final                Se crea un "desafío" temporal:
(exactamente             un identificador aleatorio (no es
como hoy)                un JWT), válido 5 minutos, ligado
                          a ese usuario, guardado en la base
                          de datos
                              │
                              ▼
                  Respuesta: { mfaRequerido: true, desafioId: "..." }
                  (todavía NO se entrega ningún JWT)
                              │
                              ▼
                  El frontend muestra: "Ingresa el código de tu app"
                              │
                              ▼
                  POST /api/auth/mfa/verificar { desafioId, codigo }
                              │
                              ▼
                  Se recalcula el código esperado con el secreto
                  guardado de ese usuario y se compara
                        │                    │
                     inválido              válido
                        │                    │
                        ▼                    ▼
                 se cuenta como       se borra el desafío
                 intento fallido      y AHORA sí se genera
                 y se rechaza         el JWT final (con el
                                      mismo mecanismo de
                                      siempre)
```

**Para activar MFA la primera vez** (el usuario ya está logueado con su JWT normal, va a su configuración de seguridad):

```text
1. Pide activar MFA → el backend genera un secreto nuevo y lo guarda
   (todavía inactivo) → responde con el código QR y el secreto en texto
2. El usuario escanea el QR con Google Authenticator
3. El usuario escribe el primer código de 6 dígitos que le aparece en la app
4. El backend confirma que el código es correcto → recién ahí queda
   MFA activo de verdad para ese usuario
```

---

# PARTE 5 — El hallazgo de seguridad más importante de este diseño

`JwtAuthenticationFilter` (el "guardia" que revisa cada petición al backend) **no distingue** entre "ya inicié sesión completamente" y "acabo de dar mi contraseña pero me falta el código" — cualquier JWT válido y firmado es tratado como una sesión completa y da acceso a toda la API.

Por eso, el estado intermedio "ya pasé el primer factor, me falta el segundo" **nunca puede representarse con un JWT**, ni siquiera con un JWT "especial" con una marca distinta — bastaría con que alguien, en el futuro, olvide revisar esa marca en un solo lugar del código para que el segundo factor deje de servir de nada.

**La solución de este documento:** el "desafío" temporal es simplemente un identificador aleatorio guardado en la base de datos (una fila más, como cualquier otro dato del sistema), completamente ajeno al mecanismo de JWT. No hay ninguna función en el sistema que pueda "convertir por error" ese identificador en una sesión válida, porque son dos mecanismos totalmente distintos y separados.

---

# PARTE 6 — Cambios en el backend, archivo por archivo

## 6.1 Librerías nuevas (`pom.xml`)

```xml
<dependency>
  <groupId>dev.samstevens.totp</groupId>
  <artifactId>totp</artifactId>
  <version>1.7.1</version>
</dependency>
<dependency>
  <groupId>com.google.zxing</groupId>
  <artifactId>core</artifactId>
  <version>3.5.3</version>
</dependency>
<dependency>
  <groupId>com.google.zxing</groupId>
  <artifactId>javase</artifactId>
  <version>3.5.3</version>
</dependency>
```

- **`dev.samstevens.totp`**: calcula y verifica los códigos TOTP (la fórmula del §1.2), y arma el texto `otpauth://...` que representa el secreto en un formato que cualquier app autenticadora entiende.
- **ZXing** (`core` + `javase`): dibuja el código QR como una imagen PNG. **Importante:** el QR se genera **dentro del propio backend**, nunca llamando a un servicio externo (como la vieja API de Google Charts) — eso enviaría el secreto por internet a un tercero, exactamente lo que se quiere evitar.

Ninguna de estas dos librerías debe importarse jamás desde `entities` ni `usecases` — solo dentro de los adaptadores nuevos (§6.5), igual que ya pasa con la librería de Google (Nimbus) usada para el login social.

## 6.2 `entities/model/Usuario.java` — MODIFICAR

Agregar dos campos: `mfaSecret` (texto, puede ser nulo) y `mfaHabilitado` (verdadero/falso). El constructor pasa de 7 a **9 parámetros**.

> **Recomendación:** esta es la tercera vez que este constructor crece (antes fue por email, luego por la cuenta de Google). Dado que ya se identificó el riesgo concreto de "olvidar copiar un campo nuevo" al reconstruir un usuario (§3, fila de `AutenticarConGoogleUseCase`), conviene aprovechar este cambio para agregar un par de métodos que devuelvan una copia del usuario con un solo campo distinto (por ejemplo `conMfaHabilitado(boolean nuevoValor)`), en vez de reescribir los 9 campos a mano en cada lugar que necesite modificar uno solo. No es obligatorio para que MFA funcione, pero reduce mucho el riesgo de que un campo se "pierda" por accidente en este cambio o en el siguiente.

## 6.3 `entities/model/DesafioMfa.java` — CREAR

La entidad de dominio del "desafío" explicado en la Parte 5:

```java
public class DesafioMfa {
    private final String id;              // aleatorio, no es un JWT
    private final String username;
    private final Instant expiraEn;
    private int intentos;

    public boolean expirado(Instant ahora) { return ahora.isAfter(expiraEn); }
    public boolean agotoIntentos() { return intentos >= 5; }
    public void registrarIntentoFallido() { intentos++; }
}
```

## 6.4 Persistencia Mongo — MODIFICAR / CREAR

- `UsuarioDocument.java`: agregar `mfaSecret` (String) y `mfaHabilitado` (boolean).
- `UsuarioMongoMapper.java`: mapear los 2 campos nuevos en ambas direcciones.
- `interfaceadapters/.../document/DesafioMfaDocument.java` — nuevo documento Mongo, con un **índice TTL** (`expireAfterSeconds`) para que los desafíos vencidos se borren solos automáticamente, igual que cualquier dato temporal.
- `SpringDataDesafioMfaMongoRepository` + `DesafioMfaMongoRepositoryAdapter` — mismo patrón que el resto del proyecto.

> **Sobre documentos ya existentes en la base de datos:** los usuarios que ya existen (como `admin`) no tienen el campo `mfaHabilitado` guardado todavía. Esto **no rompe nada**: Mongo/Spring Data, al leer un documento donde falta un campo booleano, lo interpreta automáticamente como `false` (el valor por defecto de Java para `boolean`). Es decir, todos los usuarios actuales seguirán entrando en un solo paso hasta que activen MFA explícitamente.

## 6.5 Nuevos puertos y adaptadores

```text
usecases/port/out/security/TotpPort.java
usecases/port/out/security/QrCodePort.java
usecases/port/out/repository/DesafioMfaRepository.java

interfaceadapters/out/security/mfa/TotpAdapter.java        (usa dev.samstevens.totp)
interfaceadapters/out/security/mfa/ZxingQrCodeAdapter.java (usa ZXing)
interfaceadapters/out/persistence/mongodb/adapter/DesafioMfaMongoRepositoryAdapter.java
```

```java
public interface TotpPort {
    String generarSecreto();
    String generarUriProvisionamiento(String secreto, String cuenta, String emisor);
    boolean verificar(String secreto, String codigo);
}

public interface QrCodePort {
    byte[] generarPng(String contenido, int tamanoPx);
}
```

Configuración TOTP recomendada (estándar, compatible con Google Authenticator):

- Algoritmo: HMAC-SHA1
- Periodo: 30 segundos
- Longitud del código: 6 dígitos
- Tolerancia: aceptar también el código del periodo inmediatamente anterior (por si hay un pequeño desfase de reloj entre el celular y el servidor)

## 6.6 Nuevo resultado de login (para poder "responder dos cosas distintas" desde el mismo endpoint)

```java
// usecases/dto/Responses.java
public sealed interface ResultadoLogin {
    record LoginExitoso(TokenResponse token) implements ResultadoLogin {}
    record LoginRequiereMfa(String desafioId) implements ResultadoLogin {}
}
```

Solo `AutenticarUsuarioUseCase.execute(...)` cambia su tipo de retorno de `TokenResponse` a `ResultadoLogin`. `AutenticarConGoogleUseCase.execute(...)` conserva `TokenResponse`: el segundo factor de esta aplicación se exige únicamente en el acceso con usuario y contraseña.

## 6.7 Nuevos casos de uso

```text
usecases/service/mfa/ConfigurarMfaUseCase.java
usecases/service/mfa/ActivarMfaUseCase.java
usecases/service/mfa/DesactivarMfaUseCase.java
usecases/service/mfa/ObtenerEstadoMfaUseCase.java
usecases/service/auth/VerificarMfaUseCase.java
```

| Caso de uso | Qué hace |
|---|---|
| `ConfigurarMfaUseCase` | Usuario ya logueado pide activar MFA → genera secreto nuevo, lo guarda con `mfaHabilitado=false` (todavía no exigido), arma el QR y responde con `{secreto, otpauthUri, qrPngBase64}`. |
| `ActivarMfaUseCase` | Recibe el primer código que el usuario ve en su app → si coincide con el secreto pendiente, pone `mfaHabilitado=true`. Si no coincide, el secreto queda tal cual (el usuario simplemente reintenta con el código siguiente, que cambia cada 30s). |
| `DesactivarMfaUseCase` | Exige un código TOTP **vigente** (no solo el JWT de sesión) antes de borrar el secreto y desactivar — así, alguien que solo robó el JWT de sesión (sin el teléfono) no puede apagar la protección. |
| `ObtenerEstadoMfaUseCase` | Responde si el usuario tiene MFA activo o no, **sin** exponer el secreto — para que el frontend sepa si mostrar "Activar" o "Desactivar". |
| `VerificarMfaUseCase` | Recibe `{desafioId, codigo}` durante el login → busca el desafío, revisa que no haya expirado ni agotado intentos, busca al usuario, revisa que siga activo, verifica el código, y si todo es correcto recién ahí genera el JWT final. |

## 6.8 Modificar únicamente el login local

Justo antes de generar el JWT, después de validar la contraseña y confirmar que el usuario está activo:

```text
si usuario.mfaHabilitado:
    crear un DesafioMfa nuevo, guardarlo
    devolver LoginRequiereMfa(desafio.id)
si no:
    devolver LoginExitoso(tokenResponse)   ← comportamiento actual, sin cambios
```

`AutenticarConGoogleUseCase` sigue emitiendo el JWT directamente, incluso si la cuenta tiene MFA configurado. No obstante, al reconstruir el usuario en `vincularGoogleAUsuarioExistente(...)` debe conservar `mfaSecret` y `mfaHabilitado`, para que el MFA continúe activo en futuros accesos locales.

## 6.9 Nuevos endpoints REST

```http
POST   /api/mfa/configurar        (requiere sesión ya iniciada)
POST   /api/mfa/activar           (requiere sesión ya iniciada)
POST   /api/mfa/desactivar        (requiere sesión ya iniciada)
GET    /api/mfa/estado            (requiere sesión ya iniciada)
POST   /api/auth/mfa/verificar    (público — todavía no hay sesión, se valida con el desafío)
```

`/api/mfa/**` cae automáticamente bajo la regla existente `.anyRequest().authenticated()` — **no se necesita tocar `SecurityConfig`**. `/api/auth/mfa/verificar` ya queda cubierto por el matcher público existente `/api/auth/**`.

## 6.10 Códigos de error nuevos

```text
MFA_CODIGO_INVALIDO        → el código de 6 dígitos no coincide
MFA_DESAFIO_INVALIDO       → el desafioId no existe o ya se usó
MFA_DESAFIO_EXPIRADO       → pasaron más de 5 minutos
MFA_DESAFIO_AGOTADO        → se superó el máximo de intentos fallidos
MFA_YA_HABILITADO          → se pidió configurar MFA pero ya estaba activo
MFA_NO_CONFIGURADO         → se pidió desactivar MFA sin tenerlo activo
```

Nunca deben aparecer en logs ni en ningún mensaje de error: el secreto TOTP completo, el código de 6 dígitos ingresado, ni el `desafioId` completo en registros compartidos.

## 6.11 `UseCaseConfig.java`

Registrar los beans de los 2 adaptadores nuevos (`TotpAdapter`, `ZxingQrCodeAdapter`), el repositorio de desafíos, y los 5 casos de uso nuevos — mismo patrón `@Bean` que se usa para todo lo demás en este archivo.

## 6.12 `application.yml`

```yaml
app:
  mfa:
    emisor: "Andina Seguros"
    desafio-expiracion-segundos: 300
    max-intentos: 5
```

---

# PARTE 7 — Seguridad: puntos a los que hay que prestar atención

1. **El desafío nunca debe funcionar como sesión** (ya explicado a fondo en la Parte 5).
2. **Copiar los campos de MFA en cada reconstrucción de `Usuario`** — especialmente en el login con Google (§6.8).
3. **Nunca revelar por adelantado si una cuenta tiene MFA activo** — el mensaje `mfaRequerido:true` solo debe aparecer **después** de validar correctamente la contraseña o la cuenta de Google, nunca antes (si no, alguien podría usar el propio login para "adivinar" qué cuentas tienen MFA activo).
4. **Nunca registrar el secreto ni el código en logs.**
5. **El secreto solo se muestra una vez**, en el momento de `/mfa/configurar` — ninguna respuesta posterior (ni `/mfa/estado`) debe volver a incluirlo.
6. **Cifrado del secreto en la base de datos:** en un sistema real de producción, el secreto TOTP debería guardarse cifrado (no en texto plano) dentro de Mongo, para que ni siquiera alguien con acceso directo a la base de datos pueda generar códigos válidos. Para este proyecto académico, si se decide no cifrarlo por simplicidad, debe **documentarse explícitamente esa decisión** en el informe final como una limitación consciente, no como un descuido.
7. **Compatibilidad con usuarios existentes sin campo de MFA:** confirmado en §6.4 — no requiere ninguna migración manual, Mongo/Spring Data lo interpreta como `false` automáticamente.
8. **Límite de intentos por desafío** (`max-intentos`) mitiga que alguien intente adivinar el código a fuerza bruta durante el login; queda documentado como posible mejora futura un límite adicional por cuenta/IP a lo largo del tiempo.
9. **Códigos de recuperación (backup codes):** sin ellos, un usuario que pierda el teléfono queda bloqueado permanentemente (nadie puede generarle un código válido sin el secreto). Se documenta como una función estándar de los sistemas MFA reales, que queda **fuera del alcance de este proyecto académico** por decisión consciente, no por descuido.

---

# PARTE 8 — Cambios en el frontend

| Archivo | Cambio |
|---|---|
| `frontend/src/stores/auth.ts` | `login`/`loginWithGoogle` deben revisar si la respuesta trae `mfaRequerido:true`; si es así, guardar el `desafioId` (en memoria, no como si fuera la sesión) y **no** marcar al usuario como autenticado todavía. Nueva acción `verifyMfa(codigo)` que llama a `/api/auth/mfa/verificar` y recién ahí guarda la sesión real. |
| `frontend/src/views/LoginView.vue` | Si el store queda en estado "pendiente de MFA", mostrar un paso adicional: un campo para escribir el código de 6 dígitos, en vez de navegar directo al dashboard. |
| Nueva vista, p. ej. `frontend/src/views/MfaSetupView.vue` | Pantalla de configuración: muestra el QR (`<img :src="'data:image/png;base64,'+qr">`), el secreto en texto como respaldo manual, un campo para confirmar el primer código, y un botón para desactivar (pidiendo un código vigente). |
| `frontend/src/router/index.ts` | Nueva ruta para el paso de verificación de MFA durante el login; debe revisar que exista un desafío pendiente, si no, redirigir a `/login`. |
| `frontend/src/services/api.ts` | Agregar los códigos de error nuevos del §6.10 al mapa de mensajes en español. |

No hace falta agregar ninguna librería de QR al frontend — el backend ya entrega la imagen lista en base64 (ver §6.1), evitando sumar una dependencia más de npm/pnpm.

---

# PARTE 9 — Plan de pruebas

## Pruebas automáticas (backend)

1. Generación de secreto válido y verificación de código correcto/incorrecto (con tiempo simulado, no la hora real del sistema).
2. Tolerancia de ventana de tiempo (un periodo antes/después).
3. `ConfigurarMfaUseCase`: genera y guarda el secreto como pendiente.
4. `ActivarMfaUseCase`: código correcto activa, código incorrecto no activa (y no bloquea, se puede reintentar).
5. `DesactivarMfaUseCase`: con código correcto desactiva; sin código válido, rechaza.
6. `VerificarMfaUseCase`: desafío inexistente, expirado, con intentos agotados, código incorrecto, código correcto.
7. Usuario sin MFA: sigue recibiendo el JWT directo (regresión).
8. Usuario con MFA, login local: recibe el desafío, no recibe JWT.
9. Usuario con MFA, login con Google: recibe el JWT directamente y conserva su `mfaSecret`/`mfaHabilitado` para futuros accesos locales.
10. Usuario que se desactiva justo entre el primer y el segundo factor: el segundo factor debe rechazarlo igual que hoy se rechaza a un usuario inactivo.
11. `CleanArchitectureTest` debe seguir en verde — confirma que ninguna clase de la librería TOTP o de ZXing se filtró a `usecases`.

## Prueba manual de punta a punta

1. Levantar backend, MongoDB y frontend.
2. Iniciar sesión normalmente (sin MFA activo todavía).
3. Ir a la sección de seguridad y activar MFA.
4. Escanear el QR con Google Authenticator.
5. Escribir el código actual para confirmar la activación.
6. Confirmar que el estado ahora indica "MFA habilitado".
7. Cerrar sesión.
8. Iniciar sesión de nuevo (con contraseña) y confirmar que aparece la pantalla pidiendo el código, no el dashboard directo.
9. Probar un código incorrecto y confirmar el rechazo.
10. Probar el código correcto y confirmar que entrega la sesión y se puede navegar normalmente.
11. Repetir el login con Google y confirmar que entra directamente, sin solicitar el código MFA de la aplicación.
12. Intentar reutilizar un desafío ya usado y confirmar que se rechaza.
13. Probar la desactivación de MFA y confirmar que el siguiente login vuelve a ser de un solo paso.

---

# PARTE 10 — Orden recomendado de implementación

1. Backend: evolucionar `Usuario` (agregar los 2 campos + opcionalmente los métodos de copia) y actualizar los 15 sitios que dejen de compilar.
2. Backend: crear `DesafioMfa` y su persistencia Mongo (documento, repositorio, adaptador, índice TTL).
3. Backend: agregar las 3 dependencias Maven y crear `TotpPort`/`QrCodePort` + sus adaptadores, con tests.
4. Backend: agregar `ResultadoLogin` (sealed interface) en `Responses.java`.
5. Backend: crear `ConfigurarMfaUseCase`, `ActivarMfaUseCase`, `DesactivarMfaUseCase`, `ObtenerEstadoMfaUseCase` + tests.
6. Backend: crear `VerificarMfaUseCase` + tests.
7. Backend: modificar únicamente `AutenticarUsuarioUseCase` para bifurcar por `mfaHabilitado`; el login con Google conserva la emisión directa del JWT.
8. Backend: `MfaController`, endpoint `AuthController.verificarMfa`, DTOs REST, `UseCaseConfig`, `application.yml`.
9. Backend: build completo con Docker (como se hizo con el login de Google) — confirmar que toda la suite y `CleanArchitectureTest` quedan en verde.
10. Frontend: store, paso de código en `LoginView`, vista de configuración de MFA, mensajes de error nuevos.
11. Prueba manual de punta a punta (Parte 9).

---

# PARTE 11 — Criterios de aceptación

- [ ] Una cuenta sin MFA sigue entrando en un solo paso, por contraseña y por Google, sin cambios de comportamiento.
- [ ] Una cuenta con MFA no recibe un JWT completo antes de validar el código cuando utiliza usuario y contraseña.
- [ ] El login con Google entrega el JWT directamente y no solicita el TOTP de la aplicación.
- [ ] El desafío de MFA no es un JWT y jamás es aceptado como sesión por `JwtAuthenticationFilter`.
- [ ] Un código incorrecto cuenta como intento fallido y no revela información adicional.
- [ ] Superar el máximo de intentos invalida el desafío y obliga a iniciar sesión de nuevo.
- [ ] Activar/desactivar MFA exige un código TOTP vigente, no solo el JWT de sesión.
- [ ] El secreto solo se muestra una vez, en `/mfa/configurar`.
- [ ] El QR se genera enteramente en el backend; el secreto nunca viaja a un servicio externo.
- [ ] Ningún log contiene el secreto ni el código en texto plano.
- [ ] Un login con Google de un usuario con MFA activo conserva su configuración de MFA después de vincularse.
- [ ] La app Google Authenticator genera códigos que el sistema acepta correctamente.
- [ ] `docker build` (o `mvn test`) pasa completo, incluyendo `CleanArchitectureTest`.
- [ ] El build del frontend (`pnpm run build`) pasa sin errores de tipos.

---

# PARTE 12 — Entregables sugeridos para la entrega académica

- Código backend de MFA respetando Clean Architecture.
- Código frontend de activación y verificación.
- Pruebas automatizadas del backend (Parte 9).
- Captura del QR mostrado por el sistema (sin revelar el secreto en el informe final).
- Captura de Google Authenticator generando el código.
- Captura de la pantalla que solicita el segundo factor durante el login.
- Evidencia de acceso exitoso después de validar MFA.
- Evidencia de rechazo con un código incorrecto.
- Este documento (o una versión resumida) como parte del informe, explicando la decisión de diseño del §5 (por qué el desafío no es un JWT).
