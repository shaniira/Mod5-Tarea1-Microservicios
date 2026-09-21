# Andina Seguros — Análisis de la arquitectura actual: riesgos, vulnerabilidades y desventajas

Este documento describe el diagrama [DIAGRAMA-ARQUITECTURA-ACTUAL.png](a_DIAGRAMA-ARQUITECTURA-ACTUAL.png) y evalúa sus riesgos. Se basa en el código y en los `docker-compose.yml`. Cada hallazgo indica dónde se comprobó. Lo que **no** se pudo comprobar se dice expresamente. El detalle técnico de la arquitectura está en [ARQUITECTURA-ACTUAL.md](a_ARQUITECTURA-ACTUAL.md).

Escala de severidad: **Crítica** (explotable ya y con impacto total), **Alta**, **Media**, **Baja**.

---

## 1. Qué muestra el diagrama

### 1.1 Componentes

| Zona del diagrama | Componente | Rol |
|---|---|---|
| Fuera de Docker | Usuarios (ADMIN, AGENTE, ACTUARIO, CLIENTE) y Cliente Web (navegador) | Ejecutan la SPA Vue y llaman directamente al backend |
| Fuera de Docker | Google Authenticator | App del usuario que genera el código TOTP (no es un servicio de red) |
| Red por defecto | Frontend (Nginx) | Solo sirve archivos estáticos en `:5173`; no habla con el backend |
| `clean_network` + `rabbitmq_network` | Backend (Spring Boot, Clean Architecture) | API REST en `:8083`; toda la lógica de negocio y la seguridad |
| `clean_network` | MongoDB | Base única `andina_seguros_clean` (9 colecciones) |
| `rabbitmq_network` | RabbitMQ | Exchange `andina.insurance.events`, colas de notificación y auditoría, DLX y DLQ |
| `clean_network` + `rabbitmq_network` | Notification Consumer | Worker sin HTTP que envía WhatsApp cuando se emite una póliza |
| Internet | Google Identity, Facebook OAuth/Graph, JSON.pe (placas y WhatsApp) | Servicios de terceros |

### 1.2 Flujos que dibuja

1. **Navegación:** el navegador pide la SPA a Nginx (HTTP `:5173`).
2. **API:** el código Vue llama al backend (HTTP `:8083/api`, `Authorization: Bearer <JWT>`).
3. **Login social:** el navegador carga el botón de Google y redirige a Facebook. El backend verifica el ID token de Google y canjea el código de Facebook.
4. **Datos:** el backend lee y escribe en MongoDB.
5. **Evento asíncrono:** al emitir una póliza, el backend publica `policy.issued.v1` en RabbitMQ. El evento llega a `notification.queue` y a `audit.queue`.
6. **Notificación:** el Consumer lee la cola, consulta el teléfono en MongoDB, llama a JSON.pe y envía el WhatsApp.
7. **Fallos:** tras 4 intentos, el mensaje va al DLX y a la DLQ.

### 1.3 Estilo arquitectónico

Es un **monolito en capas (Clean Architecture)** más un **worker asíncrono orientado a eventos**. Son 3 aplicaciones desplegables y 2 infraestructuras (MongoDB y RabbitMQ). No es una arquitectura de microservicios: el backend concentra todos los dominios (clientes, cotizaciones, pólizas, siniestros, renovaciones, tarifas y autenticación).

---

## 2. Resumen ejecutivo

| # | Hallazgo | Severidad |
|---|---|---|
| S1 | Cualquiera puede registrarse como ADMIN | **Crítica** |
| S2 | Casi ningún endpoint valida el rol ni la propiedad del recurso | **Crítica** |
| S3 | Secretos y tokens escritos en el código y en el compose | **Alta** |
| S4 | MongoDB sin autenticación y con puerto publicado | **Alta** |
| S5 | RabbitMQ con credenciales por defecto y consola publicada | **Alta** |
| S6 | Todo el tráfico va en HTTP plano | **Alta** |
| S7 | Cuenta ADMIN demo con contraseña conocida que se recrea al arrancar | **Alta** |
| S8 | Sesión: JWT en `localStorage`, `logout` sin efecto, sin revocación | **Media** |
| S9 | Sin límite de intentos en el login | **Media** |
| S10 | Swagger UI público | **Media** |
| S11 | Nginx sin cabeceras de seguridad | **Media** |
| A1 | Póliza y evento se guardan en pasos separados (sin Outbox) | **Alta** |
| A2 | Base de datos compartida entre backend y consumer | **Media** |
| A3 | Estado en memoria del backend: no escala ni sobrevive a reinicios | **Media** |
| A4 | `audit.queue` sin consumidor | **Media** |
| A5 | Punto único de fallo en todos los componentes | **Media** |
| A6 | Dependencia de terceros sin plan de contingencia | **Media** |

