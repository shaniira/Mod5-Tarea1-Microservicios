# Guía para levantar y probar el proyecto en Docker

Esta guía es para correr **Backend Seguros** completo en tu máquina con Docker, configurar las variables de entorno y probar que todo funciona. Sigue los pasos en orden. Si algo falla, mira la sección 8, "Problemas frecuentes".

## 1. Qué vas a levantar

Con un solo `docker compose` se levantan unos 24 contenedores:

| Qué | Contenedores |
|---|---|
| Interfaz web | `backend-seguros-frontend-1` → http://localhost:5173 |
| Punto de entrada de la API | `api-gateway` → http://localhost:8080/api |
| 6 microservicios | `backend-seguros-identity-service-1`, `customer-service`, `quotation-service`, `policy-service`, `claims-service`, `notification-service` |
| Una base MongoDB por servicio | `identity-mongodb`, `customer-mongodb`, `quotation-mongodb`, `policy-mongodb`, `claims-mongodb`, `notification-mongodb` |
| Mensajería y caché | `backend-seguros-rabbitmq-1`, `redis` |
| Observabilidad (opcional) | `grafana`, `prometheus`, `alertmanager`, `jaeger`, `loki`, `promtail`, `otel-collector`, `blackbox` |

Solo el frontend (5173) y el gateway (8080) abren puertos. Todo lo demás es interno.

## 2. Requisitos

| Requisito | Detalle |
|---|---|
| **Docker Desktop** (Windows o Mac) o Docker Engine + Compose v2 (Linux) | Comprueba con `docker compose version`. En Windows, con el backend WSL 2 |
| **Memoria para Docker** | Al menos **8 GB** asignados (Docker Desktop → Settings → Resources). El stack completo usa unos 5–6 GB |
| **Disco** | Unos 10 GB libres para imágenes y dependencias de Maven |
| **Git** | En Windows, **Git Bash**: todos los comandos de esta guía son de bash |
| **Node.js 18 o más** | Solo para los scripts de prueba (`caos.sh`) y para leer JSON en los ejemplos |
| Puertos libres | 5173 y 8080; con observabilidad, también 3000, 9090, 9093 y 16686 |

No necesitas Java ni Maven instalados: todo se compila dentro de Docker.

## 3. Obtener el código

```bash
git clone <url-del-repositorio>
cd Mod5-Tarea1-Microservicios
git checkout docs/migracion-microservicios
git pull
```

La rama de trabajo es `docs/migracion-microservicios`. El código del monolito original ya no está en la rama; queda en la etiqueta `monolito-final` (no lo necesitas para correr el proyecto).

## 4. Variables de entorno (archivo `.env`)

Las variables van en un archivo `.env` en la **raíz del repositorio**. Ese archivo **nunca se sube a git** (está en `.gitignore`), porque lleva secretos. Créalo desde la plantilla:

```bash
cp .env.example .env
```

**El proyecto arranca aunque dejes todo vacío.** Cada variable tiene un valor por defecto para uso local, o desactiva solo la función que la necesita. Completa solo lo que quieras probar. Después de cambiar el `.env`, aplica los cambios con `docker compose up -d`: Compose recrea los contenedores afectados. Las variables del frontend (`GOOGLE_CLIENT_ID`, `FRONTEND_*`) se leen al **construir** la imagen, así que para esas usa `docker compose up -d --build frontend`.

**Los secretos se piden por un canal privado** (mensaje directo, gestor de contraseñas). Nunca se escriben en el repositorio, en un issue ni en un chat de grupo.

### 4.1 Claves de las bases de datos: no hace falta tocarlas en local

| Variable | Para qué | Valor si queda vacía | Cómo obtenerla |
|---|---|---|---|
| `IDENTITY_MONGO_ROOT_PASSWORD`, `IDENTITY_DB_PASSWORD` | Usuario administrador y usuario del servicio en la base de identity | `identity-root-local` / `identity-local` | Las inventas tú (cualquier texto largo). Solo importan fuera de tu máquina |
| `CUSTOMER_…`, `CLAIMS_…`, `QUOTATION_…`, `POLICY_…`, `NOTIFICATION_…` (`_MONGO_ROOT_PASSWORD` y `_DB_PASSWORD`) | Lo mismo para cada base | `<servicio>-root-local` / `<servicio>-local` | Igual que la anterior |
| `GRAFANA_ADMIN_PASSWORD` | Contraseña del usuario `admin` de Grafana | `grafana-local` | La inventas tú |

