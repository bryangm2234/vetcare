# ms-facturacion

Tarifas, facturas, pagos, abonos, caja y reportes financieros.

- **Historias principales:** HU-061 a HU-070. La especificación está en [HU-VetCare-SDD.md](../docs/specs/HU-VetCare-SDD.md).
- **Base de datos:** `vetcare_facturacion`
- **Puerto local:** 8085

## Requisitos

- JDK 17. Maven no hace falta, porque el proyecto incluye Maven Wrapper.
- MongoDB local o una cadena de conexión de MongoDB Atlas.

## Variables de entorno

| Variable | Obligatoria | Valor por defecto | Uso |
|---|---|---|---|
| `MONGODB_URI` | En la nube | `mongodb://localhost:27017` | Conexión a MongoDB, sin nombre de base de datos |
| `PORT` | No | `8085` | Puerto HTTP. Render lo define automáticamente |
| `JWT_PUBLIC_KEY` | Sí | Ninguno | Clave pública RSA en Base64 para validar los JWT |
| `PACIENTES_URL` | En la nube | `http://localhost:8081` | URL de ms-pacientes |
| `AGENDA_URL` | En la nube | `http://localhost:8082` | URL de ms-agenda |
| `HISTORIA_URL` | En la nube | `http://localhost:8083` | URL de ms-historia-clinica |
| `FARMACIA_URL` | En la nube | `http://localhost:8084` | URL de ms-farmacia |
| `VETCARE_TASA_IMPUESTO` | No | `19` | Tasa de impuesto en porcentaje |
| `VETCARE_CORREO_REMITENTE`, `SPRING_MAIL_*` | Para HU-070 | Ninguno | Remitente y servidor SMTP |

## Ejecutar en PowerShell

En local, el servicio lee las variables del archivo `.env` de la raíz del repositorio, que Git ignora. Agrega ahí `MONGODB_URI` si usas Atlas.

```powershell
java scripts/GenerarClavesJwt.java   # desde la raíz, solo la primera vez
cd ms-facturacion
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

### Facturación y caja

| Ruta | Rol | Historia |
|---|---|---|
| `POST /api/tarifas`, `PUT /api/tarifas/{id}` | Administrador | HU-062 |
| `GET /api/tarifas` | Cajero, administrador | HU-062 |
| `POST /api/facturas` | Cajero | HU-061 |
| `GET /api/facturas/{id}` | Cajero, administrador | HU-061 |
| `PATCH /api/facturas/{id}/ajustes` | Cajero | HU-063 |
| `POST /api/facturas/{id}/pagos` | Cajero | HU-064 |
| `PATCH /api/facturas/{id}/anulacion` | Cajero | HU-065 |
| `POST /api/cajas/apertura`, `PATCH /api/cajas/{id}/cierre` | Cajero | HU-066 |
| `GET /api/portal/facturas`, `GET /api/portal/facturas/{id}/pdf` | Propietario | HU-067 |
| `POST /api/facturas/{id}/abonos` | Cajero | HU-068 |
| `GET /api/reportes/ingresos?desde=&hasta=&servicio=&medioPago=` | Administrador | HU-069 |
| `POST /api/facturas/{id}/envio-correo` | Cajero | HU-070 |

Generar o anular una factura necesita ms-pacientes, ms-agenda, ms-historia-clinica y ms-farmacia en ejecución.

## Errores comunes

| Síntoma | Causa | Solución |
|---|---|---|
| 422 "No hay tarifa vigente" | El servicio no tiene tarifa | Créala con `POST /api/tarifas` |
| 422 "Debe abrir la caja" | El cajero no tiene caja abierta | Usa `POST /api/cajas/apertura` |
| 503 al facturar | Un microservicio no responde | Arráncalo o revisa su variable de URL |
| 502 al enviar la factura | Falta configurar el correo o falló el servidor | Revisa `SPRING_MAIL_*` |

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
