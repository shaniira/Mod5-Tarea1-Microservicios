# Informe didáctico: autenticación con Google y MFA

## 1. Propósito

Este documento explica cómo funciona la autenticación del proyecto Andina Seguros. Está dirigido a personas que recién comienzan con seguridad de aplicaciones, pero también introduce los términos técnicos necesarios para comprender y mantener la solución.

El sistema ofrece dos formas de ingreso:

1. **Login social con Google:** el usuario demuestra su identidad mediante una cuenta de Google.
2. **Login local con usuario y contraseña:** si el usuario activó MFA, debe ingresar además un código de Google Authenticator.

> Decisión del proyecto: el MFA propio se solicita únicamente en el login local. El login con Google entrega la sesión directamente y no solicita el código TOTP de la aplicación.

---

## 2. Conceptos fundamentales

### 2.1 Autenticación y autorización

Aunque suelen confundirse, son procesos distintos:

- **Autenticación:** comprueba quién es el usuario. Ejemplos: contraseña, cuenta de Google o código TOTP.
- **Autorización:** determina qué acciones puede realizar el usuario autenticado según su rol.

```mermaid
flowchart LR
    A[Usuario] --> B[Autenticación]
    B --> C{Identidad válida?}
    C -- No --> D[Acceso rechazado]
    C -- Sí --> E[Autorización por rol]
    E --> F[Recursos permitidos]
```

Los roles actuales son:

| Rol          | Responsabilidad principal                     |
| ------------ | --------------------------------------------- |
| `ADMIN`    | Administración general                       |
| `AGENTE`   | Gestión de clientes, cotizaciones y pólizas |
| `ACTUARIO` | Gestión de tarifas                           |
| `CLIENTE`  | Consulta de su propia cuenta                  |

### 2.2 Factores de autenticación

Un factor es una prueba utilizada para demostrar la identidad:

- **Algo que sabes:** contraseña o PIN.
- **Algo que tienes:** teléfono con Google Authenticator.
- **Algo que eres:** huella digital o reconocimiento facial.

El login local con MFA combina dos factores:

```text
Contraseña                    Google Authenticator
(algo que sabes)       +      (algo que tienes)
       Factor 1                       Factor 2
```

### 2.3 OAuth 2.0 y OpenID Connect

**OAuth 2.0** es un estándar de autorización. **OpenID Connect (OIDC)** agrega una capa de identidad sobre OAuth 2.0.

Google Identity Services utiliza estos estándares para que la aplicación pueda recibir una identidad verificada sin conocer la contraseña de Google del usuario.

En este proyecto:

- El frontend obtiene un **ID Token** de Google.
- El backend valida ese token.
- El backend crea su propio JWT de sesión.

La aplicación no utiliza el ID Token de Google como sesión interna.

### 2.4 JWT

JWT significa **JSON Web Token**. Es un token firmado que representa la sesión del usuario.

Contiene información o *claims*, por ejemplo:

```json
{
  "sub": "admin",
  "rol": "ADMIN",
  "exp": 1780000000
}
```

- `sub`: identidad o nombre del usuario.
- `rol`: rol utilizado para autorizar operaciones.
- `exp`: momento de expiración.

El frontend envía el JWT en cada petición protegida:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 2.5 MFA, TOTP y Google Authenticator

**MFA** significa autenticación multifactor. **TOTP** significa contraseña temporal de un solo uso basada en tiempo.

Google Authenticator y el backend comparten una clave secreta. Ambos calculan el mismo código utilizando:

- La clave secreta.
- La hora actual dividida en periodos de 30 segundos.
- El algoritmo HMAC-SHA1.
- Una salida de seis dígitos.

```mermaid
flowchart LR
    S[Secreto compartido] --> G[Google Authenticator]
    S --> B[Backend]
    T[Periodo actual de 30 s] --> G
    T --> B
    G --> C1[Código 123456]
    B --> C2[Código esperado 123456]
    C1 --> V{Comparación}
    C2 --> V
    V -->|Coinciden| OK[Segundo factor válido]
```

El teléfono no necesita conexión a Internet para generar el código. Sí necesita tener su reloj correctamente sincronizado.

---

## 3. Vista general de los flujos

```mermaid
flowchart TD
    I[Pantalla de login] --> M{Método elegido}
    M -->|Google| G[Validar ID Token con Google]
    G --> J1[Emitir JWT de Andina]
    M -->|Usuario y contraseña| L[Validar credenciales locales]
    L --> Q{MFA habilitado?}
    Q -->|No| J2[Emitir JWT de Andina]
    Q -->|Sí| D[Crear desafío temporal]
    D --> T[Solicitar código TOTP]
    T --> V[Validar Google Authenticator]
    V -->|Correcto| J3[Emitir JWT de Andina]
    V -->|Incorrecto o vencido| R[Rechazar acceso]
```

Los dos métodos terminan generando el mismo tipo de JWT interno. Por ello, el resto de la aplicación no necesita saber cómo ingresó el usuario.

---

## 4. Flujo del login con Google

### 4.1 Secuencia

