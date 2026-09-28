# Retiro del monolito del repositorio (cierre de la migración)

**Fecha:** 2026-09-28 · **Responsable:** Shanira · **Rama:** `docs/migracion-microservicios`

Este documento explica cómo se sacó del repositorio el monolito (`Arquitectura-Clean`): por qué, qué dependía todavía de él, qué se hizo paso a paso y en qué archivos, qué errores aparecieron, cómo se resolvieron y cómo se verificó que el sistema funciona sin él.

## 0. Resumen

- El monolito no recibía tráfico desde el corte del 2026-09-27 (`m_…`, sección 7). Seguía en el repositorio como plan de reversa y porque el Compose del stack completo vivía en su carpeta.
- Ahora **la carpeta `Arquitectura-Clean` ya no existe**:
  - El Compose, su variante de depuración y el `.env` están en la raíz.
  - Una instalación nueva arranca solo con los microservicios y sus datos demo.
  - El código del monolito queda en la etiqueta de git **`monolito-final`**.
  - Su base queda en el respaldo `respaldos/monolito-andina_seguros_clean-2026-09-27.archive.gz`.
- Verificado:
  - `docker compose config`, válido.
  - 7 servicios sanos, levantados desde la raíz.
  - Siembra de tablas tarifarias en una base vacía.
  - Reconciliación de las 7 proyecciones.
  - Pruebas de caos.
  - 48 pruebas de quotation-service.

## 1. Por qué ahora

| Motivo | Detalle |
|---|---|
| Ya no aportaba nada en ejecución | Desde el corte, el gateway envía todo el negocio a customer, claims, quotation y policy; `backend` y su MongoDB solo arrancaban con `--profile monolito` (paso 6.10) |
| El periodo de seguridad terminó | La reversa servía mientras se comprobaba que los microservicios funcionaban solos. La fase 7 lo comprobó (caos, carga, reconciliación, contratos) |
| Hay respaldo | La base del monolito se respaldó y se probó restaurándola al retirarlo del Compose (paso 6.10: 9 colecciones con los mismos conteos). Antes de borrar el volumen se volvió a comprobar la integridad del archivo (`gzip -t`) |
| El proyecto se presenta como microservicios | Con el Compose dentro de `Arquitectura-Clean/`, todo el sistema seguía "colgando" de la carpeta del monolito, y los documentos decían `cd Arquitectura-Clean` |

## 2. Qué dependía todavía del monolito (análisis previo)

Antes de borrar se buscó todo lo que nombraba `Arquitectura-Clean`, `backend` o `andina-clean-mongodb`:

| Dependencia | Tipo | Cómo se resolvió |
|---|---|---|
| `docker-compose.yml`, `docker-compose.debug.yml`, `.env.example` y `.env` estaban en `Arquitectura-Clean/`, con rutas `../infra`, `../services`, `../gateway` | Funcional | Se movieron a la raíz y las rutas pasaron a `./` (sección 3.3) |
| **Tablas tarifarias:** solo llegaban a `quotation_db` con `migrar-cotizaciones.sh`, que las copiaba de la base del monolito. Sin ellas no se puede cotizar | **Funcional (la única de datos)** | quotation-service las siembra (sección 3.5) |
| Clientes demo: customer-service tenía `APP_DEMO_DATA_ENABLED=false` porque los clientes llegaban de la migración | Funcional en una instalación nueva | Se activó (sección 3.5) |
| `services/*/migracion/migrar-*.sh` (5 scripts) leen `andina-clean-mongodb` | Scripts de una sola vez | Se conservan como evidencia, marcados como históricos (sección 3.7) |
| `.github/workflows/backend.yml` | CI del monolito | Borrado |
| `k8s/archivo-monolito/` (manifiestos 20-23) | Archivo | Borrado (queda en la etiqueta) |
| Overlay de observabilidad, `caos.sh`, `carga.js`, comentarios de Alertmanager, gateway, k8s y `Roles.java` | Rutas y comentarios | Actualizados (sección 3.6) |
| 18 documentos con `cd Arquitectura-Clean` o `Arquitectura-Clean/.env` | Documentación | Operativos actualizados; históricos con una nota (sección 6) |

No había ninguna dependencia de código: ningún microservicio importa nada del monolito, y el gateway ya no tenía ruta hacia él (paso 6.10).

