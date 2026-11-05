# ms-farmacia

Medicamentos, lotes, despachos, kardex, proveedores y órdenes de compra.

- **Historias principales:** HU-051 a HU-060. La especificación está en [HU-VetCare-SDD.md](../docs/specs/HU-VetCare-SDD.md).
- **Base de datos:** `vetcare_farmacia`
- **Puerto local:** 8084

## Requisitos

- JDK 17. Maven no hace falta, porque el proyecto incluye Maven Wrapper.
- MongoDB local o una cadena de conexión de MongoDB Atlas.

## Variables de entorno

| Variable | Obligatoria | Valor por defecto | Uso |
|---|---|---|---|
| `MONGODB_URI` | En la nube | `mongodb://localhost:27017` | Conexión a MongoDB, sin nombre de base de datos |
| `PORT` | No | `8084` | Puerto HTTP. Render lo define automáticamente |
| `JWT_PUBLIC_KEY` | Sí | Ninguno | Clave pública RSA en Base64 para validar los JWT |
| `HISTORIA_URL` | En la nube | `http://localhost:8083` | URL de ms-historia-clinica |

## Ejecutar en PowerShell

En local, el servicio lee las variables del archivo `.env` de la raíz del repositorio, que Git ignora. Agrega ahí `MONGODB_URI` si usas Atlas.

```powershell
java scripts/GenerarClavesJwt.java   # desde la raíz, solo la primera vez
cd ms-farmacia
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

### Inventario, despachos y proveedores

| Ruta | Rol | Historia |
|---|---|---|
| `POST /api/medicamentos` | Farmacia | HU-051 |
| `GET /api/medicamentos?texto=&page=&size=` | Farmacia, veterinario, administrador | HU-051 |
| `GET /api/medicamentos/{id}` | Farmacia, veterinario, administrador | HU-051 |
| `POST /api/medicamentos/{id}/lotes` | Farmacia | HU-052 |
| `POST /api/despachos` | Farmacia | HU-053 |
| `GET /api/despachos/items-pendientes` | Cajero, administrador | HU-053 |
| `GET /api/alertas/stock-minimo` | Farmacia, administrador | HU-054 |
| `GET /api/medicamentos/{id}/kardex?desde=&hasta=` | Farmacia, administrador | HU-055 |
| `POST /api/medicamentos/{id}/ajustes` | Farmacia | HU-055 |
| `GET /api/lotes/proximos-vencer?dias=60` | Farmacia, administrador | HU-056 |
| `POST /api/lotes/{numero}/consumos` | Veterinario | HU-037 |
| `POST /api/proveedores` | Administrador | HU-057 |
| `GET /api/proveedores` | Farmacia, administrador | HU-057 |
| `POST /api/ordenes-compra` | Farmacia | HU-058 |
| `GET /api/ordenes-compra` | Farmacia, administrador | HU-058 |
| `GET /api/proveedor/ordenes-compra` | Proveedor | HU-059 |
| `PATCH /api/proveedor/ordenes-compra/{id}/confirmacion` | Proveedor | HU-059 |
| `POST /api/ordenes-compra/{id}/recepciones` | Farmacia | HU-060 |

Para despachar, este servicio necesita ms-historia-clinica en ejecución.

## Reglas de negocio

- **RN-02:** solo se despacha una fórmula vigente, sin superar lo pendiente. Si falla la actualización de la fórmula, el descuento se revierte y queda en el kardex.
- **RN-04:** cada movimiento queda en el kardex por lote, el stock nunca queda negativo, sale primero el lote que vence antes y los lotes vencidos no se despachan.

## Errores comunes

| Síntoma | Causa | Solución |
|---|---|---|
| 503 al despachar | ms-historia-clinica no responde | Arráncalo o revisa `HISTORIA_URL` |
| 422 "No se puede despachar sin fórmula vigente" | Fórmula vencida, anulada o ya despachada | Consulta su estado en `GET /api/formulas/{id}/vigencia` |
| 409 "Stock insuficiente" con stock aparente | Parte del stock está en lotes vencidos | Revisa los lotes en el kardex |

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