---

## 3. Vulnerabilidades de seguridad

### S1. Registro público con elección de rol — Crítica

- **Evidencia:** `SecurityConfig` deja `/api/auth/**` sin autenticación ([SecurityConfig.java](../../Arquitectura-Clean/src/main/java/com/andinaseguros/frameworksdrivers/configuration/spring/SecurityConfig.java)). `POST /api/auth/register` no tiene ninguna restricción adicional y su cuerpo (`CrearUsuarioRequest`) incluye el campo `rol` de tipo `RolUsuario`. `RegistrarUsuarioUseCase` guarda el usuario con ese rol, sin verificar quién lo pide.
- **Cómo se explota:** `POST /api/auth/register` con `{"username":"x","password":"y","rol":"ADMIN"}` y luego `POST /api/auth/login`. No se necesita ninguna credencial previa.
- **Consecuencias:** toma total del sistema. El atacante ve, crea y modifica clientes, pólizas, siniestros, renovaciones y tablas tarifarias. Puede además crear cuentas persistentes para volver a entrar.
- **Qué hacer:** que el registro no acepte `rol`, o que solo lo permita un ADMIN autenticado. Un usuario que se autoregistra debe quedar con un rol mínimo por defecto.

### S2. Autorización rota: falta control por rol y por propiedad — Crítica

- **Evidencia:** en todos los controladores solo hay `@PreAuthorize` en 3 endpoints de negocio: `POST /api/clientes`, `GET /api/clientes` y `POST /api/tablas-tarifarias`, más los de sesión en `/api/auth`. Todo lo demás (cotizaciones, pólizas, siniestros, renovaciones, `GET /api/clientes/{id}`, vehículos, tablas tarifarias en lectura, consulta de placas) solo exige estar autenticado. `ListarPolizasUseCase.execute()` devuelve todas las pólizas sin filtrar por usuario. No encontré verificación de que el recurso pertenezca al CLIENTE que lo pide.
- **Consecuencias:** un usuario con rol CLIENTE, aunque el frontend no le muestre esas pantallas, puede llamar a la API directamente y:
  - listar las pólizas y cotizaciones de todos los clientes (fuga de datos personales);
  - aprobar o rechazar renovaciones y emitir pólizas;
  - registrar o cambiar el estado de siniestros ajenos.
  Las restricciones de rol del frontend (`router.beforeEach`) son solo de interfaz; no protegen la API.
- **Qué hacer:** reglas de rol en cada endpoint y control de propiedad (el CLIENTE solo accede a sus propios recursos) en los casos de uso.

### S3. Secretos escritos en el repositorio — Alta

- **Evidencia:**
  - `application.yml` del backend contiene el token de JSON.pe para placas y el de WhatsApp en texto plano, y un `JWT_SECRET` por defecto.
  - `docker-compose.yml` fija `JWT_SECRET: cambia-esta-clave-en-produccion-debe-superar-32-caracteres` y la contraseña de RabbitMQ por defecto `andina-local`.
  - El backend declara `app.whatsapp.token` aunque no lo usa.
- **Consecuencias:** cualquiera con acceso al repositorio puede consumir las APIs de terceros (con costo o abuso a nombre de la empresa) y, sobre todo, **firmar JWT válidos** si el secreto por defecto llega a producción, lo que equivale a suplantar a cualquier usuario, incluido un ADMIN.
- **Qué hacer:** mover todo a variables de entorno o a un gestor de secretos, **rotar los tokens que ya quedaron expuestos**, y hacer que la aplicación falle al arrancar si `JWT_SECRET` no está definido. Los archivos `.env` ya están en `.gitignore`; conviene comprobar que ninguno se haya subido antes.

### S4. MongoDB sin autenticación y expuesto — Alta

- **Evidencia:** el compose no define usuario ni contraseña de MongoDB (`MONGO_INITDB_ROOT_USERNAME` no aparece) y publica `27020:27017`. El backend y el consumer se conectan con `mongodb://mongodb:27017/...` sin credenciales.
- **Consecuencias:** cualquiera que alcance el puerto del host (red local, VPN, máquina compartida) puede leer o borrar toda la base: usuarios con hashes, clientes con teléfonos y documentos, pólizas. Como también contiene los tokens de Facebook cifrados, se pierde además esa capa si se roba la clave.
- **Qué hacer:** activar autenticación, no publicar el puerto en el host (dejarlo solo en la red interna) y usar un usuario de aplicación con permisos mínimos.