```mermaid
sequenceDiagram
    actor U as Usuario
    participant F as Frontend Vue
    participant G as Google Identity Services
    participant B as Backend Clean
    participant DB as MongoDB

    U->>F: Presiona Continuar con Google
    F->>G: Solicita autenticación
    G->>U: Muestra selector o acceso de Google
    U->>G: Confirma su cuenta
    G-->>F: Devuelve ID Token
    F->>B: POST /api/auth/google con idToken
    B->>B: Valida firma, issuer, audience y expiración
    B->>DB: Busca usuario por googleSubject o email
    DB-->>B: Usuario existente o resultado vacío
    B->>DB: Crea o vincula usuario cuando corresponde
    B-->>F: JWT propio de Andina Seguros
    F->>F: Guarda sesión y redirige según el rol
```

### 4.2 ¿Qué valida el backend?

El backend no debe confiar en datos personales enviados directamente por el navegador. Solo recibe el `idToken` y valida:

- **Firma:** demuestra que Google emitió el token.
- **Issuer (`iss`):** debe corresponder a Google.
- **Audience (`aud`):** debe coincidir con el Client ID de la aplicación.
- **Expiración (`exp`):** impide aceptar tokens vencidos.
- **Subject (`sub`):** identificador estable de la cuenta Google.
- **Email verificado:** evita aceptar correos que Google no confirmó.

### 4.3 ¿Por qué se usa `googleSubject`?

El correo puede cambiar. El claim `sub`, guardado como `googleSubject`, es el identificador estable asignado por Google a la cuenta para esta aplicación.

### 4.4 Resultado del login social

Si la identidad es válida y el usuario está activo, el backend devuelve:

```json
{
  "token": "jwt-de-andina-seguros",
  "tipo": "Bearer",
  "expiraEnSegundos": 28800
}
```

En este flujo no se solicita el TOTP propio del sistema.

---

## 5. Activación de Google Authenticator

Antes de exigir MFA, el usuario debe vincular su aplicación Google Authenticator.

```mermaid
sequenceDiagram
    actor U as Usuario autenticado
    participant F as Frontend Vue
    participant B as Backend Clean
    participant DB as MongoDB
    participant A as Google Authenticator

    U->>F: Abre Seguridad MFA
    F->>B: GET /api/mfa/estado
    B-->>F: habilitado = false
    U->>F: Presiona Activar MFA
    F->>B: POST /api/mfa/configurar + JWT
    B->>B: Genera secreto Base32 y URI otpauth
    B->>B: Genera imagen QR
    B->>DB: Guarda secreto con mfaHabilitado=false
    B-->>F: Secreto, URI y QR
    F-->>U: Muestra el QR
    U->>A: Escanea el QR
    A-->>U: Muestra código de 6 dígitos
    U->>F: Ingresa el código
    F->>B: POST /api/mfa/activar + JWT + código
    B->>B: Valida el TOTP
    B->>DB: Guarda mfaHabilitado=true
    B-->>F: 204 No Content
    F-->>U: MFA habilitado
```

La activación tiene dos fases por seguridad:

1. Se genera y guarda el secreto como pendiente.
2. Solo se habilita MFA cuando el usuario demuestra que escaneó correctamente el QR.

---

## 6. Login local con MFA

### 6.1 Primer factor

El usuario envía su nombre y contraseña:

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin123*"
}
```

Si MFA no está habilitado, el backend devuelve el JWT normalmente. Si está habilitado, no entrega el JWT y devuelve un desafío:

```json
{
  "requiresMfa": true,
  "token": null,
  "challengeToken": "desafio-temporal",
  "challengeExpiresIn": 300
}
```

### 6.2 Segundo factor

```mermaid
sequenceDiagram
    actor U as Usuario
    participant F as Frontend Vue
    participant B as Backend Clean
    participant DB as MongoDB

    U->>F: Envía usuario y contraseña
    F->>B: POST /api/auth/login
    B->>DB: Busca usuario
    B->>B: Comprueba contraseña y estado
    B->>B: Detecta mfaHabilitado=true
    B-->>F: challengeToken, sin JWT
    F-->>U: Solicita código de 6 dígitos
    U->>F: Ingresa código TOTP
    F->>B: POST /api/auth/mfa/verificar
    B->>B: Consume desafío y valida expiración
    B->>DB: Comprueba que el usuario siga activo
    B->>B: Verifica el código TOTP
    B-->>F: JWT definitivo
    F->>F: Guarda sesión y redirige
```

Petición del segundo factor:

```http
POST /api/auth/mfa/verificar
Content-Type: application/json

{
  "challengeToken": "desafio-temporal",
  "codigo": "123456"
}
```

El desafío:

- Tiene una duración de cinco minutos.
- Es aleatorio.
- No funciona como JWT de sesión.
- Se elimina al consumirse para impedir su reutilización.

---

## 7. Desactivación de MFA

Desactivar MFA requiere una sesión válida y un código TOTP vigente:

```http
DELETE /api/mfa
Authorization: Bearer jwt-de-sesion
Content-Type: application/json