## 3. Qué se hizo, paso por paso

### 3.1 Etiqueta `monolito-final`

`git tag -a monolito-final` sobre el commit `6077ee9`, el último con el código del monolito. **Para qué:** poder volver a ver o levantar el monolito sin que ocupe la rama. Se consulta con `git show monolito-final:Arquitectura-Clean/pom.xml` o `git worktree add ../monolito monolito-final`. La etiqueta es local, igual que los commits: se sube con `git push origin monolito-final`.

### 3.2 Respaldo comprobado

`respaldos/monolito-andina_seguros_clean-2026-09-27.archive.gz` (14 KB, no versionado porque contiene datos de clientes). Se probó al tomarlo, restaurándolo en un MongoDB temporal. Antes de borrar el volumen se volvió a comprobar su integridad: `gzip -t`, sin errores.

### 3.3 Compose a la raíz

| Archivo | Acción | Por qué |
|---|---|---|
| `docker-compose.yml` (raíz, antiguo) | `git rm` | Era un subconjunto viejo que solo levantaba el monolito (`name: andina-arquitecturas`) |
| `Arquitectura-Clean/docker-compose.yml` → `docker-compose.yml` | `git mv` (conserva el historial) | El stack completo se levanta desde la raíz |
| `Arquitectura-Clean/docker-compose.debug.yml` → `docker-compose.debug.yml` | `git mv` | Ídem |
| `Arquitectura-Clean/.env.example` → `.env.example` | `git mv` | Plantilla de variables junto al Compose |
| `Arquitectura-Clean/.env` → `.env` | `mv` (no se versiona) | Compose lee el `.env` de la carpeta del proyecto; el contenido (secretos) no se leyó ni se cambió |

Cambios dentro del Compose:
- Rutas `../infra`, `../services`, `../gateway` y `../frontend` → `./…`.
- Encabezado nuevo que explica dónde quedó el monolito.
- Se quitaron el servicio `mongodb` (perfil `monolito`), el servicio `backend` (perfil `monolito`), la red `clean_network` y el volumen `clean_mongo_data`.
- Se actualizaron 9 comentarios que hablaban del backend o de la reversa.

**Decisión: el nombre del proyecto de Compose se mantiene `andina-clean`** (`name:` en la primera línea). Cambiarlo habría roto varias cosas:
- Los contenedores habrían cambiado de nombre: `andina-clean-rabbitmq-1` e `andina-clean-identity-service-1`, que usan `caos.sh`, `reconciliar.sh`, `reprocesar-dlq.sh` y la guía de operación.
- El volumen de RabbitMQ (`andina-clean_rabbitmq_data`) habría sido otro.
- El filtro de Promtail por proyecto habría dejado de recoger los logs.

Es solo un nombre, y conservarlo evita recrear datos.

`docker-compose.debug.yml` quedó solo con los puertos de RabbitMQ: se quitaron los de `mongodb` (27020) y `backend` (8083).

`.env.example`: sin `BACKEND_MONGO_ROOT_PASSWORD` ni `BACKEND_DB_PASSWORD`, y sin menciones a la reversa al monolito.

### 3.4 Overlay de observabilidad

En `infra/observability/docker-compose.observability.yml`, las rutas `../infra/observability/…` pasaron a `./infra/observability/…`. **Por qué:** Compose resuelve las rutas de todos los archivos `-f` contra la carpeta del proyecto, que ahora es la raíz.

### 3.5 Datos demo sin la base del monolito

