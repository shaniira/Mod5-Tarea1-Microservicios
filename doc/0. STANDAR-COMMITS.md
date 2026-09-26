# Estándar de commits

## 1. Convención

El proyecto utilizará **Conventional Commits** con mensajes breves en español.

Formato general:

```text
tipo(scope): descripción breve en presente
```

Ejemplo:

```text
feat(auth): agrega autenticación MFA con TOTP
```

## 2. Reglas de nomenclatura

- Escribir el `tipo` y el `scope` en minúsculas.
- Escribir la descripción en español y en presente: `agrega`, `corrige`, `actualiza`, `evita`.
- No colocar punto al final de la primera línea.
- Procurar que la primera línea no supere los 72 caracteres.
- Indicar un único cambio principal por commit.
- Usar un `scope` que identifique el módulo afectado.
- No usar mensajes genéricos como `cambios`, `arreglos`, `avance` o `actualización`.
- No mencionar archivos concretos si el mensaje puede expresar la intención del cambio.

## 3. Tipos permitidos

### `feat`

Agrega una funcionalidad nueva visible o útil para el sistema.

```text
feat(auth): agrega inicio de sesión con Google
feat(mfa): agrega configuración TOTP para Google Authenticator
feat(mfa): agrega verificación del segundo factor durante el login
feat(customer): permite actualizar datos del asegurado
```

### `fix`

Corrige un error o comportamiento incorrecto.

```text
fix(jwt): corrige validación de token expirado
fix(auth): evita crear usuarios duplicados con Google
fix(auth): asegura la carga del botón de Google
fix(ui): corrige el nombre de la arquitectura en el frontend
fix(mfa): rechaza desafíos MFA vencidos
```

### `test`

Agrega o modifica pruebas sin incorporar una funcionalidad productiva por sí sola.

```text
test(auth): agrega pruebas del login OAuth2
test(mfa): agrega pruebas de validación TOTP
test(mfa): verifica que un desafío no pueda reutilizarse
```

### `refactor`

Reorganiza el código sin cambiar su comportamiento externo.

```text
refactor(policy): extrae reglas de negocio al dominio
refactor(auth): centraliza la emisión de tokens de sesión
refactor(mfa): separa la validación TOTP mediante puertos
```

### `docs`

Modifica únicamente documentación.

```text
docs(auth): documenta flujo OAuth2 y OpenID Connect
docs(mfa): documenta configuración de Google Authenticator
docs(api): agrega ejemplos de los endpoints MFA
```

### `chore`

Realiza tareas de mantenimiento que no cambian directamente una funcionalidad.

```text
chore(deps): actualiza Spring Boot
chore(repo): elimina archivos temporales del proyecto
chore(config): actualiza variables de entorno de ejemplo
```

Para dependencias que modifican específicamente el proceso de compilación también puede utilizarse `build(deps)`.

### `build`

Modifica el sistema de construcción, empaquetado o dependencias de ejecución.

```text
build(docker): optimiza imagen del servicio
build(maven): agrega dependencias TOTP y ZXing
build(frontend): ajusta configuración de Vite
```

### `ci`

Modifica pipelines y automatizaciones de integración continua.

```text
ci(sonar): agrega análisis de calidad
ci(github): ejecuta pruebas del backend en cada push
ci(docker): valida la construcción de imágenes
```

### `perf`

Mejora el rendimiento sin cambiar el resultado funcional.

```text
perf(customer): reduce consultas durante búsqueda
perf(auth): evita búsquedas repetidas del usuario
perf(api): reduce el tamaño de las respuestas
```

## 4. Scopes recomendados