{
  "codigo": "123456"
}
```

Solicitar nuevamente el código evita que una persona que robó solamente el JWT pueda desactivar la protección sin tener acceso al teléfono.

---

## 8. Endpoints principales

| Método    | Endpoint                    | Autenticación | Propósito                               |
| ---------- | --------------------------- | -------------- | ---------------------------------------- |
| `POST`   | `/api/auth/login`         | Público       | Login con usuario y contraseña          |
| `POST`   | `/api/auth/google`        | Público       | Login social mediante ID Token de Google |
| `POST`   | `/api/auth/mfa/verificar` | Desafío MFA   | Validar segundo factor y obtener JWT     |
| `GET`    | `/api/mfa/estado`         | JWT            | Consultar si MFA está habilitado        |
| `POST`   | `/api/mfa/configurar`     | JWT            | Generar secreto y QR                     |
| `POST`   | `/api/mfa/activar`        | JWT + TOTP     | Confirmar y activar MFA                  |
| `DELETE` | `/api/mfa`                | JWT + TOTP     | Desactivar MFA                           |

---

## 9. Aplicación de Clean Architecture

La lógica se divide para que el negocio no dependa directamente de Spring, MongoDB, Google o ZXing.

```mermaid
flowchart TB
    subgraph FD[Frameworks & Drivers]
        CFG[UseCaseConfig / SecurityConfig]
    end
    subgraph IA[Interface Adapters]
        REST[AuthController / MfaController]
        SEC[Adaptadores Google, TOTP, QR y desafío]
        MONGO[Repositorios y mappers MongoDB]
    end
    subgraph UC[Use Cases]
        AUTH[AutenticarUsuario / AutenticarConGoogle]
        MFA[Configurar / Activar / Verificar / Desactivar MFA]
        PORTS[Puertos de seguridad y repositorios]
    end
    subgraph EN[Entities]
        USER[Usuario]
    end

    FD --> IA
    IA --> UC
    UC --> EN
    SEC -. implementa .-> PORTS
    MONGO -. implementa .-> PORTS
```

### Distribución de responsabilidades

- **Entities:** `Usuario` contiene `mfaSecret` y `mfaHabilitado`, sin importar clases de frameworks.
- **Use Cases:** deciden cuándo configurar, activar, exigir y validar MFA.
- **Puertos:** definen qué necesita la aplicación: generar secretos, comprobar TOTP, crear QR o almacenar usuarios.
- **Adaptadores:** implementan los detalles técnicos de TOTP, QR, Google, JWT y MongoDB.
- **Controllers:** traducen HTTP a modelos de entrada y llaman casos de uso.
- **UseCaseConfig:** conecta interfaces con implementaciones mediante inyección de dependencias.

La dirección de dependencias apunta hacia el núcleo. Esto permite cambiar una librería TOTP o una base de datos sin reescribir las reglas de autenticación.

---

## 10. Responsabilidad del frontend Vue

El frontend no valida tokens de Google ni códigos TOTP por su cuenta. Su responsabilidad es coordinar la experiencia del usuario:

```mermaid
stateDiagram-v2
    [*] --> Login
    Login --> Sesion: login local sin MFA
    Login --> Sesion: login con Google
    Login --> EsperandoMFA: login local requiere MFA
    EsperandoMFA --> Sesion: TOTP correcto
    EsperandoMFA --> Login: cancelar o desafío vencido
    Sesion --> ConfiguracionMFA: abrir Seguridad
    ConfiguracionMFA --> Sesion: activar o desactivar
    Sesion --> Login: cerrar sesión
```

Elementos principales:

- `auth.ts`: guarda la sesión y el desafío pendiente.
- `LoginView.vue`: inicia el login local o Google.
- `MfaVerificationView.vue`: solicita el TOTP después del login local.
- `MfaSetupView.vue`: muestra el QR y permite activar o desactivar MFA.
- `router/index.ts`: impide entrar a la verificación sin un desafío pendiente.
- `api.ts`: adjunta el JWT y presenta mensajes de error comprensibles.

El `challengeToken` se guarda en `sessionStorage`; el JWT definitivo se guarda como sesión después de completar la autenticación.

---

## 11. Controles de seguridad

La implementación aplica las siguientes reglas:

- La contraseña de Google nunca llega a Andina Seguros.
- El backend verifica criptográficamente el ID Token.
- El rol no se toma del frontend.
- El JWT definitivo no se emite antes del segundo factor en el login local con MFA.
- El código TOTP debe tener exactamente seis dígitos.
- Se acepta una pequeña ventana temporal para tolerar diferencias de reloj.
- El desafío MFA expira y es de un solo uso.
- El usuario se vuelve a consultar antes de emitir el JWT.
- Los secretos, códigos TOTP y tokens no deben escribirse en logs.
- La configuración y desactivación MFA requieren una sesión autenticada.

### Consideraciones para producción

Para un entorno real se recomienda además:

- Usar HTTPS obligatoriamente.
- Cifrar `mfaSecret` antes de guardarlo en MongoDB.
- Persistir desafíos en Redis o MongoDB si existen varias instancias del backend.
- Limitar intentos y aplicar protección contra fuerza bruta.
- Incorporar códigos de recuperación.
- Utilizar cookies `HttpOnly` y `Secure` para reducir la exposición del JWT.
- Registrar eventos de seguridad sin registrar secretos.
- Sincronizar la hora del servidor mediante NTP.

La implementación académica conserva los desafíos en memoria. Si el backend se reinicia, los desafíos pendientes se invalidan, lo cual es seguro aunque obliga al usuario a iniciar el login nuevamente.

---

## 12. Errores controlados

| Código                        | Significado                               |
| ------------------------------ | ----------------------------------------- |
| `CREDENCIALES_INVALIDAS`     | Usuario o contraseña incorrectos         |
| `GOOGLE_TOKEN_INVALIDO`      | El ID Token no pudo validarse             |
| `GOOGLE_EMAIL_NO_VERIFICADO` | Google no confirmó el correo             |
| `USUARIO_INACTIVO`           | La cuenta está deshabilitada             |
| `MFA_CODIGO_INVALIDO`        | El TOTP no coincide                       |
| `MFA_DESAFIO_INVALIDO`       | El desafío no existe o ya fue consumido  |
| `MFA_DESAFIO_EXPIRADO`       | El desafío superó su duración          |
| `MFA_YA_HABILITADO`          | Se intentó configurar MFA estando activo |
| `MFA_NO_CONFIGURADO`         | No existe una configuración MFA válida  |

Los errores no deben revelar si una parte específica de las credenciales era correcta ni mostrar información secreta.

---

## 13. Guía de prueba manual

### 13.1 Activar MFA

1. Abrir `http://localhost:5173`.
2. Iniciar sesión con usuario y contraseña.
3. Entrar a **Seguridad MFA**.
4. Presionar **Activar MFA**.
5. Abrir Google Authenticator en el teléfono.
6. Escanear el QR mostrado.
7. Escribir el código actual de seis dígitos.
8. Confirmar que el estado cambie a **Habilitado**.