| Servicio | Cambio | Para qué |
|---|---|---|
| quotation-service | **Nuevo** `frameworksdrivers/config/DemoDataInitializer.java` (se activa con `app.demo-data.enabled`, variable `APP_DEMO_DATA_ENABLED`). Siembra las 3 tablas del monolito con **los mismos ids**: `DEMO-AUTO` v1 (AUTO, 1250.00, VIGENTE), `DEMO-CAMIONETA` v1 (CAMIONETA, 1680.00, VIGENTE) y `DEMO-AUTO` v2 (AUTO, 1320.00, BORRADOR). Prima mínima 900.00, vigencia 2025–2030, uso particular y factor de antigüedad ×1.10. Solo crea las que no existen | Sin tablas no se puede cotizar, y hasta ahora solo llegaban migrando la base del monolito. Con los mismos ids, en el entorno actual (ya migrado) no hace nada, y las cotizaciones existentes siguen apuntando a sus tablas |
| quotation-service | `application.yml`: `app.demo-data.enabled: ${APP_DEMO_DATA_ENABLED:false}`. Compose: `APP_DEMO_DATA_ENABLED: "true"` | Apagado por defecto (en producción no se siembra) y encendido en el entorno local |
| quotation-service | **Nuevo** `DemoDataInitializerTest` (2 pruebas): con la base vacía crea las 3 en orden; si existen, no guarda nada | Asegurar que la siembra es idempotente |
| customer-service | Compose: `APP_DEMO_DATA_ENABLED` de `"false"` a `"true"` (el inicializador ya existía desde la fase 3) | Estaba apagado porque los clientes llegaban de la migración |
| identity-service | Sin cambios (ya sembraba `admin` / `Admin123*`) | — |

### 3.6 Scripts, comentarios y manifiestos

| Archivo | Cambio |
|---|---|
| `infra/operacion/caos.sh` | Los casos `replicas` y `relay-lote` hacían `cd Arquitectura-Clean && docker compose -f … -f ../infra/…`; ahora `docker compose -f docker-compose.yml -f infra/observability/…` desde la raíz |
| `infra/carga/carga.js` | Instrucciones de uso desde la raíz |
| `infra/observability/alertmanager/alertmanager.yml` | La contraseña va en el `.env` de la raíz |
| `gateway/src/main/resources/application.yml`, `gateway/pom.xml` | Comentarios que nombraban `Arquitectura-Clean` |
| `Roles.java` de customer, claims, quotation y policy | La referencia a las reglas del monolito apunta a la etiqueta `monolito-final` |
| `k8s/11`, `30`, `33`, `51`, `53`, `72`, `README.md` | Comentarios que mencionaban el backend o `archivo-monolito/`. En `72` se aclara que el relay admite varias réplicas desde la fase 7 |

### 3.7 Scripts de migración marcados como históricos

A los 5 scripts `services/*/migracion/migrar-*.sh` se les agregó, bajo el `#!`, una cabecera **HISTÓRICO**. Explica tres cosas:
- Ya se ejecutaron durante el corte.
- No se pueden volver a usar, porque leen `andina-clean-mongodb`.
- Cómo consultar aquellos datos: restaurar el respaldo.

**Por qué no se borraron:** son la evidencia de cómo se copiaron los datos con huella SHA-256, y los documentos de las fases 2 a 6 los citan.

### 3.8 Borrado

| Qué | Cantidad |
|---|---|
| `Arquitectura-Clean/src` | 202 archivos (código y pruebas del monolito) |
| `Arquitectura-Clean/pom.xml`, `Dockerfile`, `README.md`, `.dockerignore`, `.gitignore`, `docker/` | 6 |
| `Arquitectura-Clean/target/` (compilados, no versionados) | Borrado del disco |
| `k8s/archivo-monolito/` | 4 manifiestos |
| `.github/workflows/backend.yml` | 1 pipeline |
| Volumen Docker `andina_clean_mongo_data` | Borrado (ningún contenedor lo usaba; respaldo comprobado) |
| Imagen Docker `andina-seguros-clean:1.0.0` | Borrada |

No se tocaron:
- Los contenedores de otros proyectos de la máquina (Onion, Hexagonal, minikube, kind).
- Las imágenes antiguas `andina-seguros-clean:mfa-step1` y `:verify`, que son de trabajos previos.

## 4. Cómo volver a ver el monolito

```bash
git worktree add ../monolito monolito-final     # copia de trabajo con el código del monolito
# su Compose de esa versión está en ../monolito/Arquitectura-Clean/
docker run -d --name monolito-mongo mongo:8
docker exec -i monolito-mongo mongorestore --archive --gzip < respaldos/monolito-andina_seguros_clean-2026-09-27.archive.gz
```

Todo lo escrito desde el corte (2026-09-27) existe solo en los microservicios.

## 5. Errores encontrados y cómo se resolvieron