| Scope        | Uso                                                      |
| ------------ | -------------------------------------------------------- |
| `auth`     | Login local, Google Login y autorización general        |
| `mfa`      | Google Authenticator, TOTP y desafíos de segundo factor |
| `jwt`      | Generación, validación y expiración de tokens JWT     |
| `user`     | Entidad, registro y persistencia de usuarios             |
| `customer` | Clientes o asegurados                                    |
| `policy`   | Pólizas y sus reglas de negocio                         |
| `pricing`  | Cotizaciones, primas y tablas tarifarias                 |
| `ui`       | Cambios visuales generales del frontend                  |
| `router`   | Rutas y guards de Vue Router                             |
| `api`      | Contratos HTTP, controllers y cliente Axios              |
| `mongo`    | Persistencia y migraciones de MongoDB                    |
| `security` | Configuración transversal de seguridad                  |
| `docker`   | Dockerfile y Docker Compose                              |
| `maven`    | Configuración y dependencias Maven                      |
| `deps`     | Actualización general de dependencias                   |
| `sonar`    | Configuración o ejecución de SonarQube                 |
| `docs`     | Estructura general de documentación                     |

Se debe escoger el scope más específico. Por ejemplo, una corrección del código TOTP utiliza `mfa`, no el scope general `security`.

## 5. Commits con cuerpo

Cuando el cambio necesite explicación, agregar un cuerpo después de una línea en blanco:

```text
feat(mfa): agrega desafío temporal al inicio de sesión

Evita emitir el JWT definitivo cuando el usuario tiene MFA habilitado.
El desafío vence en cinco minutos y solo puede utilizarse una vez.
```

El cuerpo debe explicar principalmente el motivo y las decisiones relevantes, no repetir el código modificado.

## 6. Cambios incompatibles

Si se modifica un contrato de forma incompatible, agregar `!` después del tipo o scope y explicar el cambio mediante `BREAKING CHANGE`:

```text
feat(auth)!: cambia el contrato de respuesta del login

BREAKING CHANGE: los endpoints de login ahora devuelven un resultado
discriminado que puede contener un JWT o un desafío MFA.
```

Antes de integrar un cambio incompatible se deben actualizar backend, frontend, pruebas y documentación relacionados.

## 7. Referencia rápida

| Tipo         | Cuándo utilizarlo                                     |
| ------------ | ------------------------------------------------------ |
| `feat`     | Nueva funcionalidad                                    |
| `fix`      | Corrección de un error                                |
| `test`     | Pruebas nuevas o modificadas                           |
| `refactor` | Reorganización sin cambiar comportamiento             |
| `docs`     | Documentación                                         |
| `chore`    | Mantenimiento general                                  |
| `build`    | Compilación, empaquetado o dependencias de ejecución |
| `ci`       | Pipelines y automatización                            |
| `perf`     | Mejora de rendimiento                                  |

## 8. Secuencia sugerida para implementar MFA

Los commits deben representar pasos funcionales y verificables. Una posible secuencia es:

```text
feat(mfa): agrega estado MFA al usuario y persistencia MongoDB
build(maven): agrega dependencias TOTP y ZXing
feat(mfa): implementa puertos y adaptadores TOTP
feat(mfa): agrega configuración y confirmación de Google Authenticator
feat(auth): devuelve desafío MFA durante el inicio de sesión
feat(mfa): agrega verificación de desafíos y emisión del JWT
test(mfa): cubre activación y validación del segundo factor
feat(mfa): agrega configuración de Google Authenticator en Vue
feat(auth): agrega pantalla de verificación MFA al login
docs(mfa): documenta activación y prueba del segundo factor
```

No es obligatorio usar exactamente esta cantidad de commits. Cada commit debe compilar siempre que sea razonablemente posible y no debe mezclar cambios ajenos a su objetivo.

## 9. Ejemplos que deben evitarse

```text
cambios
arreglo login
avance del proyecto
feat: varias cosas
fix(auth): arreglado
actualización final
```

Alternativas correctas:

```text
feat(auth): agrega inicio de sesión con Google
fix(auth): evita duplicar usuarios durante el login con Google
test(auth): cubre el rechazo de tokens de Google vencidos
```