### 13.2 Probar login local con MFA

1. Cerrar sesión.
2. Ingresar nuevamente usuario y contraseña.
3. Confirmar que el sistema no abra todavía el panel.
4. Escribir un código incorrecto y comprobar el rechazo.
5. Iniciar nuevamente el login si el desafío fue consumido.
6. Escribir el código correcto.
7. Confirmar que se abra el panel y exista una sesión válida.

### 13.3 Probar login con Google

1. Cerrar sesión.
2. Presionar **Continuar con Google**.
3. Elegir una cuenta autorizada.
4. Confirmar que el sistema ingrese directamente sin solicitar el TOTP propio.
5. Verificar que las opciones visibles correspondan al rol del usuario.

### 13.4 Probar desactivación

1. Abrir **Seguridad MFA** con una sesión válida.
2. Introducir el código actual de Google Authenticator.
3. Presionar **Desactivar MFA**.
4. Cerrar sesión.
5. Confirmar que el próximo login local entregue la sesión sin segundo paso.

---

# PARTE II — Análisis de seguridad arquitectónica

Esta segunda parte conecta explícitamente lo implementado (Partes 1 a 13) con los fundamentos de seguridad en arquitectura de software: tríada CIA+2, principios de Saltzer & Schroeder, Zero Trust/Shift-Left, STRIDE, patrones de Authentication/Authorization, Data Encryption, OWASP Top 10 y pruebas de arquitectura. El objetivo es que este informe no solo describa *qué* se construyó, sino que demuestre *por qué* cada decisión es (o no es todavía) una decisión de seguridad correcta, con evidencia concreta del propio código: un modelado de amenazas sobre los diagramas de arquitectura ya presentados, y un mapeo explícito de cada patrón contra la categoría STRIDE que mitiga.

---

## 14. Atributos de seguridad (tríada CIA + 2) aplicados a esta funcionalidad

| Atributo                   | ¿Cómo lo protege esta implementación?                                                                                                                                                                                                                          | Evidencia                 |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------- |
| **Confidencialidad** | El`mfaSecret` y el código TOTP nunca se registran en logs; el secreto solo se devuelve una vez, en `/api/mfa/configurar`. El ID Token de Google nunca se reenvía a otro servicio.                                                                           | §5, §11                 |
| **Integridad**       | El JWT de Andina y el ID Token de Google están firmados — cualquier alteración invalida la firma y`JwtAuthenticationFilter`/`GoogleIdentityVerifierAdapter` la rechazan.                                                                                   | §4.2,`JwtTokenAdapter` |
| **Disponibilidad**   | El desafío MFA expira solo (5 min) para no acumular estado indefinidamente; el filtro JWT falla rápido (sin llamadas de red) al validar localmente la firma.                                                                                                    | §6.2                     |
| **Autenticidad**     | Google demuestra la identidad social verificando firma/`iss`/`aud`; el backend demuestra la propia emitiendo un JWT firmado con una clave que solo él conoce.                                                                                                | §4.2, §2.4              |
| **No repudio**       | *(Gap identificado, ver §21)* Hoy no existe un registro de auditoría explícito de "quién inició sesión, cuándo y por qué método" ni de intentos fallidos de MFA — solo los logs genéricos de la aplicación. Es la casilla más débil de las cinco. | —                        |

**Lectura:** cuatro de los cinco atributos ya tienen un control identificable en el diseño actual; el "No repudio" es el que requiere trabajo adicional (ver recomendación en §21).

---

## 15. Principios de Saltzer & Schroeder aplicados