### S5. RabbitMQ con credenciales por defecto y consola publicada — Alta

- **Evidencia:** usuario `andina` / contraseña `andina-local` por defecto; puertos `5672` y `15672` (consola de gestión) publicados en el host.
- **Consecuencias:** un atacante puede publicar eventos falsos en `policy.issued.v1` (el consumer enviaría WhatsApp a números arbitrarios de clientes existentes), leer eventos o vaciar colas. El consumer confía en el contenido del evento y solo valida tipo y versión.
- **Qué hacer:** contraseña fuerte por entorno, cerrar los puertos al exterior y, en producción, TLS y permisos por usuario (el backend solo publica, el consumer solo consume).

### S6. Tráfico sin cifrar — Alta

- **Evidencia:** navegador → Nginx en HTTP `:5173`; navegador → backend en `http://localhost:8083`; backend ↔ MongoDB y RabbitMQ sin TLS. Nginx no tiene bloque HTTPS ([frontend/nginx.conf](../../frontend/nginx.conf)).
- **Consecuencias:** en un despliegue fuera de `localhost`, el JWT, las contraseñas y los códigos MFA viajan en claro y se pueden capturar o modificar en la red.
- **Qué hacer:** terminar TLS en un proxy inverso delante de Nginx y del backend, y cifrar las conexiones internas si los servicios dejan de estar en la misma máquina.

### S7. Cuenta ADMIN demo con contraseña conocida — Alta

- **Evidencia:** `MongoDemoDataInitializer` crea el usuario `admin` con la contraseña `Admin123*` cada vez que arranca sin encontrarlo. La pantalla de login además sugiere esa contraseña ("Usuario inicial sugerido: admin / Admin123*").
- **Consecuencias:** si el sistema se despliega tal cual, cualquiera entra como administrador. Se agrava con S1 (se puede crear un ADMIN sin necesidad de esa cuenta).
- **Qué hacer:** sembrar datos demo solo con un perfil de desarrollo, exigir cambio de contraseña y quitar la pista del login.

### S8. Sesión débil — Media

- **Evidencia:** el frontend guarda el JWT en `localStorage` ([stores/auth.ts](../../frontend/src/stores/auth.ts), [services/api.ts](../../frontend/src/services/api.ts)). `POST /api/auth/logout` devuelve `204` sin hacer nada. El JWT vive 8 horas (`28800` s) y no hay lista de revocación.
- **Consecuencias:** un XSS en la SPA permite robar el token, y quien lo robe lo puede usar hasta que caduque aunque el usuario cierre sesión. Si se desactiva a un usuario, su token sigue funcionando hasta expirar.
- **Qué hacer:** tokens de acceso más cortos con renovación, revocación (lista negra o versión del token en el usuario) y, si es posible, cookie `HttpOnly` en lugar de `localStorage`.

### S9. Sin límite de intentos — Media

- **Evidencia:** no encontré en el código ni en el `pom.xml` ningún mecanismo de limitación de tasa, bloqueo de cuenta o CAPTCHA. Aplica a `/login`, a la verificación del código MFA y al registro.
- **Consecuencias:** fuerza bruta de contraseñas y de códigos TOTP de 6 dígitos (aunque el desafío MFA expira en 300 s, se pueden abrir desafíos nuevos), y abuso de la consulta de placas (JSON.pe con cuota de pago).
- **Qué hacer:** limitar intentos por IP y por usuario, y bloquear temporalmente tras varios fallos.

### S10. Swagger UI y documentación pública — Media

- **Evidencia:** `/swagger-ui.html` y `/v3/api-docs/**` están en la lista pública de `SecurityConfig`.
- **Consecuencias:** cualquiera obtiene el mapa completo de la API, lo que facilita explotar S1 y S2.
- **Qué hacer:** deshabilitar o proteger en producción.

### S11. Cabeceras de seguridad ausentes — Media

- **Evidencia:** `nginx.conf` es una sola regla `try_files` y no añade CSP, `X-Frame-Options`, `X-Content-Type-Options` ni `Referrer-Policy`.
- **Consecuencias:** mayor exposición a clickjacking y a XSS; junto con S8, el impacto de un XSS es el robo de sesión.