| # | Error o problema | Causa | Solución | Cómo se verificó |
|---|---|---|---|---|
| 1 | Una instalación nueva no habría podido cotizar | Las tablas tarifarias solo llegaban migrando la base del monolito | `DemoDataInitializer` en quotation-service (3.5) | Imagen de quotation contra un MongoDB **vacío** y temporal: log `Tablas tarifarias demo listas (3 nuevas)` y las 3 tablas con los ids del monolito. En el stack actual: `(0 nuevas)` |
| 2 | Al levantar el stack con `--build` desde la raíz, identity, customer y policy figuraron *unhealthy* durante unos 6 minutos | Se reconstruyeron las 7 imágenes, que ejecutan sus pruebas, y los 7 servicios arrancaron a la vez en un equipo de 4 CPU: identity tardó 359 s en arrancar. No era un error de configuración | Se esperó a que terminaran de arrancar. Recomendación: construir primero (`docker compose build`) y levantar después | Los 7 servicios `healthy` y el gateway respondiendo 200 |
| 3 | **Un script de prueba de la fase 6 llevaba unas 28 h corriendo huérfano** (`paralelo.sh`, la prueba de doble emisión del 2026-09-27) y lanzaba un `mongosh` contra policy-mongodb cada segundo | Esperaba con un `until` **sin límite de tiempo** que una cotización llegara a `accepted_quotes`. Por el error del script que usaba el número en lugar del id (`m_…`, 9.5, fila 2), esa cotización nunca se aceptó, así que la espera no terminaba | Se detuvo el proceso. Lección: toda espera en un script de prueba necesita un límite (`caos.sh` ya lo tiene en `espera`) | Ya no hay procesos `mongosh` huérfanos. Nota: este proceso también sumó carga durante las mediciones de la fase 7 (CPU de las bases, `n_…` defecto 8) |
| 4 | Dos casos de caos fallaron en la primera pasada: `claims` (emitir con claims caído devolvió 400) y `quotation` (emitir devolvió 503, aunque la póliza sí se emitió) | Corrieron recién reconstruido el stack y con el equipo cargado (fila 3 y una quotation temporal de la fila 1). La cotización previa no se creó a tiempo y la emisión se hizo con un id vacío (400), o la emisión superó el timeout del gateway (503) | Se repitieron con el equipo tranquilo (sección 7) | Ver sección 7 |
| 5 | Algunas ediciones con `sed` fallaron (`unterminated s command`) | El delimitador `#` chocaba con una barra invertida al final del patrón | Delimitador `\|` | Herramienta, no del sistema |
| 6 | No hay Python en el equipo | — | Las ediciones de varios archivos se hicieron con scripts de Node | Herramienta, no del sistema |
| 7 | Al renombrar la carpeta de Grafana en la configuración, el tablero siguió en la carpeta vieja ("Andina Seguros") y apareció otra carpeta nueva, vacía | Grafana identifica la carpeta existente por su uid y, al ver otro nombre, crea una carpeta nueva en lugar de renombrar la vieja | Se borró la carpeta nueva vacía y se renombró la existente por la API de Grafana | El tablero "Backend Seguros — Resumen" aparece en la carpeta "Backend Seguros" |
| 8 | El primer PNG del diagrama salió en blanco (20 KB) | La URL `file:///` del SVG se armó mal (falló un `sed` con barras invertidas) y Chrome capturó una página vacía | La URL se arma con `url.pathToFileURL` de Node | PNG de 407 KB, revisado a la vista; se corrigieron además 3 superposiciones de flechas y textos |

## 6. Documentación actualizada

| Documento | Cambio |
|---|---|
| `README.md` (raíz) | Nuevo: portada del proyecto de microservicios. El README anterior (login social y MFA del monolito) se movió sin cambios a `doc/3. Auth Google-MFA/README-LOGIN-SOCIAL-MFA.md` con una nota |
| `DOCKER-EJECUCION.md` | Comandos desde la raíz, datos demo, sección "Monolito retirado" (etiqueta, respaldo, scripts), nombre del proyecto de Compose |
| `CLAUDE.md` | Estado, estructura, comandos y datos demo sin el monolito |
| `o_GUIA-OPERACION.md` | Comandos desde la raíz |
| `d_RUTA-…` | Nota en la fase 6 |
| `k_IMPACTO-EN-EL-MONOLITO.md` | Sección 8 |
| `l_…` | Fila 4 de sus errores (siembra de tarifas) |
| Documentos históricos: `a_`, `b_`, `e_` a `n_`, `doc/0`, `doc/2`, `doc/3`, `doc/4` | Una nota al inicio: las rutas `Arquitectura-Clean/...` describen el estado de su momento |
| READMEs de identity y notification | Comandos desde la raíz |

