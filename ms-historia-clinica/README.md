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