### Otros puntos de seguridad menores

- **CORS con credenciales:** `allowCredentials(true)` con la lista de orígenes configurable. Es correcto con orígenes explícitos, pero un valor amplio en `CORS_ALLOWED_ORIGINS` abriría la API a otros sitios.
- **Datos personales:** el consumer registra en log el teléfono enmascarado (bien), pero el paquete de eventos del backend está en nivel `DEBUG`, lo que puede volcar contenido de mensajes a los logs.
- **Contenedores:** los Dockerfiles no definen un usuario no root. No lo verifiqué ejecutando la imagen.

---

## 4. Riesgos de arquitectura y de fiabilidad

### A1. Sin patrón Outbox: la póliza y el evento no son atómicos — Alta

- **Evidencia:** `EmitirPolizaUseCase` guarda la póliza en Mongo y después llama a `DomainEventPublisherPort.publicar(...)`. Son dos operaciones independientes; RabbitMQ solo registra un aviso en el log si rechaza la publicación.
- **Consecuencias:** si RabbitMQ está caído o el proceso muere entre ambos pasos, **la póliza existe y el cliente nunca recibe el WhatsApp**, sin ningún reintento ni rastro recuperable. En el caso contrario (evento publicado, error después), se notificaría una póliza que no quedó guardada.
- **Qué hacer:** patrón Outbox (guardar el evento en Mongo junto con la póliza y publicarlo con un proceso aparte) o, como mínimo, reintento y alerta ante fallo de publicación.

### A2. Base de datos compartida — Media

- **Evidencia:** el consumer lee directamente la colección `clientes` y escribe `processed_notification_events` en la misma base que el backend.
- **Consecuencias:** acoplamiento por datos. Si el backend cambia el esquema de `clientes` (por ejemplo, renombra `telefono`), el consumer se rompe sin aviso. Además, un fallo o bloqueo en la base afecta a ambos, y el consumer tiene acceso a colecciones que no necesita.
- **Qué hacer:** que el evento lleve el dato necesario (o una consulta a una API del backend), o al menos limitar al consumer a una vista de solo lectura.

### A3. Estado en memoria del backend — Media

- **Evidencia:** `InMemoryOAuthStateAdapter` (300 s), `InMemoryLoginTicketAdapter` (60 s) e `InMemoryMfaChallengeAdapter` (300 s).
- **Consecuencias:** con **más de una réplica** del backend, el login con Facebook y el MFA fallan de forma intermitente, porque el estado está en otra instancia. Un reinicio también corta los flujos en curso. Esto impide escalar horizontalmente.
- **Qué hacer:** llevar ese estado a un almacén compartido con caducidad (por ejemplo Redis o una colección Mongo con TTL).

### A4. Cola de auditoría sin consumidor — Media

- **Evidencia:** `andina.policy.audit.queue` está declarada y enlazada, y solo existe un `@RabbitListener` en todo el proyecto (el de notificaciones).
- **Consecuencias:** la cola acumula un mensaje por cada póliza emitida sin límite ni caducidad. Con el tiempo consume memoria y disco de RabbitMQ y puede causar bloqueos del broker. Además da una falsa sensación de que hay auditoría cuando no la hay.
- **Qué hacer:** implementar el consumidor de auditoría o eliminar la cola; si se conserva, fijarle TTL y tamaño máximo.

### A5. Puntos únicos de fallo — Media

- **Evidencia:** una sola instancia de cada componente, sin réplicas, sin `replicaSet` de MongoDB y sin clúster de RabbitMQ.
- **Consecuencias:** la caída de MongoDB deja inutilizable todo el sistema. La caída de RabbitMQ no bloquea la emisión de pólizas, pero deja de notificar sin aviso (ver A1). Los volúmenes locales no tienen respaldo definido.
- **Qué hacer:** respaldos de MongoDB, alertas y, si el negocio lo exige, replicación.

### A6. Dependencia de terceros — Media

- **Evidencia:** el backend depende de JSON.pe para consultar placas (timeout 5 s, sin reintento ni caché en el cliente), de Google y de Facebook para el login social, y el consumer depende de JSON.pe WhatsApp.
- **Consecuencias:**
  - Si JSON.pe falla, no se puede consultar placas (el backend traduce el error, pero la funcionalidad queda no disponible).
  - Si Google o Facebook caen, esos accesos fallan, aunque el login con contraseña sigue funcionando.
  - Una caída prolongada de WhatsApp lleva los mensajes a la DLQ. **No hay reproceso automático desde la DLQ ni alerta.**
