# ms-agenda

Horarios y cupos, citas, disponibilidad, llegada de pacientes y peluquería.

- **Historias principales:** HU-021 a HU-030 y HU-073. La especificación está en [HU-VetCare-SDD.md](../docs/specs/HU-VetCare-SDD.md).
- **Base de datos:** `vetcare_agenda`
- **Puerto local:** 8082

## Requisitos

- JDK 17. Maven no hace falta, porque el proyecto incluye Maven Wrapper.
- MongoDB local o una cadena de conexión de MongoDB Atlas.

## Variables de entorno

| Variable | Obligatoria | Valor por defecto | Uso |
|---|---|---|---|
| `MONGODB_URI` | En la nube | `mongodb://localhost:27017` | Conexión a MongoDB, sin nombre de base de datos |
| `PORT` | No | `8082` | Puerto HTTP. Render lo define automáticamente |
| `JWT_PUBLIC_KEY` | Sí | Ninguno | Clave pública RSA en Base64 para validar los JWT |
| `PACIENTES_URL` | En la nube | `http://localhost:8081` | URL de ms-pacientes |
| `VETCARE_CLAVE_PROCESO` | Para HU-030 | Ninguno | Clave de la ejecución diaria de recordatorios, enviada en `X-Clave-Proceso` |
| `VETCARE_CORREO_REMITENTE` | Para HU-030 | Ninguno | Remitente de los correos |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Para HU-030 | Ninguno | Servidor SMTP. Sin él, los recordatorios quedan pendientes |

## Ejecutar en PowerShell

En local, el servicio lee las variables del archivo `.env` de la raíz del repositorio, que Git ignora. Agrega ahí `MONGODB_URI` si usas Atlas.

```powershell
java scripts/GenerarClavesJwt.java   # desde la raíz, solo la primera vez
cd ms-agenda
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

### Agenda, citas y peluquería

| Ruta | Rol | Historia |
|---|---|---|
| `PUT /api/horarios/{veterinarioId}` | Administrador | HU-021 |
| `GET /api/disponibilidad?fecha=&veterinarioId=` | Recepcionista, administrador, propietario, peluquería | HU-023 |
| `POST /api/citas` | Recepcionista | HU-022 |
| `GET /api/citas/{id}` | Recepcionista, veterinario, auxiliar, administrador | HU-022 |
| `PUT /api/citas/{id}/reprogramacion` | Recepcionista | HU-025 |
| `PATCH /api/citas/{id}/cancelacion` | Recepcionista | HU-025 |
| `PATCH /api/citas/{id}/llegada` | Recepcionista | HU-026 |
| `GET /api/citas/mi-agenda?fecha=` | Veterinario | HU-027 |
| `PATCH /api/citas/{id}/atencion` | Veterinario | HU-035 |
| `POST /api/portal/citas` | Propietario | HU-024 |
| `GET /api/portal/citas` | Propietario | HU-071 |
| `GET /api/reportes/citas?desde=&hasta=` | Administrador | HU-072 |
| `GET /api/reportes/ocupacion?veterinarioId=&desde=&hasta=` | Administrador | HU-073 |
| `PATCH /api/peluqueria/items-facturados` | Cajero, administrador | HU-061 |
| `PUT /api/peluqueria/horario` | Administrador | HU-028 |
| `POST /api/peluqueria/citas` | Peluquería | HU-028 |
| `PATCH /api/peluqueria/citas/{id}/servicio` | Peluquería | HU-029 |
| `GET /api/peluqueria/items-pendientes` | Cajero, administrador | HU-029 |
| `POST /api/recordatorios/ejecucion` | Clave `X-Clave-Proceso` | HU-030 |

`veterinarioId` es el `id` del usuario veterinario en ms-pacientes. Este servicio necesita ms-pacientes en ejecución para agendar.

## Errores comunes

| Síntoma | Causa | Solución |
|---|---|---|
| 503 al agendar | ms-pacientes no responde | Arráncalo o revisa `PACIENTES_URL` |
| 404 al agendar con una mascota válida | El veterinario no tiene horario | Configúralo con `PUT /api/horarios/{veterinarioId}` |
| 422 "fuera del horario" | La hora no coincide con el inicio de una franja | Consulta las franjas libres en `/api/disponibilidad` |
| Recordatorios `fallidos` | No hay SMTP configurado | Define las variables `SPRING_MAIL_*` |

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
