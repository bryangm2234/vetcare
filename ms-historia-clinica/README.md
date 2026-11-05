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

## Ejecutar en PowerShell

```powershell
cd ms-historia-clinica
$env:MONGODB_URI = "mongodb://localhost:27017"
.\mvnw.cmd spring-boot:run
```

## Probar

Las pruebas no necesitan MongoDB en ejecución.

```powershell
.\mvnw.cmd verify
```

## Endpoints disponibles

| Ruta | Acceso | Respuesta |
|---|---|---|
| `GET /actuator/health` | Público | 200 si MongoDB responde; 503 si no |
| `GET /swagger-ui.html` | Público | Documentación interactiva |
| `GET /v3/api-docs` | Público | Contrato OpenAPI en JSON |

Las rutas de negocio se agregan en la Etapa 3. Hasta que se implemente HU-003, cualquier otra ruta se rechaza.

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