| Principio                            | Cómo se aplica en este proyecto                                                                                                                                                                                                                                                                               | Evidencia en el código                                                                                                                                                       |
| ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Mínimo privilegio**         | RBAC con 4 roles (`ADMIN`, `AGENTE`, `ACTUARIO`, `CLIENTE`); cada controlador de negocio exige el rol mínimo necesario, no "cualquier usuario autenticado".                                                                                                                                           | `@PreAuthorize("hasAnyRole('ADMIN','AGENTE')")` en `ClienteController`, `CotizacionController`, `PolizaController`, `RenovacionController`, `SiniestroController` |
| **Defensa en profundidad**     | Tres capas independientes: (1) primer factor (contraseña o Google), (2) segundo factor TOTP en login local, (3) autorización por rol en cada endpoint. Cada capa falla de forma independiente.                                                                                                               | Flujo completo §3 +`@PreAuthorize`                                                                                                                                         |
| **Fail secure**                | Cualquier excepción durante la validación del JWT hace que`JwtAuthenticationFilter` limpie el contexto de seguridad y responda 401, en vez de dejar pasar la petición por defecto. Un `AccessDeniedException` se traduce a 403, nunca a "permitir".                                                     | `JwtAuthenticationFilter.doFilterInternal` (bloque `catch`), `GlobalExceptionHandler.accessDenied`                                                                      |
| **Economía de mecanismo**     | Un único`TokenGeneratorPort`/`JwtTokenAdapter` genera el JWT sin importar si el usuario entró por contraseña, Google o MFA — no hay tres mecanismos de sesión distintos, solo tres caminos que convergen en el mismo.                                                                                 | §4.4, §6.2 (ambos flujos terminan en el mismo`TokenResponse`)                                                                                                             |
| **Mediación completa**        | Cada petición protegida vuelve a pasar por`JwtAuthenticationFilter`: no hay caché de "ya lo autoricé antes" — cada decisión de autorización se reevalúa, sin depender de una caché con TTL.                                  | `JwtAuthenticationFilter` se ejecuta en cada request, sin estado de sesión del lado del servidor (`SessionCreationPolicy.STATELESS`)                                     |
| **Diseño abierto**            | Los algoritmos son todos estándar y públicos: BCrypt (contraseñas), HMAC-SHA1/TOTP RFC 6238 (segundo factor), RS256 verificado contra las claves públicas de Google (login social). Ningún algoritmo "hecho en casa"; lo único secreto son las llaves (`JWT_SECRET`, el secreto TOTP de cada usuario). | `BCryptPasswordEncoderAdapter`, `TotpAdapter`, `GoogleIdentityVerifierAdapter`                                                                                          |
| **Separación de privilegios** | Desactivar MFA exige**dos condiciones simultáneas**: sesión JWT válida + código TOTP vigente. Ninguna de las dos por sí sola alcanza.                                                                                                                                                               | §7 (`DELETE /api/mfa` requiere JWT y `codigo`)                                                                                                                           |

---

## 16. Principios contemporáneos: Zero Trust, Secure by Design, Shift-Left

| Principio                  | Evidencia en este proyecto                                                                                                                                                                                                                                                                                                                             | Brecha pendiente                                                                                                                                                                                                                                                                 |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Zero Trust**       | El backend es`STATELESS`: no confía en ninguna sesión de red ni cookie, revalida la identidad (firma del JWT) en cada petición, sin importar si viene de la misma red o de internet.                                                                                                                                                              | No hay identidad de*workload* (mTLS entre servicios) porque hoy es un monolito; sería relevante recién si se separa en microservicios.                                                                                                                   |
| **Secure by Design** | Los valores por defecto son los seguros: un usuario nuevo de Google siempre nace`CLIENTE` (nunca lo elige el cliente), `mfaHabilitado` nace en `false` y solo se activa tras confirmar un código real.                                                                                                                                          | El valor por defecto de`JWT_SECRET` en `application.yml` (`cambia-esta-clave-en-produccion...`) es *seguro por convención* pero no *seguro por diseño*: si alguien olvida sobreescribirlo en producción, el sistema arranca igual, sin advertencia. Ver §21 (A05). |
| **Shift-Left**       | `CleanArchitectureTest`/`HexagonalArchitectureTest`/`OnionArchitectureTest` (ArchUnit) corren en cada build y bloquean el merge si una regla de dependencia se rompe: la arquitectura queda codificada como prueba automática, no solo como documentación. Este proyecto ya lo hace. | No hay todavía SAST/SCA/secrets-scanning automatizados en el pipeline (ver §22).                                                                                                                                                                                               |

---

## 17. Modelado de amenazas STRIDE sobre el flujo de autenticación

Aplicando la metodología estándar de modelado de amenazas (identificar *trust boundaries* sobre el diagrama y una amenaza STRIDE por cada uno), sobre los diagramas de secuencia de §4.1 y §6.2:

**Trust boundaries identificados:**

1. Navegador (no confiable) ↔ Backend Clean.
2. Backend Clean ↔ Google (tercero externo).
3. Backend Clean ↔ MongoDB.