Si cambias una de estas claves después del primer arranque, no pasa nada: cada MongoDB vuelve a sincronizar sus usuarios con el `.env` cada vez que arranca.

**No agregues `RABBITMQ_DEFAULT_USER` ni `RABBITMQ_DEFAULT_PASS`.** El usuario de RabbitMQ se crea solo la primera vez. Si lo cambias después, los servicios se quedan sin acceso a RabbitMQ.

### 4.2 Interruptores: dejarlos como están

| Variable | Valor | Por qué |
|---|---|---|
| `CUSTOMER_EVENTS_PUBLISH_ENABLED`, `CLAIMS_EVENTS_PUBLISH_ENABLED`, `QUOTATION_EVENTS_PUBLISH_ENABLED`, `POLICY_EVENTS_PUBLISH_ENABLED` | `true` | Cada servicio publica sus eventos. Con `false` las copias de datos entre servicios dejan de actualizarse |

### 4.3 Login con Google (opcional)

| Variable | Para qué | Si queda vacía |
|---|---|---|
| `GOOGLE_CLIENT_ID` | Botón "Continuar con Google" del frontend y verificación del token en identity-service | No aparece el login con Google; el login con usuario y contraseña funciona igual |

Cómo obtenerla:
1. Entra a https://console.cloud.google.com y crea o elige un proyecto.
2. Ve a **APIs y servicios → Pantalla de consentimiento de OAuth**. Configúrala como "Externa", y en "Usuarios de prueba" agrega los correos que van a probar.
3. Ve a **Credenciales → Crear credenciales → ID de cliente de OAuth**, de tipo **Aplicación web**.
4. En **Orígenes de JavaScript autorizados** agrega `http://localhost:5173`.
5. Copia el **ID de cliente** (termina en `.apps.googleusercontent.com`) en `GOOGLE_CLIENT_ID`. El "secreto de cliente" no se usa.
6. Reconstruye el frontend: `docker compose up -d --build frontend identity-service`.

Si quieres usar el mismo proyecto de Google que ya tiene el equipo, pide el ID de cliente a quien lo creó y pídele que agregue tu correo como usuario de prueba.

### 4.4 Login con Facebook (opcional)

| Variable | Para qué | Si queda vacía |
|---|---|---|
| `FACEBOOK_APP_ID` | Identificador de la app de Facebook | El login con Facebook no funciona; el resto sí |
| `FACEBOOK_APP_SECRET` | Secreto de la app: identity-service lo usa para canjear el código de Facebook | Ídem |
| `FACEBOOK_APP_NAME` | Solo informativo | Nada |
| `FACEBOOK_REDIRECT_URI` | Adónde vuelve Facebook después del login. En local: `http://localhost:8080/api/auth/facebook/callback` | El login con Facebook falla |
| `FACEBOOK_SCOPES` | Permisos que se piden | `public_profile,email` |
| `FACEBOOK_FRONTEND_CALLBACK_URL` | Pantalla del frontend a la que vuelve el usuario | `http://localhost:5173/login` |
| `FACEBOOK_LOGIN_TICKET_TTL_SECONDS` | Vida del ticket de un solo uso con el que el frontend recibe la sesión | `60` |
| `FACEBOOK_TOKEN_ENCRYPTION_KEY` | Clave **propia de la aplicación** (no es de Facebook) para cifrar el token de Facebook guardado en la base | El login con Facebook falla |

Cómo obtenerlas:
1. Entra a https://developers.facebook.com/apps y crea una app de tipo "Consumidor" / "Autenticar usuarios con Facebook Login".
2. En **Configuración → Básica** copia el **ID de la app** (`FACEBOOK_APP_ID`) y el **Secreto de la app** (`FACEBOOK_APP_SECRET`).
3. En **Facebook Login → Configuración**, en "URI de redireccionamiento de OAuth válidos", agrega exactamente `http://localhost:8080/api/auth/facebook/callback`.
4. En **Roles de la app** agrégate como administrador, desarrollador o tester. Mientras la app esté en modo desarrollo, solo esas cuentas pueden entrar.
5. En **Revisión de la app → Permisos y funciones**, agrega `email`.
6. Genera la clave de cifrado (32 bytes en Base64) y cópiala en `FACEBOOK_TOKEN_ENCRYPTION_KEY`:
   ```bash
   node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
   ```
   Guárdala: si cambia, los tokens de Facebook ya cifrados no se pueden leer, y esos usuarios tendrán que volver a entrar con Facebook.