Además, a pedido, **cada documento de fase (`e_` a `n_`) tiene ahora una sección "Errores encontrados y cómo se resolvieron"**, con el error, su causa, la solución y cómo se verificó. Se reconstruyó a partir de las pruebas de cada fase y de los commits `fix`.

### 6.1 Otros cambios de la misma jornada (a pedido)

| Pedido | Qué se hizo | Archivos |
|---|---|---|
| Actualizar los diagramas y el documento de la arquitectura propuesta con lo implementado | Diagrama nuevo, generado por un script a partir de lo que dicen el código y el Compose (no la propuesta). Muestra:<ul><li>eventos que publica y consume cada servicio;</li><li>llamadas síncronas (lectura de refuerzo, confirmación CP);</li><li>una MongoDB por servicio y las redes reales;</li><li>Alertmanager, decisiones CAP, calidad y operación, Kubernetes.</li></ul>PNG renderizado con Chrome sin ventana. En la propuesta:<ul><li>notas **Implementado:** en cada punto que cambió;</li><li>mermaid actualizado;</li><li>estructura real del repositorio;</li><li>los 11 criterios marcados con su evidencia;</li><li>sección 13 con las diferencias y sus motivos.</li></ul> | `c_DIAGRAMA-ARQUITECTURA-MICROSERVICIOS.svg` y `.png`, `diagramas/generar-diagrama-microservicios.js`, `diagramas/README.md`, `c_ARQ_PROPUESTA-MIGRACION-MICROSERVICIOS.md` |
| Quitar "Andina" de los documentos: el nombre es "Backend Seguros" | 41 reemplazos en 20 documentos ("Andina Seguros" y "JWT de Andina"). No se cambiaron los identificadores técnicos que existen en el sistema, porque el documento dejaría de coincidir con el código:<ul><li>`andina.events`;</li><li>`com.andinaseguros`;</li><li>contenedores `andina-*`;</li><li>la clase `AndinaSegurosApplication`;</li><li>el emisor TOTP literal.</li></ul>El tablero de Grafana y su carpeta pasaron a llamarse "Backend Seguros", para que coincidan con los documentos | Documentos `.md`; `infra/observability/grafana/dashboards/andina-resumen.json`; `grafana/provisioning/dashboards/dashboards.yml` |

## 7. Verificación

| Prueba | Resultado |
|---|---|
| `docker compose config -q` (stack, y stack + observabilidad + depuración) | Válido |
| Pruebas de quotation-service (incluye `DemoDataInitializerTest`) | 48 de 48, sin fallos ni saltadas |
| Stack levantado desde la raíz (con observabilidad) | 7 servicios `healthy`; gateway 200. Se reconstruyeron las 7 imágenes desde los contextos nuevos (`./services/...`) |
| Siembra en base vacía | 3 tablas creadas con los ids del monolito (fila 1 de la sección 5) |
| Siembra en el stack actual | `Tablas tarifarias demo listas (0 nuevas)` y `Datos demo de clientes listos (0 registros nuevos)`: no toca datos existentes |
| Cotizar por el gateway | 201, prima 1412.50 (el mismo cálculo que el monolito) |
| Reconciliación (`sh infra/operacion/reconciliar.sh`) | 7 de 7 coinciden: 19 clientes en 3 proyecciones, 18 vehículos, 320 pólizas, 5 siniestros, 332 cotizaciones aceptadas |
| Caos (`caos.sh` con claims, customer, quotation, policy, rabbitmq, renovacion-reciente, renovacion-claims, revocacion-redis, replicas) | Primera pasada (con el equipo cargado por el proceso huérfano y la prueba temporal, sección 5 filas 3 y 4): 6 de 9 casos correctos (customer, policy, rabbitmq, renovacion-claims, revocacion-redis y replicas; este último creó la segunda réplica con la ruta nueva del Compose). Fallaron claims, quotation y renovacion-reciente. **Repetidos con el equipo tranquilo: los 3 pasan** ("Todas las degradaciones se comportaron como estaba planificado"). En total, 9 de 9 |