- **Qué hacer:** monitorizar la DLQ, definir un procedimiento de reproceso y valorar un canal alternativo (correo o SMS).

---

## 5. Desventajas del diseño actual

| Aspecto | Desventaja |
|---|---|
| **Monolito en capas** | Todo el dominio se despliega junto: un cambio pequeño obliga a reconstruir y reiniciar todo el backend, y no se puede escalar por separado la parte más cargada (por ejemplo, cotizaciones). |
| **Un solo módulo Maven** | Las capas se separan por paquetes, no por módulos. Solo la prueba ArchUnit impide que se rompa la regla de dependencia; un módulo por capa la haría cumplir en la compilación. |
| **Base compartida** | Ver A2. |
| **Sin observabilidad** | No hay métricas, trazas ni alertas. El estado del consumer solo se ve en los logs; no tiene endpoint de salud. |
| **Configuración dispersa** | Hay un compose en la raíz (solo Mongo y backend) y otro en `Arquitectura-Clean` (el stack completo). Es fácil levantar una versión incompleta sin darse cuenta. |
| **Restos de otras arquitecturas** | La URI por defecto del consumer apunta a `andina_seguros_hexagonal` (puerto 27019) y su README aún menciona `hexagonal-mongodb`. Fuera de Docker se conectaría a una base equivocada. |
| **Consumidor duplica la topología** | Backend y consumer declaran las mismas colas y exchanges. Si una definición cambia en un lado y no en el otro, RabbitMQ rechaza el arranque por propiedades incompatibles. |
| **Sin ambientes definidos** | No hay perfiles claros de desarrollo y producción; los datos demo y los secretos por defecto se comportan igual en ambos. |
| **Pruebas de integración** | Hay pruebas unitarias y de arquitectura. No encontré pruebas automáticas del flujo completo backend → RabbitMQ → consumer → WhatsApp. |

---

## 6. Escenarios de consecuencia

| Escenario | Qué ocurre | Riesgos implicados |
|---|---|---|
| Un atacante llama a `/api/auth/register` con rol ADMIN | Control total del sistema y de los datos de clientes | S1, S2, S10 |
| Un cliente legítimo explora la API con su token | Lee pólizas y datos personales de otros clientes; puede modificar renovaciones y siniestros | S2 |
| El repositorio se publica o se comparte | Se filtran los tokens de JSON.pe y el secreto por defecto del JWT; se pueden falsificar sesiones y consumir el servicio de pago | S3 |
| Alguien alcanza la red del servidor | Lee o borra MongoDB sin credenciales; publica eventos falsos en RabbitMQ | S4, S5, S6 |
| RabbitMQ se reinicia justo tras emitir una póliza | La póliza queda emitida y el cliente no recibe el mensaje ni se registra el fallo | A1 |
| JSON.pe WhatsApp deja de responder | Los mensajes agotan 4 intentos y pasan a la DLQ; nadie los reprocesa | A6, sin monitoreo |
| Se levantan dos réplicas del backend | Fallan de forma aleatoria el MFA y el login con Facebook | A3 |
| La cola de auditoría crece durante meses | RabbitMQ consume disco y memoria hasta degradarse | A4 |
| Un XSS inyecta un script en la SPA | Roba el JWT de `localStorage` y lo usa 8 horas, aunque el usuario cierre sesión | S8, S11 |

---

## 7. Prioridad de corrección

**Antes de cualquier despliegue fuera de un entorno local:**

1. Cerrar S1: el registro no debe aceptar `rol`.
2. Cerrar S2: reglas de rol en todos los endpoints y control de propiedad para CLIENTE.
3. Sacar los secretos del código y rotar los tokens ya expuestos (S3); quitar el usuario demo (S7).
4. Autenticar MongoDB y RabbitMQ y dejar de publicar sus puertos (S4, S5).
5. Poner TLS delante de la aplicación (S6).

**Después:**

6. Outbox o reintento de publicación (A1) y monitoreo de la DLQ (A6).
7. Sesión: expiración corta, revocación y `logout` real (S8); límite de intentos (S9).
8. Cabeceras de seguridad y Swagger protegido (S10, S11).
9. Estado compartido para permitir varias réplicas (A3); resolver `audit.queue` (A4).
10. Ordenar los archivos compose, quitar los restos de Hexagonal y añadir pruebas de integración.