| STRIDE                           | Amenaza concreta en este sistema                                                                                                                              | Control que la mitiga                                                                                                                                                                                                                                                                                                | ¿Implementado?                                                                                             |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| **Spoofing**               | Alguien envía un ID Token falso o de otra aplicación para hacerse pasar por un usuario de Google.                                                           | Verificación de firma +`aud` + `iss` contra las claves públicas de Google (`GoogleIdentityVerifierAdapter`).                                                                                                                                                                                                 | ✅                                                                                                          |
| **Tampering**              | Alguien modifica el`rol` dentro de un JWT ya emitido para elevar sus privilegios.                                                                           | El JWT está firmado (HMAC); cualquier modificación invalida la firma y`JwtAuthenticationFilter` lo rechaza.                                                                                                                                                                                                      | ✅                                                                                                          |
| **Repudiation**            | Un usuario administrador niega haber realizado una acción sensible (p. ej. desactivar MFA de otra cuenta, emitir una póliza).                               | Registro de auditoría con quién, cuándo y qué acción.                                                                                                                                                                                                                                                           | ⚠️**No implementado** — es el hallazgo más importante de esta sección (ver §21, A09).           |
| **Information Disclosure** | El`mfaSecret` o el código TOTP se filtran por un log, una respuesta HTTP de error, o quedan legibles directamente en MongoDB.                              | No se loguean secretos (§11); el secreto solo se devuelve una vez.**Pendiente:** el secreto viaja y se guarda sin cifrar en Mongo.                                                                                                                                                                            | ⚠️ Parcial                                                                                                |
| **Denial of Service**      | Un atacante satura`/api/auth/mfa/verificar` probando códigos de 6 dígitos (1,000,000 combinaciones) hasta acertar antes de que expire un desafío robado. | Expiración de 5 minutos del desafío + límite de intentos.                                                                                                                                                                                                                                                         | ✅ Mitigado a nivel de un desafío;**falta** un límite adicional por cuenta/IP a través del tiempo. |
| **Elevation of Privilege** | Un usuario con rol`CLIENTE` intenta invocar `/api/clientes` o `/api/cotizaciones` para ver datos de otros clientes.                                     | `@PreAuthorize("hasAnyRole('ADMIN','AGENTE')")` + `CleanArchitectureTest`. **Este fue un hallazgo real detectado durante el desarrollo de este proyecto**: antes de la corrección, `CotizacionController` no tenía ninguna restricción y cualquier `CLIENTE` autenticado podía listar las cotizaciones de todos los clientes. | ✅ Corregido (ver`doc/USUARIOS-DE-PRUEBA.md`)                   |

Este ejercicio confirma que el diseño cubre razonablemente bien 4 de las 6 categorías STRIDE, con **Repudiation** como la brecha más clara y **Information Disclosure/Denial of Service** como brechas parciales.

---

## 18. Patrones de Authentication y Authorization: qué se usó y qué antipatrones hay que vigilar

### 18.1 Authentication

| Patrón de Authentication                    | ¿Se usa aquí? | Detalle                                                                                                    |
| ---------------------------------------- | --------------- | ---------------------------------------------------------------------------------------------------------- |
| Identity Provider externo (OIDC)         | ✅              | Google Identity Services actúa como IdP para el login social.                                             |
| Token-based (JWT)                        | ✅              | JWT propio para la sesión de la aplicación (`JwtTokenAdapter`).                                        |
| Federated Identity                       | ✅              | El login con Google es, por definición, identidad federada.                                               |
| Passwordless / Passkeys (FIDO2/WebAuthn) | ❌              | No implementado; el login local sigue siendo usuario+contraseña. Sería la evolución natural sobre TOTP. |

**Antipatrones frecuentes de autenticación, revisados uno por uno contra el código real:**

| Antipatrón                   | ¿Aplica a este proyecto?                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| --------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| "Autenticación implementada en casa"               | Parcial: el*mecanismo* de comparar contraseña/generar JWT es propio, pero usa librerías criptográficas estándar (BCrypt, JJWT) en vez de criptografía propia — es la variante aceptable de "hacerlo en casa".                                                                                                                                                                                                                                                               |
| **"JWT en localStorage"**                     | ⚠️**Sí aplica.** `frontend/src/stores/auth.ts` guarda el JWT con `localStorage.setItem('token', ...)`. Un XSS en el frontend podría robar el token. La alternativa (`HttpOnly` + `Secure` cookies) ya está anotada como recomendación de producción en §11, pero **hoy no está implementada** — se documenta aquí como una limitación consciente del alcance académico, no como un descuido. |
| "Tokens de larga vida sin revocación"              | ⚠️**Aplica parcialmente.** El JWT dura 8 horas (`expiraEnSegundos: 28800`) y no existe una lista de revocación: si un JWT se filtra, es válido hasta que expire por sí solo, sin forma de invalidarlo antes. Mitigación recomendada: acortar la vida útil y agregar refresh tokens, o una lista de revocación mínima (por `jti`) para casos críticos (ej. cierre de sesión forzado por un admin).                                                              |
| **"MFA opcional para roles administrativos"** | ⚠️**Aplica.** Hoy MFA es una opción que cualquier usuario activa voluntariamente — incluido `ADMIN`. Recomendación de mejora: hacer MFA **obligatorio** (no opcional) para los roles `ADMIN`, `AGENTE` y `ACTUARIO`, dado que son quienes pueden crear pólizas, ver datos de todos los clientes y administrar el sistema.                                                                           |

### 18.2 Authorization

Este proyecto usa **RBAC puro** (`hasAnyRole('ADMIN','AGENTE')`, etc.) para casi todo — es el más simple de los tres modelos de autorización habituales (RBAC/ABAC/ReBAC), adecuado porque los permisos de negocio se explican bien por rol (un `AGENTE` gestiona clientes/pólizas, un `ACTUARIO` gestiona tarifas).

Hay una excepción interesante: `ObtenerMiCuentaUseCase` no usa un rol para decidir qué póliza mostrar — busca el `Cliente` cuyo correo coincide con el del usuario logueado, y solo devuelve **sus propias** pólizas. Eso ya no es RBAC puro: es una regla de **propiedad del recurso** (¿es dueño de este dato el usuario que pregunta?), más cercana a ABAC. Es la buena práctica de verificar propiedad del recurso y no solo el rol, que evita vulnerabilidades de tipo IDOR/BOLA.