### 4.5 JSON.pe: consulta de placas y WhatsApp (opcional)

| Variable | Para qué | Si queda vacía |
|---|---|---|
| `JSONPE_TOKEN` | Token de la API de placas: customer-service autocompleta los datos del vehículo con la placa | La consulta de placas responde "sin datos" y el vehículo se registra a mano; nada más se afecta |
| `JSONPE_BASE_URL` | Dirección de la API de placas | `https://api.json.pe` (real). Para pruebas: `http://jsonpe-mock:8080` (simulador, sección 6.4) |
| `WHATSAPP_TOKEN` | Token de la API de WhatsApp de JSON.pe: notification-service avisa por WhatsApp cuando se emite una póliza | El envío falla; tras los reintentos, el mensaje queda en la cola de errores (DLQ). Las pólizas se emiten igual |
| `WHATSAPP_BASE_URL` | Dirección de la API de WhatsApp | `https://api.whatsapp.json.pe` (real). Para pruebas: `http://whatsapp-mock:8080` (simulador) |

Cómo obtenerlas:
- Los tokens salen del panel de la cuenta de **JSON.pe** (https://json.pe). Allí, tanto la API de placas como la de WhatsApp muestran su token; la de WhatsApp además exige vincular un número. La cuenta del proyecto la administra Shanira: pídele los tokens por un canal privado.
- **Para probar, usa los simuladores** en lugar de los servicios reales (sección 6.4). No consumes créditos de JSON.pe y no se envían WhatsApp a teléfonos reales: los clientes demo tienen un número de teléfono real.

### 4.6 Alertas por correo (opcional)

| Variable | Para qué | Si queda vacía |
|---|---|---|
| `ALERTMANAGER_SMTP_PASSWORD` | Contraseña de aplicación de Gmail para que Alertmanager envíe por correo las alertas (servicio caído, DLQ con mensajes…) | El stack arranca igual; las alertas se ven en Prometheus (http://localhost:9090/alerts) y en Alertmanager (http://localhost:9093), pero no llegan por correo |

El envío está configurado en `infra/observability/alertmanager/alertmanager.yml` con la cuenta del proyecto: remitente y destino `ramirezlisset361@gmail.com`. La contraseña de aplicación tiene que ser **de esa cuenta**. Si quieres recibir las alertas en tu propio Gmail:
1. En tu cuenta de Google activa la verificación en 2 pasos.
2. Entra a https://myaccount.google.com/apppasswords, crea una contraseña de aplicación y copia las 16 letras, sin espacios, en `ALERTMANAGER_SMTP_PASSWORD`.
3. En tu copia local de `alertmanager.yml` cambia `smtp_from`, `smtp_auth_username` y `to` por tu correo. **No subas ese cambio.**

### 4.7 Variables avanzadas (no están en la plantilla; normalmente no se tocan)

| Variable | Para qué |
|---|---|
| `FRONTEND_API_URL`, `FRONTEND_API_ORIGIN`, `CORS_ALLOWED_ORIGINS` | Dirección del gateway que usa el frontend y orígenes permitidos. Solo cambian con HTTPS local (sección 6.5) |
| `CUSTOMER_SERVICE_URL`, `CLAIMS_SERVICE_URL`, `QUOTATION_SERVICE_URL`, `POLICY_SERVICE_URL` | A qué servicio envía el gateway cada ruta |
| `GOOGLE_ISSUER`, `GOOGLE_JWK_SET_URI`, `FACEBOOK_GRAPH_BASE_URL`, `FACEBOOK_TOKEN_URI` | Direcciones de Google y Facebook; se cambian solo para apuntar a simuladores |
| `WHATSAPP_CB_WAIT_OPEN` | Cuánto espera notification-service antes de reintentar con WhatsApp caído (`30s`) |

## 5. Levantar el proyecto

Todo se ejecuta **desde la raíz del repositorio**.

### 5.1 Primera vez

```bash
docker compose build          # 20–40 min la primera vez: descarga dependencias y corre las pruebas de cada imagen
docker compose up -d
docker compose ps             # espera a que los servicios digan "healthy" (2–5 min)
```

Construir aparte (`build` y después `up`) evita que las 7 imágenes compitan por la CPU mientras los servicios arrancan.

En el primer arranque, cada servicio crea sus datos de prueba:

| Qué | Datos |
|---|---|
| Usuario | `admin` / `Admin123*` (ADMIN, sin MFA) |
| Clientes demo | Ana (`10000000-0000-0000-0000-000000000001`) y Luis (`…-000000000002`), cada uno con un vehículo (`20000000-0000-0000-0000-000000000001` y `…-000000000002`) |
| Tablas tarifarias | 3 tablas (auto y camioneta vigentes, un borrador) |

La clave con la que identity firma los tokens se genera sola la primera vez y se guarda en un volumen.

### 5.2 Las siguientes veces

```bash
docker compose up -d          # arranca lo que ya existe (sin reconstruir)
docker compose up -d --build  # si bajaste cambios de código con git pull
```

### 5.3 Con observabilidad (Grafana, Prometheus, Jaeger, Alertmanager)

```bash
docker compose -f docker-compose.yml -f infra/observability/docker-compose.observability.yml up -d
```

## 6. Probar

Esta sección es la prueba rápida. **El plan de pruebas completo está en [CASOS-DE-PRUEBA.md](CASOS-DE-PRUEBA.md):** cada flujo explicado (qué pasa por dentro), casos por servicio, seguridad, fallas y caos, colas y DLQ, observabilidad y alertas, consistencia, carga y una planilla para anotar resultados.

### 6.1 En el navegador

Abre http://localhost:5173 y entra con `admin` / `Admin123*`. Puedes ver clientes y vehículos, cotizar, emitir pólizas, registrar siniestros y evaluar renovaciones.

### 6.2 Crear más usuarios (opcional)

Con el token de `admin` (sección 6.3, paso 1) puedes crear cuentas de personal:

```bash
curl -s -X POST http://localhost:8080/api/auth/register -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"username":"agente1","password":"Agente123*","rol":"AGENTE"}' -w '%{http_code}\n'
```

Los roles son `ADMIN`, `AGENTE`, `ACTUARIO` y `CLIENTE`. Sin token, el registro solo crea cuentas `CLIENTE`.

### 6.3 El flujo completo por la API

Copia y pega en Git Bash, bloque por bloque. Cada paso muestra el código HTTP esperado.

```bash
API=http://localhost:8080/api
campo() { node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{const j=JSON.parse(s);console.log($1)})"; }

# 1. Login (el token viene anidado en token.token). En una base nueva: admin / Admin123*.
#    Si la respuesta trae "requiresMfa": true, esa cuenta tiene MFA: usa otra sin MFA (6.2).
USUARIO=admin; CLAVE='Admin123*'
TOKEN=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USUARIO\",\"password\":\"$CLAVE\"}" | campo 'j.token && j.token.token')
AUTH="Authorization: Bearer $TOKEN"; echo "${TOKEN:0:20}..."

# 2. Clientes demo (200)
curl -s -H "$AUTH" $API/clientes | campo 'j.map(c=>c.id+" "+c.nombres).join("\n")'

# 3. Cotizar para Ana y su vehículo (201): prima 1412.50
COT=$(curl -s -X POST $API/cotizaciones -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"clienteId":"10000000-0000-0000-0000-000000000001","vehiculoId":"20000000-0000-0000-0000-000000000001","siniestrosResponsables":0}' | campo 'j.id')
echo "cotización $COT"

# 4. Aceptarla (200). Publica quote.accepted: policy-service la recibe en 1–2 s
curl -s -X PATCH $API/cotizaciones/$COT/aceptar -H "$AUTH" -o /dev/null -w '%{http_code}\n'; sleep 3

# 5. Emitir la póliza (201). Dispara la saga: claims, quotation y notification (WhatsApp) reaccionan
INICIO=$(date -d '+7 days' +%F)
POL=$(curl -s -X POST $API/polizas -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"cotizacionId\":\"$COT\",\"inicioVigencia\":\"$INICIO\"}" | campo 'j.id')
echo "póliza $POL"; sleep 3

# 6. La cotización quedó EMITIDA (la actualizó el evento policy.issued)
curl -s -H "$AUTH" $API/cotizaciones/$COT | campo 'j.estado'

# 7. Emitir otra vez la misma cotización: 422 (la saga garantiza una sola póliza)
curl -s -X POST $API/polizas -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"cotizacionId\":\"$COT\",\"inicioVigencia\":\"$INICIO\"}" -o /dev/null -w '%{http_code}\n'

# 8. Registrar un siniestro responsable sobre la póliza (201). claims publica claim.registered
FECHA=$(date -d '+10 days' +%F)
SIN=$(curl -s -X POST $API/polizas/$POL/siniestros -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"fecha\":\"$FECHA\",\"tipo\":\"CHOQUE\",\"montoEstimado\":700,\"responsabilidadAsegurado\":true,\"gravedad\":\"LEVE\",\"estado\":\"REPORTADO\"}" | campo 'j.id')
echo "siniestro $SIN"; sleep 3

# 9. Evaluar la renovación con el siniestro abierto: 422 SINIESTROS_PENDIENTES
curl -s -X POST $API/renovaciones/poliza/$POL/evaluar -H "$AUTH" | campo 'j.codigo'

# 10. Cerrar el siniestro (200) y volver a evaluar (201): la prima sube 8 % por el siniestro responsable
curl -s -X PATCH $API/polizas/$POL/siniestros/$SIN/estado -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"estado":"LIQUIDADO"}' -o /dev/null -w '%{http_code}\n'; sleep 3
REN=$(curl -s -X POST $API/renovaciones/poliza/$POL/evaluar -H "$AUTH" | campo 'j.id+" "+j.estado+" "+j.primaAnterior+" -> "+j.nuevaPrima')
echo "renovación $REN"; REN=${REN%% *}

# 11. Aprobar (200) y generar la póliza renovada (201). Al generar, policy confirma los siniestros con claims
curl -s -X PATCH $API/renovaciones/$REN/aprobar -H "$AUTH" -o /dev/null -w '%{http_code}\n'
curl -s -X POST $API/renovaciones/$REN/generar-poliza -H "$AUTH" | campo 'j.numero'

# 12. La póliza original ya no está vigente: un siniestro nuevo sobre ella da 422 POLIZA_NO_VIGENTE
curl -s -X POST $API/polizas/$POL/siniestros -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"fecha\":\"$FECHA\",\"tipo\":\"CHOQUE\",\"montoEstimado\":700,\"responsabilidadAsegurado\":false,\"gravedad\":\"LEVE\",\"estado\":\"REPORTADO\"}" | campo 'j.codigo'
```

El login tiene un límite de **1 por segundo** (ráfaga de 5). Si repites el paso 1 muy seguido, recibirás `429`: espera un segundo.

### 6.4 Sin servicios externos reales (recomendado para probar)

Levanta los simuladores de WhatsApp y de la API de placas, y apunta los servicios a ellos:

```bash
WHATSAPP_BASE_URL=http://whatsapp-mock:8080 JSONPE_BASE_URL=http://jsonpe-mock:8080 \
  docker compose --profile whatsapp-mock --profile jsonpe-mock up -d
```

Así, los WhatsApp de la emisión llegan al simulador (`docker logs notification-service | grep ENVIADA`) y ningún teléfono real recibe mensajes. Para volver a los servicios reales, ejecuta `docker compose up -d` sin esas variables.

### 6.5 HTTPS local (opcional)

```bash
FRONTEND_API_URL=https://localhost:8444/api FRONTEND_API_ORIGIN=https://localhost:8444 \
CORS_ALLOWED_ORIGINS=https://localhost:8443 docker compose --profile tls up -d --build
```

El frontend queda en https://localhost:8443. El navegador avisará que el certificado es de una CA local: acéptalo.

### 6.6 Observabilidad

Con el stack de observabilidad levantado (5.3):

| Herramienta | Dirección | Qué mirar |
|---|---|---|
| Grafana | http://localhost:3000 (`admin` / `GRAFANA_ADMIN_PASSWORD` o `grafana-local`) | Tablero "Backend Seguros — Resumen". En Explore → Loki, busca `{service=~".+"} \|= "<X-Correlation-Id>"` para seguir una petición por todos los servicios |
| Jaeger | http://localhost:16686 | Una emisión de póliza es una sola traza: gateway → policy → RabbitMQ → notification, claims y quotation |
| Prometheus | http://localhost:9090/alerts | Alertas activas |
| Alertmanager | http://localhost:9093 | Alertas agrupadas y envíos por correo |

Cada respuesta del gateway trae la cabecera `X-Correlation-Id` (`curl -i`); con ese valor encuentras la petición en Grafana.

### 6.7 Pruebas de resiliencia y consistencia (scripts)

Con los simuladores de la sección 6.4 levantados:

```bash
bash infra/operacion/caos.sh claims rabbitmq redis   # apaga componentes y comprueba que el sistema se degrada como está previsto
bash infra/operacion/caos.sh                         # todos los casos básicos (~25 min)
sh infra/operacion/reconciliar.sh                    # compara cada copia de datos con su fuente: debe decir "Todo coincide"
sh infra/mongo/respaldar.sh                          # respalda las 6 bases en respaldos/
```

La lista de casos de caos está al inicio de `infra/operacion/caos.sh`.

### 6.8 Pruebas automáticas (sin Maven instalado)

```bash
cd services/policy-service      # o cualquier otro servicio
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W 2>/dev/null || pwd):/app" -v "$HOME/.m2:/root/.m2" -w /app \
  maven:3.9.9-eclipse-temurin-21 mvn -q test
```

Hay 272 pruebas en total: identity 62, policy 52, quotation 48, customer 43, claims 33, notification 30 y gateway 4. Las del gateway necesitan Redis: córrelas con el stack levantado, agregando `--network gateway_network -e REDIS_HOST=redis`.

## 7. Detener y limpiar

```bash
docker compose stop           # detiene todo y conserva contenedores y datos
docker compose down           # borra contenedores y redes; los datos (volúmenes) se conservan
```

Con observabilidad o perfiles, usa los mismos `-f` y `--profile` también al detener.

**`docker compose down -v` borra también todos los datos**: bases, clave de firma de los tokens y colas. Úsalo solo si quieres empezar de cero; en el siguiente `up` se vuelven a sembrar los datos demo.

## 8. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| Un servicio figura `unhealthy` o `starting` los primeros minutos | Es normal mientras arrancan 7 JVM a la vez. Espera y revisa con `docker compose ps`. Si sigue así después de 10 min: `docker compose logs <servicio>` |
| La primera petición después de arrancar responde `503` | El servicio todavía estaba arrancando o el circuito del gateway se abrió. Espera unos segundos y repite |
| `429 Too Many Requests` en el login | Límite de 1 login por segundo. Espera y repite |
| `401` en todas las peticiones | El token venció (dura 8 h) o se cerró la sesión. Vuelve a hacer login |
| `port is already allocated` | Otro programa usa el 5173 o el 8080. Ciérralo, o cambia el puerto publicado en `docker-compose.yml` solo en tu copia |
| `bad interpreter` o `\r: command not found` en un `.sh` | El script se guardó con fin de línea CRLF. El repositorio fuerza LF (`.gitattributes`); si lo editaste, vuelve a guardarlo con LF o ejecuta `git checkout -- <archivo>` |
| En Git Bash, una ruta que empieza con `/` se convierte en `C:/Program Files/Git/...` | Antepón `MSYS_NO_PATHCONV=1` al comando |
| La construcción falla por falta de memoria o se congela el equipo | Sube la memoria de Docker (sección 2) y construye un servicio a la vez: `docker compose build policy-service` |
| El login con Google dice que el correo no es cliente | Un usuario CLIENTE entra con Google solo si su correo ya está registrado como cliente. El personal entra con usuario y contraseña |
| Las pólizas se emiten pero no sale el WhatsApp | Sin `WHATSAPP_TOKEN` real, el envío falla y el mensaje termina en la DLQ. Usa el simulador (6.4) |
| Grafana, Jaeger o Prometheus no abren | Solo existen con el archivo de observabilidad (5.3) |

## 9. Dónde seguir leyendo

| Documento | Para qué |
|---|---|
| [README.md](README.md) | Visión general, arquitectura y patrones |
| [DOCKER-EJECUCION.md](DOCKER-EJECUCION.md) | Referencia técnica del stack: redes, volúmenes y aislamiento |
| [doc/5. Microservicios/o_GUIA-OPERACION.md](doc/5.%20Microservicios/o_GUIA-OPERACION.md) | Qué hacer ante cada alerta, DLQ, respaldos y reconciliación |
| [doc/5. Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md](doc/5.%20Microservicios/c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md) | Arquitectura y diagrama |
| [doc/0. USUARIOS-DE-PRUEBA.md](doc/0.%20USUARIOS-DE-PRUEBA.md) | Usuarios del entorno original (en una instalación nueva solo existe `admin`) |
| [contracts/](contracts/) | Contratos de eventos y OpenAPI de cada servicio |
