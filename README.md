# Andina Seguros — Login con Google, Login con Facebook y MFA

Este documento explica qué se implementó para que un usuario pueda entrar al sistema con su cuenta de Google, con su cuenta de Facebook, o activando un segundo factor de seguridad (MFA) con Google Authenticator. También explica cómo levantar el proyecto en tu máquina, de dónde sacar las credenciales de Google y Facebook, y dónde va cada una. Al final hay un apartado corto que conecta este trabajo con los temas del módulo de seguridad en arquitectura de software.

Todo lo que se describe aquí vive en la arquitectura **Clean** (carpeta `Arquitectura-Clean`) y en `frontend`. Las otras dos carpetas del repositorio (`Arquitectura-Hexagonal` y `Arquitectura-Onion`) son variantes del mismo sistema hechas con otro estilo arquitectónico, pero no tienen login social ni MFA.

## Índice

1. [Login con Google](#1-login-con-google)
2. [Login con Facebook](#2-login-con-facebook)
3. [MFA — verificación en dos pasos](#3-mfa--verificación-en-dos-pasos)
4. [Cómo levantar el proyecto](#4-cómo-levantar-el-proyecto)
5. [Cómo conseguir las claves de Google y Facebook](#5-cómo-conseguir-las-claves-de-google-y-facebook)
6. [Relación con el módulo de Seguridad en Arquitectura de Software](#6-relación-con-el-módulo-de-seguridad-en-arquitectura-de-software)

---

## 1. Login con Google

La idea es simple: en vez de crear un usuario y contraseña, la persona entra con su cuenta de Google. Google se encarga de comprobar quién es, y a nuestro backend le llega solamente el resultado ya firmado.

**Cómo funciona el flujo:**

1. En la pantalla de login se carga el botón oficial de Google (`google.accounts.id`). Ese botón vive dentro de un espacio que controla Google, nosotros no vemos ni tocamos la contraseña de nadie.
2. Cuando la persona elige su cuenta, Google le entrega al navegador un token firmado (`idToken`) con su nombre, correo, y si ese correo está verificado.
3. El frontend manda ese token al backend: `POST /api/auth/google`.
4. El backend valida la firma del token contra las llaves públicas de Google (no hace falta ningún secreto de por medio, por eso este flujo no necesita un "Client Secret").
5. Si el correo no está verificado, se rechaza.
6. Si la persona ya tiene cuenta (por su Google Subject, un ID que nunca cambia), entra directo. Si no la tiene, se revisa si su correo corresponde a un cliente ya registrado de Andina Seguros; si es así, se le crea una cuenta nueva; si no, se le pide que contacte a un agente.
7. Se genera el JWT (el "gafete" de sesión) y el frontend lo guarda.

**Qué se agregó/corrigió en esta implementación:**

- Ahora, además del correo, se captura el **nombre y apellido** que entrega Google (`given_name` / `family_name`) y se guardan en la cuenta del usuario.
- Se corrigió un bug real: cuando una persona vinculaba Google a una cuenta que ya tenía Facebook conectado, el código antiguo **borraba silenciosamente** el vínculo de Facebook (usaba un constructor que reseteaba esos campos sin querer). Ahora se preserva todo lo que ya tenía la cuenta.
- El nombre también se mantiene actualizado si la persona vuelve a iniciar sesión y su nombre en Google cambió.

## 2. Login con Facebook

Este es más largo porque, a diferencia de Google, Facebook usa el flujo clásico de OAuth2 con redirecciones de página completa.

**Cómo funciona el flujo:**

1. La persona hace clic en "Continuar con Facebook". El navegador va directo a `GET /api/auth/facebook`.
2. El backend genera un código aleatorio de un solo uso (`state`, protección contra CSRF) y redirige a Facebook con los datos de la app.
3. Facebook muestra su pantalla de login y de permisos. Si la persona acepta, Facebook redirige de vuelta a `GET /api/auth/facebook/callback` con un `code`.
4. El backend, **desde el servidor** (nunca desde el navegador), cambia ese `code` por un access token, y con ese token le pide a Facebook el perfil (`id`, `nombre`, `apellido`, `correo`) y los permisos concedidos.
5. Si el usuario ya existe (por su ID de Facebook), se actualiza su token; si no existe, se crea una cuenta nueva.
6. En vez de mandar el JWT directo en la URL de la redirección (lo cual quedaría expuesto en el historial del navegador), el backend genera un **ticket temporal de un solo uso** y redirige al frontend con ese ticket. El frontend lo canjea aparte, con una llamada que no queda en el historial, y recién ahí recibe el JWT real.

**Qué se corrigió durante las pruebas en vivo (esto es lo más importante de esta sección):**

Al conectar la app real de Facebook nos encontramos con tres errores concretos, uno detrás del otro, y los tres quedaron resueltos:

1. **`redirect_uri isn't an absolute URI`**: el código armaba las URLs hacia Facebook a mano, codificando los parámetros con `URLEncoder` y pasándolos como texto plano al cliente HTTP. Spring volvía a interpretar y codificar ese texto, dañando la URL de retorno que ya venía codificada. Se corrigió construyendo esas URLs con `UriComponentsBuilder`, que codifica una sola vez.
2. **`UnknownContentTypeException`**: el Graph API de Facebook responde algunos endpoints con `Content-Type: text/javascript` en vez de `application/json` (es un resabio histórico de JSONP). El cliente HTTP no tenía ese tipo de contenido registrado para el conversor JSON, así que rechazaba una respuesta que en realidad sí era JSON válido. Se corrigió registrando `text/javascript` como tipo aceptado.
3. **El correo no se guardaba en logins repetidos**: cuando un usuario ya existía y volvía a loguearse (por ejemplo, después de habilitar el permiso de correo en Facebook), el código solo actualizaba el token de acceso, nunca el correo ni el nombre. Se corrigió para que ambos se sincronicen en cada login.

Además, se agregó la captura de **nombre y apellido** (`first_name` / `last_name`) igual que con Google, y el botón de "Continuar con Facebook" se rediseñó visualmente (fondo blanco, forma de píldora, ícono a la izquierda, en línea con el botón de Google), en vez del botón azul plano que había antes.

**Importante sobre el panel de Meta for Developers:** para que Facebook conceda el permiso de correo (`email`), no alcanza con tenerlo en el código. Hay que:
- Agregar tu cuenta como Administrador/Desarrollador/Tester de la app en **App roles**, mientras la app esté en modo desarrollo.
- Agregar explícitamente el permiso `email` en **App Review → Permissions and Features** (con el botón "Agregar"), aunque seas administrador de la app.

Sin ese permiso agregado, Facebook devuelve un error de "Invalid Scopes" antes de que el código llegue a ejecutarse.

## 3. MFA — verificación en dos pasos

MFA (Multi-Factor Authentication) agrega un segundo paso al login con usuario y contraseña: después de escribir la contraseña, hay que ingresar un código de 6 dígitos que genera una app como Google Authenticator, y que cambia cada 30 segundos.

**Cómo funciona activarlo** (desde la sección "Seguridad" del sistema, ya logueado):

1. Se pide un secreto nuevo (`POST /mfa/configurar`), que se muestra como un código QR para escanear con la app y también como texto por si se quiere ingresar a mano.
2. Se escanea el QR y se confirma con el primer código que la app genera (`POST /mfa/activar`). Recién ahí queda habilitado de verdad.

**Cómo funciona en el login:**

1. Se ingresa usuario y contraseña como siempre.
2. Si la cuenta tiene MFA activado, el backend **no entrega el JWT todavía**: solo confirma que la contraseña era correcta y devuelve un "desafío" temporal (`challengeToken`), válido por 5 minutos.
3. El usuario escribe el código de 6 dígitos de su app, se manda junto con el `challengeToken` (`POST /auth/mfa/verificar`), y recién ahí se entrega el JWT real.

El algoritmo detrás (TOTP) es un estándar abierto: el servidor y el teléfono comparten un secreto generado una sola vez, y ambos calculan el mismo código combinando ese secreto con la hora actual redondeada a bloques de 30 segundos — por eso funciona sin que el teléfono necesite conexión a internet.

**Punto importante a tener en cuenta:** hoy el MFA solo se aplica al login con usuario y contraseña. Si una cuenta tiene MFA activado pero también tiene Google o Facebook vinculado, entrar por esas vías **no pide el segundo factor**. Es una limitación conocida del diseño actual, documentada en detalle en `doc/Analisis-Login-Social-MFA/MFA.md`.

---

## 4. Cómo levantar el proyecto

Todo corre en Docker: backend (Spring Boot), base de datos (MongoDB) y frontend (Vue, servido por nginx desde un build ya compilado).

### Backend + base de datos

```bash
cd Arquitectura-Clean
docker compose up -d --build
```

Esto levanta:
- La API en `http://localhost:8083` (Swagger en `http://localhost:8083/swagger-ui.html`).
- MongoDB en el puerto `27020`.

### Frontend

```bash
cd frontend
docker compose up -d --build
```

Esto sirve la aplicación en `http://localhost:5173`.

> Si ya tenías el frontend corriendo y cambiaste algo del código, tenés que volver a correr `docker compose up -d --build frontend` para que el cambio se refleje. El frontend no usa un servidor de desarrollo con recarga automática, sirve un build ya compilado.

### Ver logs / apagar

```bash
docker compose logs -f backend      # dentro de Arquitectura-Clean
docker compose down                 # apaga sin borrar los datos de Mongo
```

Si querés levantar las tres arquitecturas del repositorio juntas (Onion + Hexagonal + Clean) para comparar, mirá `DOCKER-EJECUCION.md` en la raíz — pero para probar login social y MFA, usá siempre `Arquitectura-Clean`, porque es la única que lo tiene implementado.

---

## 5. Cómo conseguir las claves de Google y Facebook

Hacen falta credenciales propias de cada proveedor. Nunca se suben al repositorio: van en archivos `.env` que están ignorados por git.

### Google

1. Entrar a [console.cloud.google.com](https://console.cloud.google.com) y crear (o elegir) un proyecto.
2. Ir a **APIs y servicios → Credenciales → Crear credenciales → ID de cliente de OAuth**.
3. Tipo de aplicación: **Aplicación web**.
4. En "Orígenes de JavaScript autorizados" agregar `http://localhost:5173`.
5. Guardar y copiar el **Client ID** (no hace falta el "Client Secret" para este flujo).

Ese mismo Client ID va en **dos** archivos, y tiene que ser idéntico en ambos:

| Archivo | Variable |
|---|---|
| `Arquitectura-Clean/.env` | `GOOGLE_CLIENT_ID` |
| `frontend/.env` | `VITE_GOOGLE_CLIENT_ID` |

Si no coinciden, todo intento de login con Google falla.

### Facebook

1. Entrar a [developers.facebook.com](https://developers.facebook.com/apps) y crear una app nueva (tipo "Consumidor" / "Autenticar usuarios con Facebook Login").
2. Agregar el producto **Facebook Login** desde el panel de productos.
3. En **Configuración → Básica**, copiar el **ID de la app** y el **Secreto de la app**.
4. En **Facebook Login → Configuración**, agregar en "URI de redirección de OAuth válidos" exactamente:
   ```
   http://localhost:8083/api/auth/facebook/callback
   ```
5. En **Funciones de la aplicación → Roles**, agregarte a vos mismo (y a quien vaya a probar la app) como Administrador, Desarrollador o Tester. Sin esto, Facebook bloquea el login con un aviso de "revisión pendiente".
6. En **Revisión de la app → Permisos y funciones**, buscar `email` y darle **Agregar** (si solo se pide `public_profile`, este paso no es necesario, pero sin él nunca vas a tener el correo del usuario).

Con eso, completar en `Arquitectura-Clean/.env`:

```
FACEBOOK_APP_ID=<el App ID>
FACEBOOK_APP_SECRET=<el App Secret>
FACEBOOK_REDIRECT_URI=http://localhost:8083/api/auth/facebook/callback
FACEBOOK_SCOPES=public_profile,email
FACEBOOK_FRONTEND_CALLBACK_URL=http://localhost:5173/login
FACEBOOK_TOKEN_ENCRYPTION_KEY=<ver más abajo>
```

`FACEBOOK_TOKEN_ENCRYPTION_KEY` no es ninguna clave de Facebook: es una clave propia de la aplicación, usada para cifrar el access token de Facebook antes de guardarlo en la base de datos. Se genera una sola vez, con este comando:

```bash
openssl rand -base64 32
```

Hay un archivo `Arquitectura-Clean/.env.example` con la plantilla completa de todas las variables, y `frontend/.env.example` con las del frontend — conviene copiarlos como `.env` y completar los valores en vez de escribirlos desde cero.

Después de cambiar cualquier valor de `.env`, hay que reconstruir el contenedor correspondiente (`docker compose up -d --build`) para que tome el cambio.

---

## 6. Relación con el módulo de Seguridad en Arquitectura de Software

Repasando el material del módulo 4 (Jaime Farfán), varios de los principios que ahí se explican en teoría son exactamente lo que se puso en práctica — o lo que quedó pendiente — en este proyecto:

- **Authentication and Authorization como patrones separados**: el material distingue "¿quién eres?" de "¿qué puedes hacer?". Acá se ve clarísimo: Google y Facebook resuelven la autenticación (identidad verificada por un tercero), y el rol (`CLIENTE`, `ADMIN`, etc.) guardado en el JWT resuelve la autorización.
- **Defensa en profundidad**: el login de Facebook no confía en una sola verificación. Valida el `state` contra CSRF, valida que el `code` no esté vacío, valida que Facebook haya concedido permisos, y cifra el token antes de guardarlo — varias capas independientes, tal como plantea el principio.
- **Fail secure**: si falta la configuración de Facebook (`FACEBOOK_APP_ID`, etc.), el sistema rechaza la operación en vez de intentar continuar con datos incompletos.
- **Gestión de secretos**: el `App Secret` de Facebook nunca llega al navegador, solo vive en el backend. El access token de cada usuario se guarda cifrado (AES-256), no en texto plano — el mismo principio de "cifrado en reposo por capas" que se ve en el material.
- **MFA como control de autenticación reforzado**: el material menciona explícitamente "MFA opcional para roles administrativos" como anti-patrón. Es prácticamente el hallazgo que documentamos: en este sistema el MFA queda opcional de hecho para cualquier cuenta con login social vinculado, porque ese camino no lo exige. Es un ejemplo real, dentro de nuestro propio proyecto, del mismo anti-patrón que la diapositiva 16 advierte evitar.
- **Modelado de amenazas (STRIDE)**: pensar el login de Facebook en esos términos ayuda a explicar por qué existen ciertos controles — el `state` existe para evitar *Spoofing* (que alguien complete el login en nombre de otra persona), y el ticket de un solo uso para el intercambio final existe para reducir *Information Disclosure* (evitar que el JWT quede expuesto en una URL).

En resumen: no hicimos un ejercicio académico aparte, sino que al construir el login social y el MFA terminamos aplicando (y en algún caso, encontrando en nuestro propio código) varios de los principios y anti-patrones que plantea el módulo de seguridad.

---

## Integrantes

- Shanira Ramirez
- Erick Soto