**PEP / PDP de este sistema:**

```text
Cliente (frontend)
     │  petición + JWT
     ▼
PEP: JwtAuthenticationFilter + @PreAuthorize en cada controller
     │  ¿permitir?
     ▼
PDP: AuthorizationManager de Spring Security (evalúa hasAnyRole(...) contra el rol del JWT)
     │  solo si permitido
     ▼
Recurso: caso de uso / repositorio Mongo
```

El PDP aquí **no está externalizado** (no hay un motor de políticas tipo OPA/Cedar separado del código) — las reglas viven como anotaciones en cada controlador. Es una decisión razonable para un monolito con 4 roles fijos; se volvería insuficiente si el número de reglas creciera mucho o si varios servicios necesitaran compartir la misma política (escenario típico de sistemas distribuidos/microservicios).

---

## 19. Data Encryption y Secure Communication: estado real

| Dato                             | ¿Cómo se protege hoy?                                                             | Observación                                                                                                                                                                                                                                  |
| -------------------------------- | ----------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Contraseña del usuario          | BCrypt (hash con sal, no reversible)                                                | ✅ Algoritmo estándar y auditado, no una función débil como MD5/SHA-1                                                                                                                                                                                          |
| `JWT_SECRET`                   | Variable de entorno, con un valor por defecto en`application.yml` si no se define | ⚠️ El valor por defecto en el repositorio es un riesgo si nunca se sobreescribe en producción — lo recomendable es gestionar este secreto en un Vault/Secrets Manager, nunca en el repositorio                                                                              |
| `mfaSecret` de cada usuario    | Texto plano en MongoDB                                                              | ⚠️ Gap documentado desde el plan original (`IMPLEMENTACION-MFA-GOOGLE-AUTHENTICATOR.md §7.6`) — correspondería a un cifrado a nivel de columna/campo: cifrar el valor antes de guardarlo, con la llave gestionada en un KMS, no junto al dato |
| Comunicación navegador↔backend | HTTP en desarrollo (`localhost`)                                                  | ⚠️ Aceptable solo en desarrollo; en producción TLS 1.3 es el mínimo aceptable en todo canal externo — ya anotado como requisito de producción en §11                                                                                                                     |
| ID Token de Google en tránsito  | HTTPS (Google exige TLS)                                                            | ✅                                                                                                                                                                                                                                                                               |

---

## 20. Mapeo con OWASP Top 10:2021 

| Categoría OWASP                                            | ¿Se encontró o se previno en este proyecto?                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| ----------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **A01 — Control de acceso roto**                     | **Se encontró y se corrigió durante el desarrollo de este proyecto**: `ClienteController`, `CotizacionController`, `PolizaController`, `RenovacionController` y `SiniestroController` no tenían ninguna restricción de rol — cualquier usuario autenticado (incluido un `CLIENTE` recién creado por Google) podía leer y crear datos de negocio de cualquier otro cliente. Es el ejemplo más concreto de todo este informe de cómo un defecto de diseño (no un bug de una línea) queda expuesto solo cuando alguien prueba el sistema de punta a punta con un rol de bajo privilegio real. |
| **A02 — Fallos criptográficos**                     | `mfaSecret` sin cifrar en reposo (ver §19) es exactamente esta categoría.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| **A04 — Diseño inseguro**                           | El diseño explícito de "el desafío MFA nunca es un JWT" (§6.2) es una decisión tomada*para prevenir* esta categoría, no un ejemplo de ella — se documenta aquí como caso positivo.                                                                                                                                                                                                                                                                                                                                                                                                                      |
| **A05 — Configuración insegura**                    | Valor por defecto de`JWT_SECRET` en el repositorio (ver §19).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| **A07 — Fallos de identificación y autenticación** | Es el tema completo de este informe. Los controles de §11 (no confiar en datos del navegador, validar criptográficamente cada token, exigir segundo factor antes del JWT) son mitigaciones directas de esta categoría.                                                                                                                                                                                                                                                                                                                                                                                         |
| **A09 — Fallos de logging y monitoreo**              | Relacionado con el hallazgo de "No repudio" de §14: no existe hoy un registro de auditoría de accesos/decisiones de autorización denegadas. Registrar cada decisión de autorización denegada es una buena práctica pendiente de incorporar.                                                                                                                                                                                                                                                                                                                                     |

---

## 21. Pruebas de arquitectura: lo que ya se hace y lo que falta

**Ya implementado:**

> "Arquitectura como código": usar ArchUnit para verificar automáticamente las reglas de dependencia entre capas, como una prueba más de la suite.

Este proyecto ya tiene `CleanArchitectureTest`, `HexagonalArchitectureTest` y `OnionArchitectureTest` corriendo en cada build (`mvn test` / `docker build`), verificando que ninguna capa interna dependa de una externa y que las nuevas piezas de Google/MFA (adaptadores TOTP, QR, Google) queden correctamente aisladas detrás de sus puertos. Es una aplicación directa del principio de *shift-left*: la arquitectura se valida automáticamente antes de que un defecto de diseño llegue a producción.

**Pendiente de incorporar** (siguiendo la pirámide habitual de pruebas de seguridad, de abajo hacia arriba):

