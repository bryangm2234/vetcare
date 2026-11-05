# ms-pacientes

Usuarios e inicio de sesión (D2), propietarios, mascotas, alergias y estado.

- **Historias principales:** HU-001 a HU-006, HU-009, HU-010 y HU-011 a HU-020. La especificación está en [HU-VetCare-SDD.md](../docs/specs/HU-VetCare-SDD.md).
- **Base de datos:** `vetcare_pacientes`
- **Puerto local:** 8081

## Requisitos

- JDK 17. Maven no hace falta, porque el proyecto incluye Maven Wrapper.
- MongoDB local o una cadena de conexión de MongoDB Atlas.

## Variables de entorno

| Variable | Obligatoria | Valor por defecto | Uso |
|---|---|---|---|
| `MONGODB_URI` | En la nube | `mongodb://localhost:27017` | Conexión a MongoDB, sin nombre de base de datos |
| `PORT` | No | `8081` | Puerto HTTP. Render lo define automáticamente |
| `JWT_PUBLIC_KEY` | Sí | Ninguno | Clave pública RSA en Base64 para validar los JWT |
| `JWT_PRIVATE_KEY` | Sí | Ninguno | Clave privada RSA en Base64 para firmar los JWT. Solo la usa este servicio |
| `VETCARE_ADMIN_CORREO` | Para crear el primer administrador | Ninguno | Correo del administrador inicial |
| `VETCARE_ADMIN_CONTRASENA` | Para crear el primer administrador | Ninguno | Contraseña del administrador inicial |

## Ejecutar en PowerShell

En local, el servicio lee las variables del archivo `.env` de la raíz del repositorio, que Git ignora. Agrega ahí `MONGODB_URI` si usas Atlas.

```powershell
java scripts/GenerarClavesJwt.java   # desde la raíz, solo la primera vez
cd ms-pacientes
.\mvnw.cmd spring-boot:run
```

## Probar

Las pruebas unitarias no necesitan MongoDB. Las de integración solo corren si defines `MONGODB_TEST_URI`, y usan una base de datos de prueba aparte.

```powershell
.\mvnw.cmd verify
```

## Endpoints disponibles

| Ruta | Acceso | Respuesta |
|---|---|---|
| `GET /actuator/health` | Público | 200 si MongoDB responde; 503 si no |
| `GET /swagger-ui.html` | Público | Documentación interactiva |
| `GET /v3/api-docs` | Público | Contrato OpenAPI en JSON |
| `GET /api/auditoria` | Administrador | Auditoría del servicio (HU-009) |

Las demás rutas exigen `Authorization: Bearer <token>`: responden 401 sin un token válido y 403 si el rol no está autorizado (HU-003). Los errores siguen el formato estándar de HU-079.

### Autenticación y usuarios

| Ruta | Rol | Historia |
|---|---|---|
| `POST /api/auth/login` | Público | HU-002 |
| `POST /api/usuarios` | Administrador | HU-001 |
| `GET /api/usuarios?rol=&estado=&page=&size=` | Administrador | HU-006 |
| `PATCH /api/usuarios/{id}/desactivar` | Administrador | HU-004 |
| `PUT /api/usuarios/me/contrasena` | Cualquier usuario autenticado | HU-005 |

### Propietarios y mascotas

| Ruta | Rol | Historia |
|---|---|---|
| `POST /api/propietarios` | Recepcionista | HU-011 |
| `GET /api/propietarios?documento=&nombre=&page=&size=` | Personal de atención y administrador | HU-013 |
| `GET /api/propietarios/{id}` | Personal de atención y administrador | HU-013 |
| `PATCH /api/propietarios/{id}/contacto` | Recepcionista | HU-014 |
| `GET /api/propietarios/{id}/mascotas` | Personal de atención y administrador | HU-015 |
| `POST /api/mascotas` | Recepcionista | HU-012 |
| `GET /api/mascotas/{id}` | Todo el personal; el propietario solo las suyas | HU-012, HU-016 |
| `PUT /api/mascotas/{id}/antecedentes` | Veterinario | HU-016 |
| `PATCH /api/mascotas/{id}/estado` | Recepcionista | HU-017 |
| `POST /api/mascotas/{id}/transferencia` | Recepcionista | HU-018 |
| `GET /api/reportes/mascotas` | Administrador | HU-019 |

## Primer uso con PowerShell

El primer administrador se crea al arrancar, con `VETCARE_ADMIN_CORREO` y `VETCARE_ADMIN_CONTRASENA`. Con `GenerarClavesJwt.java` esos valores quedan en el archivo `.env` local.

```powershell
$cuerpo = @{ correo = "admin@vetcare.local"; contrasena = "<contraseña del archivo .env>" } | ConvertTo-Json
$sesion = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/login -ContentType "application/json" -Body $cuerpo
Invoke-RestMethod -Uri http://localhost:8081/api/usuarios -Headers @{ Authorization = "Bearer $($sesion.token)" }
```

En Swagger UI (`/swagger-ui.html`) ejecuta el inicio de sesión, copia `token` y pégalo en **Authorize**.

Respuesta de `POST /api/auth/login`:

```json
{ "token": "eyJhbGciOiJSUzI1NiJ9...", "tipo": "Bearer", "expiraEn": "2026-11-05T15:30:00Z", "rol": "ADMINISTRADOR" }
```

Respuesta de error, igual en toda la API:

```json
{ "timestamp": "2026-11-05T15:00:00Z", "status": 409, "error": "Conflict", "mensaje": "El documento ya está registrado", "ruta": "/api/propietarios", "detalles": {} }
```

## Errores comunes

| Síntoma | Causa | Solución |
|---|---|---|
| `Falta la variable de entorno JWT_PUBLIC_KEY` al arrancar | No existe `.env` ni la variable | Ejecuta `java scripts/GenerarClavesJwt.java` desde la raíz |
| `/actuator/health` responde 503 | MongoDB no está disponible | Revisa `MONGODB_URI` y, en Atlas, la lista de IP permitidas |
| El inicio de sesión responde 401 con el administrador | Ya existía un administrador con otra contraseña | La contraseña de `.env` solo se usa al crear el primero |

Ejemplo de respuesta de `/actuator/health`:

```json
{ "groups": ["liveness", "readiness"], "status": "UP" }
```

## Estructura

| Paquete | Responsabilidad |
|---|---|
| `controller` | Controladores REST |
| `service` | Lógica de negocio y reglas |
| `repository` | Acceso a MongoDB |
| `model` | Documentos |
| `dto` | Entrada y salida de la API |
| `config` | Configuración, incluida OpenAPI |
| `security` | Seguridad y JWT |
| `exception` | Excepciones y manejo global |
