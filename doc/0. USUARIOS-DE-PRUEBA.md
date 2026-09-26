# Usuarios de prueba — Login local + Google (Arquitectura Clean)

**Fecha:** 2026-09-07
**Backend:** `http://localhost:8083` (Docker) · **Frontend:** `http://localhost:5173` (Vite dev)

---

## 1. Usuarios para probar

| Correo / usuario | Contraseña | Rol | Login con contraseña | Login con Google | Notas |
|---|---|---|---|---|---|
| `admin` | `Admin123*` | `ADMIN` | ✅ | — (no tiene correo, es la cuenta demo original) | Seedeada automáticamente por `MongoDemoDataInitializer` al levantar el backend. |
| `admin2` | `admin2` | `ADMIN` | ✅ | — (sin correo, sin `googleSubject`) | Cuenta adicional creada vía `POST /api/auth/register` para pruebas de staff sin usar la cuenta demo original. Sin MFA configurado (`mfaHabilitado=false`). |
| `ramirezlisset361@gmail.com` | `Cliente123*` | `CLIENTE` | ✅ | ✅ (ya vinculada, `googleSubject` guardado) | Al entrar ve solo **"Mi cuenta"** — su póliza `POL-CLI-TEST-001` (vigente) y su renovación pendiente. Sin acceso a clientes/cotizaciones de otros. |
| `shanira.2.rc@gmail.com` | `Agente123*` | `AGENTE` | ✅ | ✅ (se vincula sola en su **primer** login con Google) | Acceso de staff: Resumen, Clientes y vehículos, Cotizaciones, Pólizas y siniestros, Renovaciones. |

Ambas cuentas nuevas soportan **contraseña y Google indistintamente para la misma cuenta** (igual que Spotify): iniciar sesión por cualquiera de las dos vías funciona y no se bloquean entre sí.

---

## 2. Dónde consultar esta información en la base de datos

Backend Clean usa **MongoDB**, contenedor `andina-clean-mongodb`, base de datos `andina_seguros_clean`.

Comando base para entrar a una consulta interactiva:

```bash
docker exec -it andina-clean-mongodb mongosh andina_seguros_clean
```

O para correr una consulta puntual sin entrar en modo interactivo:

```bash
docker exec andina-clean-mongodb mongosh andina_seguros_clean --quiet --eval "db.usuarios.find({}, {passwordHash:0}).toArray()"
```

### Colección `usuarios` — credenciales, rol, vínculo con Google

Guarda **quién puede loguearse** y con qué rol. Es la colección que valida `/api/auth/login` y `/api/auth/google`.

```js
db.usuarios.find({}, { passwordHash: 0 })
```

Campos relevantes: `username` (= email para cuentas Google), `email`, `googleSubject` (identificador estable de Google, `null` hasta el primer login exitoso con Google), `rol` (`ADMIN`/`ACTUARIO`/`AGENTE`/`CLIENTE`), `activo`. El campo `passwordHash` existe pero se omite arriba porque es un hash BCrypt, no la contraseña en texto plano (esa nunca se guarda).

### Colección `clientes` — datos del cliente como asegurado (no como usuario del sistema)

Es una entidad de **negocio**, separada de `usuarios`. Solo si un correo aparece aquí, un login nuevo de Google con ese correo puede crear una cuenta `CLIENTE` (regla `CLIENTE_NO_REGISTRADO`), y es lo que consulta el endpoint `GET /api/mi-cuenta` para saber quién es el cliente detrás de la sesión (por coincidencia de `correo`).

```js
db.clientes.find({ correo: "ramirezlisset361@gmail.com" })
```

### Colección `vehiculos` — vehículos del cliente

```js
db.vehiculos.find({ clienteId: "99000000-0000-0000-0000-000000000001" })
```

### Colección `polizas` — pólizas emitidas

La póliza de prueba de `ramirezlisset361@gmail.com`:

```js
db.polizas.find({ clienteId: "99000000-0000-0000-0000-000000000001" })
```

### Colección `propuestas_renovacion` — renovaciones (pendientes/aceptadas/rechazadas)

```js
db.propuestas_renovacion.find({ polizaOrigenId: "99000000-0000-0000-0000-000000000004" })
```

---

## 3. IDs de los datos de prueba insertados (para referencia rápida)

| Entidad | ID | Detalle |
|---|---|---|
| Cliente | `99000000-0000-0000-0000-000000000001` | Lisset Ramirez · DNI 87654321 · correo `ramirezlisset361@gmail.com` |
| Vehículo | `99000000-0000-0000-0000-000000000002` | Toyota Yaris 2022, placa `ABC-999` |
| Póliza | `99000000-0000-0000-0000-000000000004` | `POL-CLI-TEST-001`, VIGENTE, prima 1200.00 PEN |
| Renovación | `99000000-0000-0000-0000-000000000005` | PENDIENTE, nueva prima 1260.00 PEN (variación 5%) |

---

## 4. Endpoints útiles para verificar sin usar el navegador

```bash
# Login local
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"ramirezlisset361@gmail.com","password":"Cliente123*"}'

# Mi cuenta (usar el token del login anterior)
curl -X GET http://localhost:8083/api/mi-cuenta \
  -H "Authorization: Bearer <TOKEN>"

# Login como admin2 (staff, sin MFA)
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin2","password":"admin2"}'
```
