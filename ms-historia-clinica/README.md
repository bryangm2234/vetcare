# ms-historia-clinica

Consultas, signos vitales, vacunación, laboratorio, fórmulas y hospitalización.

- **Historias principales:** HU-031 a HU-050. La especificación está en [HU-VetCare-SDD.md](../docs/specs/HU-VetCare-SDD.md).
- **Base de datos:** `vetcare_historia`
- **Puerto local:** 8083

## Requisitos

- JDK 17. Maven no hace falta, porque el proyecto incluye Maven Wrapper.
- MongoDB local o una cadena de conexión de MongoDB Atlas.

## Variables de entorno

| Variable | Obligatoria | Valor por defecto | Uso |
|---|---|---|---|
| `MONGODB_URI` | En la nube | `mongodb://localhost:27017` | Conexión a MongoDB, sin nombre de base de datos |
| `PORT` | No | `8083` | Puerto HTTP. Render lo define automáticamente |
| `JWT_PUBLIC_KEY` | Sí | Ninguno | Clave pública RSA en Base64 para validar los JWT |
| `PACIENTES_URL` | En la nube | `http://localhost:8081` | URL de ms-pacientes |
| `AGENDA_URL` | En la nube | `http://localhost:8082` | URL de ms-agenda |
| `FARMACIA_URL` | Desde el Sprint 3 | Ninguno | URL de ms-farmacia. Sin ella, la vacuna queda con inventario sin descontar |

## Ejecutar en PowerShell

En local, el servicio lee las variables del archivo `.env` de la raíz del repositorio, que Git ignora. Agrega ahí `MONGODB_URI` si usas Atlas.

```powershell
java scripts/GenerarClavesJwt.java   # desde la raíz, solo la primera vez
cd ms-historia-clinica
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

### Consultas y vacunación

| Ruta | Rol | Historia |
|---|---|---|
| `POST /api/consultas` | Veterinario | HU-031 |
| `GET /api/consultas/{id}` | Veterinario | HU-031 |
| `PUT /api/citas/{citaId}/signos-vitales` | Auxiliar veterinario | HU-032 |
| `PUT /api/consultas/{id}/registro-clinico` | Veterinario | HU-033 |
| `GET /api/historias/{mascotaId}` | Veterinario | HU-034 |
| `PATCH /api/consultas/{id}/cierre` | Veterinario | HU-035 |
| `GET /api/consultas/items-pendientes` | Cajero, administrador | HU-035 |
| `POST /api/esquemas-vacunacion` | Veterinario | HU-036 |
| `GET /api/esquemas-vacunacion?especie=` | Veterinario, recepcionista, auxiliar | HU-036 |
| `POST /api/vacunas` | Veterinario | HU-037 |
| `GET /api/vacunas/alertas?dias=30` | Recepcionista | HU-038 |
| `GET /api/portal/mascotas/{mascotaId}/carne` | Propietario | HU-039 |
| `POST /api/historias/{mascotaId}/adjuntos` | Veterinario | HU-040 |
| `GET /api/adjuntos/{id}` | Veterinario | HU-040 |

Este servicio necesita ms-pacientes y ms-agenda en ejecución. Laboratorio, fórmulas y hospitalización llegan en el Sprint 3.

## Errores comunes

| Síntoma | Causa | Solución |
|---|---|---|
| 503 al abrir o cerrar una consulta | ms-agenda no responde | Arráncalo o revisa `AGENDA_URL` |
| 422 "La cita no está en espera" | Falta registrar la llegada | Usa `PATCH /api/citas/{id}/llegada` en ms-agenda |
| 422 al registrar una vacuna | La vacuna no tiene esquema para la especie | Créalo con `POST /api/esquemas-vacunacion` |
| 413 al adjuntar | El archivo supera los 5 MB | Reduce el archivo |

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