| Técnica                                                       | Aplicación concreta recomendada para este proyecto                                                                                                                                                                                       |
| ---------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Secrets scanning (Gitleaks/TruffleHog)                           | Evitar que`JWT_SECRET`, `mfaSecret` de prueba o credenciales de `WhatsAppProperties`/`JsonPeProperties` terminen commiteados por accidente.                                                                                       |
| SCA (Trivy/Dependabot)                                           | Vigilar CVEs en las dependencias nuevas de esta funcionalidad (`dev.samstevens.totp`, ZXing, `spring-security-oauth2-jose`).                                                                                                          |
| SAST (Semgrep/SonarQube)                                         | Detectar automáticamente patrones inseguros (p. ej. loguear una variable que contenga "secret" o "token" en el nombre).                                                                                                                  |
| Matriz de autorización automatizada (rol × recurso × acción) | Convertir el hallazgo de A01 (§20) en una prueba permanente: un test que recorra todos los`@PreAuthorize` de todos los controladores y falle si alguno queda sin anotar, en vez de descubrirlo manualmente como ocurrió en este caso. |

---

## 22. Checklist del arquitecto  

| #  | Pregunta                                                           | Respuesta para Andina Seguros                                                                                                                                                                                                                                                                            |
| -- | ------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1  | ¿Dónde están los trust boundaries y qué cruza cada uno?        | Navegador↔Backend (JWT/ID Token), Backend↔Google (ID Token), Backend↔MongoDB (credenciales de conexión) — ver §17.                                                                                                                                                                                 |
| 2  | ¿Qué identidad presenta cada componente y cómo se verifica?     | El usuario presenta un JWT propio o un ID Token de Google; ambos se verifican por firma criptográfica, nunca por confianza implícita.                                                                                                                                                                  |
| 3  | ¿Quién decide la autorización y dónde se aplica?               | Spring Security (`AuthorizationManager`) decide; se aplica en cada controlador vía `@PreAuthorize`, más el filtro JWT como puerta de entrada — ver §18.2.                                                                                                                                        |
| 4  | ¿Qué datos son sensibles y en qué capa se cifran?               | Contraseñas (BCrypt, aplicación),`mfaSecret` (hoy sin cifrar, gap documentado en §19).                                                                                                                                                                                                              |
| 5  | ¿Dónde viven las llaves y los secretos, y cómo rotan?           | `JWT_SECRET` y credenciales de terceros viven en variables de entorno; no hay rotación automática ni KMS — aceptable para el alcance académico, insuficiente para producción.                                                                                                                     |
| 6  | ¿Qué pasa si falla el IdP, el KMS o el motor de políticas?      | Si Google no responde, el login social simplemente falla (`GOOGLE_TOKEN_INVALIDO`) sin afectar el login local — no hay dependencia dura de Google para el resto del sistema.                                                                                                                          |
| 7  | ¿Qué se registra para reconstruir un incidente?                  | Hoy, logs genéricos de la aplicación;**no** hay un log de auditoría dedicado a eventos de seguridad (gap de "No repudio", §14 y §20).                                                                                                                                                         |
| 8  | ¿Qué dependencias y artefactos entran, y cómo se verifican?     | Dependencias Maven declaradas en`pom.xml`; sin SCA automatizado todavía (§21).                                                                                                                                                                                                                       |
| 9  | ¿Cómo se aísla un tenant, un servicio o una carga comprometida? | No aplica multi-tenancy en este proyecto (una sola organización, Andina Seguros); la única "partición" relevante es entre roles/usuarios, resuelta por RBAC + verificación de propiedad (§18.2).                                                                                                    |
| 10 | ¿Qué prueba automatizada verifica cada respuesta anterior?       | `CleanArchitectureTest`/`HexagonalArchitectureTest`/`OnionArchitectureTest` verifican 3, 8 (parcialmente) y la separación de capas en general; el resto de respuestas todavía no tiene una prueba automatizada dedicada — es la lista de trabajo pendiente más concreta que deja este informe. |

---

## 23. Resumen final

La solución separa correctamente tres responsabilidades:

1. **Google verifica la identidad social** y entrega un ID Token.
2. **Google Authenticator genera el segundo factor TOTP** para el login local.
3. **Andina Seguros emite su propio JWT**, aplica roles y protege sus endpoints.

El punto más importante es que un ID Token, un desafío MFA y un JWT de sesión no son intercambiables. Cada elemento tiene una finalidad, duración y nivel de confianza diferente. Clean Architecture mantiene estas decisiones de negocio separadas de los detalles técnicos, haciendo que la solución sea más comprensible, comprobable y fácil de evolucionar.

El sistema ya aplica correctamente la mayoría de los atributos CIA+2, los siete principios de Saltzer & Schroeder y el *shift-left* vía ArchUnit — y **el hallazgo de control de acceso roto (A01) corregido durante este proyecto es la mejor evidencia posible de una idea central de la seguridad en arquitectura de software**: un defecto de diseño no detectado a tiempo puede convivir con un sistema que "funciona" durante mucho tiempo, hasta que alguien prueba el camino que nadie había probado. Las brechas que quedan documentadas aquí (cifrado de `mfaSecret`, JWT en `localStorage`, ausencia de auditoría, MFA opcional para administradores) no son fallas ocultas: son decisiones conscientes, explícitas y priorizadas para una siguiente iteración — que es, en el fondo, lo que significa "seguridad por diseño".
