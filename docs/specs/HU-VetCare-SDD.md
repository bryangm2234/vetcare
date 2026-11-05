# VetCare — Especificación de historias de usuario (SDD)

**Estado:** borrador pendiente de aprobación del Product Owner.

Este documento es la fuente de verdad del proyecto: si cambia un requerimiento, se actualiza primero esta especificación y después el código y las pruebas.

**Origen:** el texto, la prioridad, los puntos y los responsables de cada historia provienen del informe del proyecto y coinciden con la Lista de Producto. Los endpoints y los escenarios, salvo los cuatro primeros de HU-053, se redactaron a partir de esos documentos y de las decisiones de la sección 3, porque el informe solo especifica HU-053 en detalle.

## 1. Convenciones

| Código | Uso |
|---|---|
| 200 / 201 | Operación correcta / recurso creado |
| 400 | Datos inválidos |
| 401 / 403 | Sin autenticación válida / rol o dueño no autorizado |
| 404 | Recurso inexistente |
| 409 | Conflicto de estado: duplicado, franja ocupada, sin cupo o stock insuficiente |
| 413 / 415 | Archivo demasiado grande / tipo de archivo no permitido |
| 422 | Regla de negocio incumplida |
| 502 / 503 | Falla de un servicio externo / microservicio dependiente no disponible |

Además de los escenarios de cada historia, todo endpoint protegido responde 401 sin un token válido y 403 con un rol no autorizado (HU-003).

Las rutas `/api/portal/...` y `/api/proveedor/...` filtran por el `propietarioId` o el `proveedorId` del token.

## 2. Microservicios y bases de datos

| Microservicio | Base de datos | Puerto local |
|---|---|---|
| ms-pacientes | vetcare_pacientes | 8081 |
| ms-agenda | vetcare_agenda | 8082 |
| ms-historia-clinica | vetcare_historia | 8083 |
| ms-farmacia | vetcare_farmacia | 8084 |
| ms-facturacion | vetcare_facturacion | 8085 |

## 3. Decisiones aprobadas

| # | Decisión |
|---|---|
| D1 | Esta especificación se redacta a partir del informe y la Lista de Producto, y queda sujeta a la aprobación del Product Owner. |
| D2 | Usuarios e inicio de sesión viven en `ms-pacientes`, en colecciones separadas. Solo ese servicio firma JWT con RS256; los demás verifican con la clave pública. |
| D3 | HU-001 cubre los 10 roles. Las cuentas de propietario y proveedor se vinculan con `propietarioId` o `proveedorId`. El primer administrador se crea al arrancar `ms-pacientes`, desde variables de entorno. |
| D4 | El token dura 30 minutos y el cierre de sesión ocurre en el cliente. Un usuario desactivado conserva el acceso hasta que expire su token. |
| D5 | Frontend en React + Vite + TypeScript, en `frontend/`, publicado como sitio estático en Render. Las llamadas van directo a cada API, con CORS. |
| D6 | CI, errores globales y despliegue mínimo se montan desde el Sprint 1. HU-074 a HU-079 conservan su identificador y se cierran formalmente en el Sprint 4. |
| D7 | Una colección por entidad, vinculada por `mascotaId`. HU-034 compone la vista completa. |
| D8 | El veterinario puede anular una fórmula con motivo, dentro de HU-045. |
| D9 | Los ajustes de inventario, con motivo y auditoría, se incluyen en HU-055. |
| D10 | La fórmula admite varios ítems con cantidad pendiente propia. Estados: `VIGENTE`, `PARCIAL`, `DESPACHADA`, `VENCIDA` y `ANULADA`. |
| D11 | Una vacuna aplicada descuenta su lote en `ms-farmacia`. En hospitalización solo se administra lo ya formulado y despachado. Hasta el Sprint 3, sin `FARMACIA_URL`, la dosis queda marcada con inventario sin descontar. |
| D12 | Correo por SMTP con credenciales en variables de entorno. HU-030 se dispara con una ejecución diaria externa, como GitHub Actions, autenticada con la clave `VETCARE_CLAVE_PROCESO`. |
| D13 | Adjuntos en GridFS sobre Atlas: PDF e imágenes de hasta 5 MB. |
| D14 | Plan gratuito de Render en desarrollo, con reintentos entre servicios. Valorar un plan de pago para la entrega. |
| D15 | Bryan Garcia verifica la Definición de Hecho y Julian Barney Jaimes, como Product Owner, acepta las historias. |
| D16 | Se conservan las 80 historias, se avanza primero la prioridad Alta y la Review del Sprint 2 se mueve porque el 16 de noviembre es festivo. La entrega final es el 1 de diciembre de 2026. |

**Valores por defecto:**
- Alertas de vacunas a 30 días y de lotes a 60 días.
- Franjas de 30 minutos, configurables por veterinario.
- Las citas del portal quedan confirmadas si hay cupo.
- El administrador configura los cupos de peluquería.
- Los lotes vencidos nunca se despachan.
- La tasa de impuesto es configurable.
- La facturación electrónica ante la DIAN queda fuera del alcance.

## 4. Trazabilidad de las reglas de negocio

| Regla | Historias |
|---|---|
| RN-01 · Esquema de vacunación con fechas | HU-036, HU-037, HU-038, HU-039, HU-071, HU-077, HU-080 |
| RN-02 · No despachar sin fórmula vigente | HU-045, HU-046, HU-053, HU-077, HU-080 |
| RN-03 · Cupos por veterinario | HU-017, HU-021, HU-022, HU-023, HU-024, HU-025, HU-028, HU-073, HU-077, HU-080 |
| RN-04 · Control de inventario de medicamentos | HU-051, HU-052, HU-053, HU-054, HU-055, HU-056, HU-058, HU-060, HU-077, HU-080, HU-037 (por D11) |

## 5. Historias de usuario

### Sprint 1 · 3 nov 2026 – 9 nov 2026

#### Épica 1: Seguridad, usuarios y roles

##### HU-001 — Crear usuarios del personal con un rol asignado

Como administrador, quiero crear usuarios del personal con un rol asignado, para que cada empleado acceda solo a las funciones de su área.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 16 |
| Microservicio | ms-pacientes |
| Endpoint | `POST /api/usuarios` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Solo el administrador crea usuarios de cualquiera de los 10 roles; la contraseña se guarda cifrada y el correo es único (D2, D3). |

```gherkin
# language: es
Característica: HU-001 crear usuarios del personal con un rol asignado
  Escenario: Creación válida
    Dado que soy administrador
    Cuando creo un usuario con correo nuevo y rol "CAJERO"
    Entonces el sistema responde 201, guarda la contraseña cifrada con BCrypt y no la devuelve en la respuesta
  Escenario: Correo duplicado
    Dado que ya existe un usuario con ese correo
    Cuando intento crearlo
    Entonces el sistema responde 409 "El correo ya está registrado"
  Escenario: Rol inexistente
    Dado que soy administrador
    Cuando envío un rol que no es uno de los 10 definidos
    Entonces el sistema responde 400
  Escenario: Cuenta de propietario sin vínculo
    Dado que soy administrador
    Cuando creo un usuario con rol "PROPIETARIO" sin propietarioId
    Entonces el sistema responde 422 "El usuario debe estar vinculado a un propietario"
  Escenario: Usuario sin permiso
    Dado que tengo un rol distinto de administrador
    Cuando intento crear un usuario
    Entonces el sistema responde 403
  Escenario: Administrador inicial
    Dado que no existe ningún administrador y están configuradas VETCARE_ADMIN_CORREO y VETCARE_ADMIN_CONTRASENA
    Cuando ms-pacientes arranca
    Entonces se crea un administrador activo con esos datos
  Escenario: Administrador ya existente
    Dado que ya existe un administrador
    Cuando ms-pacientes arranca
    Entonces no se crea ni se modifica ningún usuario
```

##### HU-002 — Iniciar sesión con mi correo y contraseña

Como usuario del sistema, quiero iniciar sesión con mi correo y contraseña, para que obtenga un token que me permita usar el sistema según mi rol.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 16 |
| Microservicio | ms-pacientes |
| Endpoint | `POST /api/auth/login` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Con credenciales válidas de un usuario activo se obtiene un JWT firmado con RS256, con el rol y una expiración de 30 minutos (D2, D4). |

```gherkin
# language: es
Característica: HU-002 iniciar sesión con mi correo y contraseña
  Escenario: Inicio de sesión válido
    Dado que existe un usuario activo con ese correo y contraseña
    Cuando inicio sesión
    Entonces el sistema responde 200 con un token, su expiración a 30 minutos y el rol del usuario
  Escenario: Credenciales incorrectas
    Dado que la contraseña no corresponde al correo
    Cuando intento iniciar sesión
    Entonces el sistema responde 401 "Correo o contraseña incorrectos" sin indicar cuál de los dos falló
  Escenario: Usuario desactivado
    Dado que el usuario está desactivado
    Cuando intenta iniciar sesión con su contraseña correcta
    Entonces el sistema responde 401
  Escenario: Token expirado
    Dado que mi token expiró
    Cuando llamo a cualquier endpoint protegido
    Entonces el sistema responde 401
  Escenario: Cierre de sesión
    Dado que tengo una sesión abierta en la interfaz web
    Cuando cierro la sesión
    Entonces la interfaz elimina el token y me devuelve a la pantalla de inicio de sesión
```

##### HU-003 — Restringir cada endpoint según el rol del usuario

Como administrador, quiero restringir cada endpoint según el rol del usuario, para que se eviten accesos no autorizados a información clínica y financiera.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 24 |
| Microservicio | Todos los microservicios |
| Endpoint | `Todos los endpoints protegidos` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Cada microservicio valida la firma del JWT con la clave pública y autoriza cada endpoint según el rol. |

```gherkin
# language: es
Característica: HU-003 restringir cada endpoint según el rol del usuario
  Escenario: Rol autorizado
    Dado que tengo un token válido de un rol autorizado
    Cuando llamo al endpoint
    Entonces el sistema procesa la petición
  Escenario: Rol no autorizado
    Dado que tengo un token válido de un rol no autorizado
    Cuando llamo al endpoint
    Entonces el sistema responde 403
  Escenario: Sin token
    Dado que no envío token
    Cuando llamo a un endpoint protegido
    Entonces el sistema responde 401
  Escenario: Token con firma inválida
    Dado que el token fue firmado con otra clave
    Cuando llamo a un endpoint protegido
    Entonces el sistema responde 401
  Escenario: Propietario accede a datos ajenos
    Dado que soy propietario
    Cuando consulto una mascota de otro propietario
    Entonces el sistema responde 403
```

##### HU-004 — Desactivar usuarios del personal

Como administrador, quiero desactivar usuarios del personal, para que quien ya no trabaja en la clínica pierda el acceso.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `PATCH /api/usuarios/{id}/desactivar` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El usuario desactivado no puede volver a iniciar sesión; un token ya emitido deja de servir al expirar, en máximo 30 minutos (D4). |

```gherkin
# language: es
Característica: HU-004 desactivar usuarios del personal
  Escenario: Desactivación válida
    Dado que soy administrador y el usuario está activo
    Cuando lo desactivo
    Entonces el sistema responde 200, el usuario queda INACTIVO y la operación se audita
  Escenario: Inicio de sesión posterior
    Dado que el usuario fue desactivado
    Cuando intenta iniciar sesión
    Entonces el sistema responde 401
  Escenario: Autodesactivación
    Dado que soy administrador
    Cuando intento desactivar mi propio usuario
    Entonces el sistema responde 422 "No puede desactivar su propio usuario"
  Escenario: Usuario inexistente
    Dado que el usuario no existe
    Cuando intento desactivarlo
    Entonces el sistema responde 404
```

##### HU-005 — Cambiar mi contraseña

Como usuario del sistema, quiero cambiar mi contraseña, para que mantenga mi cuenta segura.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Media · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `PUT /api/usuarios/me/contrasena` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El usuario cambia su propia contraseña indicando la actual; la nueva tiene mínimo 8 caracteres y se guarda cifrada. |

```gherkin
# language: es
Característica: HU-005 cambiar mi contraseña
  Escenario: Cambio válido
    Dado que indico mi contraseña actual correcta y una nueva de 8 caracteres o más
    Cuando la cambio
    Entonces el sistema responde 200 y solo la nueva contraseña permite iniciar sesión
  Escenario: Contraseña actual incorrecta
    Dado que la contraseña actual no es correcta
    Cuando intento cambiarla
    Entonces el sistema responde 422 y conserva la contraseña anterior
  Escenario: Contraseña nueva corta
    Dado que la contraseña nueva tiene menos de 8 caracteres
    Cuando intento cambiarla
    Entonces el sistema responde 400
```

##### HU-006 — Consultar los usuarios filtrando por rol y estado

Como administrador, quiero consultar los usuarios filtrando por rol y estado, para que controle el personal de cada área.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Media · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `GET /api/usuarios?rol=&estado=&page=&size=` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El administrador obtiene usuarios paginados, filtrados por rol y estado, sin exponer la contraseña. |

```gherkin
# language: es
Característica: HU-006 consultar los usuarios filtrando por rol y estado
  Escenario: Filtro por rol y estado
    Dado que existen cajeros activos e inactivos
    Cuando consulto con rol "CAJERO" y estado "ACTIVO"
    Entonces el sistema devuelve solo los cajeros activos, paginados y sin la contraseña
  Escenario: Sin resultados
    Dado que ningún usuario cumple el filtro
    Cuando consulto
    Entonces el sistema responde 200 con una lista vacía
  Escenario: Usuario sin permiso
    Dado que no soy administrador
    Cuando consulto los usuarios
    Entonces el sistema responde 403
```

##### HU-007 — Crear el repositorio en GitHub con los 5 microservicios Spring Boot gestionados con Maven

Como equipo de desarrollo, quiero crear el repositorio en GitHub con los 5 microservicios Spring Boot gestionados con Maven, para que trabajemos sobre una base común y versionada.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 16 |
| Microservicio | Todos los microservicios |
| Endpoint | `No aplica (repositorio)` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El repositorio contiene los 5 microservicios Spring Boot con Maven y cada uno compila y prueba de forma independiente. |

```gherkin
# language: es
Característica: HU-007 crear el repositorio en GitHub con los 5 microservicios Spring Boot gestionados con Maven
  Escenario: Compilación de cada servicio
    Dado que clono el repositorio
    Cuando ejecuto el empaquetado con Maven en cualquiera de los 5 microservicios
    Entonces el empaquetado termina correctamente
  Escenario: Sin secretos versionados
    Dado que reviso el repositorio
    Cuando busco contraseñas, claves privadas, tokens o cadenas de conexión reales
    Entonces no encuentro ninguno
```

##### HU-008 — Conectar cada microservicio a su propia base de datos MongoDB

Como equipo de desarrollo, quiero conectar cada microservicio a su propia base de datos MongoDB, para que cada servicio maneje sus datos de forma independiente.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 16 |
| Microservicio | Todos los microservicios |
| Endpoint | `GET /actuator/health` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Cada microservicio usa únicamente su base de datos y recibe la conexión por variable de entorno. |

```gherkin
# language: es
Característica: HU-008 conectar cada microservicio a su propia base de datos MongoDB
  Escenario: Base de datos propia
    Dado que un microservicio arranca con MONGODB_URI configurada
    Cuando consulto la base de datos que utiliza
    Entonces es la que le corresponde: vetcare_pacientes, vetcare_agenda, vetcare_historia, vetcare_farmacia o vetcare_facturacion
  Escenario: MongoDB disponible
    Dado que MongoDB responde
    Cuando consulto /actuator/health
    Entonces el sistema responde 200 con estado UP
  Escenario: MongoDB no disponible
    Dado que MongoDB no responde
    Cuando consulto /actuator/health
    Entonces el sistema responde 503 con estado DOWN
```

##### HU-009 — Registrar una auditoría de accesos y operaciones críticas

Como administrador, quiero registrar una auditoría de accesos y operaciones críticas, para que pueda rastrear quién hizo cada acción.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Media · 16 |
| Microservicio | Todos los microservicios |
| Endpoint | `GET /api/auditoria?usuario=&accion=&desde=&hasta=&page=&size=` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Los accesos y las operaciones críticas quedan registrados con usuario, acción, recurso y fecha, en la base de datos de cada microservicio. |

```gherkin
# language: es
Característica: HU-009 registrar una auditoría de accesos y operaciones críticas
  Escenario: Acceso fallido
    Dado que un intento de inicio de sesión falla
    Cuando el administrador consulta la auditoría de ms-pacientes
    Entonces aparece el intento con correo, fecha y resultado, sin la contraseña
  Escenario: Operación crítica
    Dado que un usuario anula una factura
    Cuando el administrador consulta la auditoría de ms-facturacion
    Entonces aparece quién la anuló, qué factura y cuándo
  Escenario: Usuario sin permiso
    Dado que no soy administrador
    Cuando consulto la auditoría
    Entonces el sistema responde 403
```

##### HU-010 — Documentar los endpoints de autenticación y usuarios en Swagger

Como equipo de desarrollo, quiero documentar los endpoints de autenticación y usuarios en Swagger, para que se puedan probar fácilmente.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Media · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `GET /swagger-ui.html` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Los endpoints de autenticación y usuarios aparecen en Swagger con ejemplos y códigos de respuesta. |

```gherkin
# language: es
Característica: HU-010 documentar los endpoints de autenticación y usuarios en Swagger
  Escenario: Consulta de la documentación
    Dado que ms-pacientes está en ejecución
    Cuando abro Swagger UI
    Entonces veo los endpoints de autenticación y usuarios con ejemplos y códigos de respuesta
  Escenario: Prueba con token
    Dado que obtuve un token en el inicio de sesión
    Cuando lo registro en Swagger UI y ejecuto un endpoint protegido permitido para mi rol
    Entonces el endpoint responde correctamente
```

#### Épica 2: Pacientes y propietarios

##### HU-011 — Registrar propietarios con documento, nombre, teléfono, correo y dirección

Como recepcionista, quiero registrar propietarios con documento, nombre, teléfono, correo y dirección, para que pueda contactarlos y asociarles sus mascotas.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 16 |
| Microservicio | ms-pacientes |
| Endpoint | `POST /api/propietarios` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Se registra un propietario con documento único, nombre, teléfono, correo y dirección. |

```gherkin
# language: es
Característica: HU-011 registrar propietarios con documento, nombre, teléfono, correo y dirección
  Escenario: Registro válido
    Dado que soy recepcionista y el documento no existe
    Cuando registro el propietario con todos sus datos
    Entonces el sistema responde 201 con el propietario creado
  Escenario: Documento duplicado
    Dado que ya existe un propietario con ese documento
    Cuando intento registrarlo
    Entonces el sistema responde 409 "El documento ya está registrado"
  Escenario: Datos inválidos
    Dado que el correo no tiene un formato válido o falta un dato obligatorio
    Cuando intento registrarlo
    Entonces el sistema responde 400 e indica los campos con error
```

##### HU-012 — Registrar una mascota con nombre, especie, raza, sexo, fecha de nacimiento y peso asociada a su propietario

Como recepcionista, quiero registrar una mascota con nombre, especie, raza, sexo, fecha de nacimiento y peso asociada a su propietario, para que se pueda abrir su historia clínica.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 16 |
| Microservicio | ms-pacientes |
| Endpoint | `POST /api/mascotas · GET /api/mascotas/{id}` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Se registra una mascota activa asociada a un propietario existente. |

```gherkin
# language: es
Característica: HU-012 registrar una mascota con nombre, especie, raza, sexo, fecha de nacimiento y peso asociada a su propietario
  Escenario: Registro válido
    Dado que el propietario existe
    Cuando registro la mascota con nombre, especie, raza, sexo, fecha de nacimiento y peso
    Entonces el sistema responde 201 y la mascota queda ACTIVA y asociada al propietario
  Escenario: Propietario inexistente
    Dado que el propietario no existe
    Cuando intento registrar la mascota
    Entonces el sistema responde 404
  Escenario: Datos inválidos
    Dado que la fecha de nacimiento es futura o el peso no es mayor que cero
    Cuando intento registrar la mascota
    Entonces el sistema responde 400
```

##### HU-013 — Buscar propietarios por documento o nombre

Como recepcionista, quiero buscar propietarios por documento o nombre, para que los encuentre rápido cuando llegan a la clínica.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `GET /api/propietarios?documento=&nombre=&page=&size=` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Encuentra propietarios por documento exacto o por coincidencia parcial del nombre, sin distinguir mayúsculas. |

```gherkin
# language: es
Característica: HU-013 buscar propietarios por documento o nombre
  Escenario: Búsqueda por documento
    Dado que existe un propietario con ese documento
    Cuando busco por documento
    Entonces el sistema devuelve ese propietario
  Escenario: Búsqueda por nombre
    Dado que existe la propietaria "María Gómez"
    Cuando busco "gomez"
    Entonces el resultado incluye a "María Gómez"
  Escenario: Sin resultados
    Dado que nadie coincide con la búsqueda
    Cuando busco
    Entonces el sistema responde 200 con una lista vacía
```

##### HU-014 — Actualizar los datos de contacto de un propietario

Como recepcionista, quiero actualizar los datos de contacto de un propietario, para que la información se mantenga vigente.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Media · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `PATCH /api/propietarios/{id}/contacto` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Actualiza teléfono, correo y dirección sin modificar el documento. |

```gherkin
# language: es
Característica: HU-014 actualizar los datos de contacto de un propietario
  Escenario: Actualización válida
    Dado que el propietario existe
    Cuando actualizo su teléfono, correo y dirección
    Entonces el sistema responde 200 con los datos actualizados
  Escenario: Correo inválido
    Dado que el correo nuevo no tiene formato válido
    Cuando intento actualizar
    Entonces el sistema responde 400 y conserva los datos anteriores
  Escenario: Propietario inexistente
    Dado que el propietario no existe
    Cuando intento actualizar
    Entonces el sistema responde 404
```

##### HU-015 — Consultar las mascotas de un propietario

Como recepcionista, quiero consultar las mascotas de un propietario, para que vea todos sus pacientes en una sola vista.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `GET /api/propietarios/{id}/mascotas` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Devuelve todas las mascotas del propietario con su estado. |

```gherkin
# language: es
Característica: HU-015 consultar las mascotas de un propietario
  Escenario: Propietario con mascotas
    Dado que el propietario tiene 2 mascotas, una activa y una inactiva
    Cuando consulto sus mascotas
    Entonces el sistema devuelve las 2 con su estado
  Escenario: Propietario sin mascotas
    Dado que el propietario no tiene mascotas
    Cuando consulto sus mascotas
    Entonces el sistema responde 200 con una lista vacía
  Escenario: Propietario inexistente
    Dado que el propietario no existe
    Cuando consulto sus mascotas
    Entonces el sistema responde 404
```

##### HU-016 — Registrar alergias y condiciones crónicas de la mascota

Como veterinario, quiero registrar alergias y condiciones crónicas de la mascota, para que las tenga presentes al formular.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Alta · 16 |
| Microservicio | ms-pacientes |
| Endpoint | `PUT /api/mascotas/{id}/antecedentes` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El veterinario registra alergias y condiciones crónicas, que quedan disponibles al consultar la mascota. |

```gherkin
# language: es
Característica: HU-016 registrar alergias y condiciones crónicas de la mascota
  Escenario: Registro válido
    Dado que soy veterinario
    Cuando registro la alergia "Penicilina" y la condición crónica "Diabetes"
    Entonces el sistema responde 200 y ambas aparecen al consultar la mascota
  Escenario: Rol no autorizado
    Dado que soy recepcionista
    Cuando intento registrar antecedentes
    Entonces el sistema responde 403
  Escenario: Mascota inexistente
    Dado que la mascota no existe
    Cuando intento registrar antecedentes
    Entonces el sistema responde 404
```

##### HU-017 — Inactivar una mascota o registrar su fallecimiento

Como recepcionista, quiero inactivar una mascota o registrar su fallecimiento, para que no se le asignen nuevas citas.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Media · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `PATCH /api/mascotas/{id}/estado` |
| Reglas de negocio | RN-03 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Una mascota INACTIVA o FALLECIDA no admite nuevas citas; el fallecimiento no se puede revertir. |

```gherkin
# language: es
Característica: HU-017 inactivar una mascota o registrar su fallecimiento
  Escenario: Inactivar
    Dado que la mascota está ACTIVA
    Cuando cambio su estado a INACTIVA
    Entonces el sistema responde 200 y ms-agenda rechaza nuevas citas para ella
  Escenario: Registrar fallecimiento
    Dado que la mascota está ACTIVA
    Cuando registro su fallecimiento con la fecha
    Entonces el sistema responde 200 y la mascota queda FALLECIDA
  Escenario: Revertir fallecimiento
    Dado que la mascota está FALLECIDA
    Cuando intento cambiar su estado a ACTIVA
    Entonces el sistema responde 422
```

##### HU-018 — Transferir una mascota a otro propietario

Como recepcionista, quiero transferir una mascota a otro propietario, para que se refleje el cambio de dueño.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Baja · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `POST /api/mascotas/{id}/transferencia` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | La mascota pasa al nuevo propietario y se conserva el registro del cambio. |

```gherkin
# language: es
Característica: HU-018 transferir una mascota a otro propietario
  Escenario: Transferencia válida
    Dado que la mascota y el nuevo propietario existen
    Cuando transfiero la mascota
    Entonces el sistema responde 200, la mascota aparece en el nuevo propietario y queda registrado el anterior
  Escenario: Propietario destino inexistente
    Dado que el nuevo propietario no existe
    Cuando intento transferir la mascota
    Entonces el sistema responde 404
  Escenario: Mascota fallecida
    Dado que la mascota está FALLECIDA
    Cuando intento transferirla
    Entonces el sistema responde 422
```

##### HU-019 — Consultar un reporte de mascotas por especie y raza

Como administrador, quiero consultar un reporte de mascotas por especie y raza, para que conozca la población que atiende la clínica.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Baja · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `GET /api/reportes/mascotas` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El administrador obtiene el total de mascotas agrupado por especie y raza. |

```gherkin
# language: es
Característica: HU-019 consultar un reporte de mascotas por especie y raza
  Escenario: Reporte con datos
    Dado que existen 3 caninos criollos y 2 felinos persas
    Cuando consulto el reporte
    Entonces el sistema devuelve esos totales por especie y raza
  Escenario: Usuario sin permiso
    Dado que no soy administrador
    Cuando consulto el reporte
    Entonces el sistema responde 403
```

##### HU-020 — Documentar la API de pacientes y propietarios en Swagger

Como equipo de desarrollo, quiero documentar la API de pacientes y propietarios en Swagger, para que los demás microservicios se integren con ella.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 1 · Media · 8 |
| Microservicio | ms-pacientes |
| Endpoint | `GET /v3/api-docs` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | La API de pacientes y propietarios queda documentada en OpenAPI para que los demás servicios se integren. |

```gherkin
# language: es
Característica: HU-020 documentar la API de pacientes y propietarios en Swagger
  Escenario: Contrato disponible
    Dado que ms-pacientes está en ejecución
    Cuando consulto /v3/api-docs
    Entonces obtengo el contrato con los endpoints de propietarios y mascotas
  Escenario: Ejemplos documentados
    Dado que abro Swagger UI
    Cuando reviso un endpoint de mascotas
    Entonces veo ejemplos de solicitud y respuesta y sus códigos de respuesta
```

### Sprint 2 · 10 nov 2026 – 16 nov 2026

#### Épica 3: Agenda, citas y cupos

##### HU-021 — Configurar el horario y el número de cupos diarios de cada veterinario

Como administrador, quiero configurar el horario y el número de cupos diarios de cada veterinario, para que no se sobrecargue la agenda.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 16 |
| Microservicio | ms-agenda |
| Endpoint | `PUT /api/horarios/{veterinarioId}` |
| Reglas de negocio | RN-03 |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El administrador define días, franja horaria, duración de la franja (30 minutos por defecto) y cupos diarios de cada veterinario. |

```gherkin
# language: es
Característica: HU-021 configurar el horario y el número de cupos diarios de cada veterinario
  Escenario: Configuración válida
    Dado que soy administrador
    Cuando configuro de lunes a viernes de 08:00 a 12:00, franjas de 30 minutos y 8 cupos diarios
    Entonces el sistema responde 200 y esa configuración rige la disponibilidad del veterinario
  Escenario: Horario inválido
    Dado que la hora de fin es anterior a la de inicio o los cupos son menores que 1
    Cuando intento guardar
    Entonces el sistema responde 400
  Escenario: Cupos menores a las citas agendadas
    Dado que el veterinario ya tiene 6 citas en una fecha futura
    Cuando intento reducir sus cupos diarios a 5
    Entonces el sistema responde 422 e indica la fecha en conflicto
```

##### HU-022 — Agendar una cita indicando mascota, veterinario, fecha, hora y motivo

Como recepcionista, quiero agendar una cita indicando mascota, veterinario, fecha, hora y motivo, para que se reserve la atención.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 24 |
| Microservicio | ms-agenda |
| Endpoint | `POST /api/citas · GET /api/citas/{id}` |
| Reglas de negocio | RN-03 |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Solo se agenda dentro del horario del veterinario, con cupo disponible, en una franja libre y para una mascota activa. |

```gherkin
# language: es
Característica: HU-022 agendar una cita indicando mascota, veterinario, fecha, hora y motivo
  Escenario: Cita válida
    Dado que el veterinario tiene cupo y la franja de las 09:00 está libre
    Cuando agendo la cita para una mascota activa con motivo
    Entonces el sistema responde 201, la cita queda PROGRAMADA y se consume un cupo
  Escenario: Sin cupos
    Dado que el veterinario ya usó todos sus cupos de esa fecha
    Cuando intento agendar
    Entonces el sistema responde 409 "No hay cupos disponibles"
  Escenario: Franja ocupada
    Dado que la franja de las 09:00 ya está reservada
    Cuando intento agendar a las 09:00
    Entonces el sistema responde 409 "La franja ya está ocupada"
  Escenario: Fuera de horario
    Dado que el veterinario no atiende a esa hora
    Cuando intento agendar
    Entonces el sistema responde 422
  Escenario: Mascota no activa
    Dado que la mascota está INACTIVA o FALLECIDA
    Cuando intento agendar
    Entonces el sistema responde 422
  Escenario: Fecha pasada
    Dado que la fecha de la cita ya pasó
    Cuando intento agendar
    Entonces el sistema responde 422
  Escenario: Servicio de pacientes no disponible
    Dado que ms-pacientes no responde
    Cuando intento agendar
    Entonces el sistema responde 503 y no crea la cita
```

##### HU-023 — Consultar la disponibilidad de los veterinarios por fecha

Como recepcionista, quiero consultar la disponibilidad de los veterinarios por fecha, para que ofrezca horarios libres al propietario.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 16 |
| Microservicio | ms-agenda |
| Endpoint | `GET /api/disponibilidad?fecha=&veterinarioId=` |
| Reglas de negocio | RN-03 |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Devuelve por veterinario las franjas libres y los cupos restantes de la fecha. |

```gherkin
# language: es
Característica: HU-023 consultar la disponibilidad de los veterinarios por fecha
  Escenario: Veterinario con disponibilidad
    Dado que un veterinario tiene 8 cupos y 3 citas ese día
    Cuando consulto la fecha
    Entonces el sistema muestra 5 cupos restantes y sus franjas libres
  Escenario: Sin cupos
    Dado que el veterinario agotó sus cupos
    Cuando consulto la fecha
    Entonces el sistema muestra 0 cupos y ninguna franja libre
  Escenario: Fecha pasada
    Dado que la fecha ya pasó
    Cuando consulto la disponibilidad
    Entonces el sistema responde 400
```

##### HU-024 — Solicitar una cita desde el portal

Como propietario, quiero solicitar una cita desde el portal, para que no tenga que llamar a la clínica.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Media · 16 |
| Microservicio | ms-agenda |
| Endpoint | `POST /api/portal/citas` |
| Reglas de negocio | RN-03 |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El propietario solicita citas solo para sus mascotas; si cumple RN-03 la cita queda confirmada. |

```gherkin
# language: es
Característica: HU-024 solicitar una cita desde el portal
  Escenario: Solicitud válida
    Dado que soy propietario de la mascota y hay cupo en la franja
    Cuando solicito la cita
    Entonces el sistema responde 201 y la cita queda PROGRAMADA
  Escenario: Mascota ajena
    Dado que la mascota no es mía
    Cuando solicito la cita
    Entonces el sistema responde 403
  Escenario: Sin cupos
    Dado que no hay cupos en esa fecha
    Cuando solicito la cita
    Entonces el sistema responde 409 "No hay cupos disponibles"
```

##### HU-025 — Reprogramar o cancelar una cita

Como recepcionista, quiero reprogramar o cancelar una cita, para que se libere o se mueva el cupo.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 16 |
| Microservicio | ms-agenda |
| Endpoint | `PUT /api/citas/{id}/reprogramacion · PATCH /api/citas/{id}/cancelacion` |
| Reglas de negocio | RN-03 |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Cancelar libera el cupo y la franja; reprogramar valida RN-03 en el destino y libera el origen. |

```gherkin
# language: es
Característica: HU-025 reprogramar o cancelar una cita
  Escenario: Cancelación
    Dado que la cita está PROGRAMADA
    Cuando la cancelo indicando el motivo
    Entonces la cita queda CANCELADA y se liberan el cupo y la franja
  Escenario: Reprogramación válida
    Dado que el destino tiene cupo y franja libre
    Cuando reprogramo la cita
    Entonces se ocupa el destino y se libera el origen
  Escenario: Destino sin cupo
    Dado que el destino no tiene cupo
    Cuando intento reprogramar
    Entonces el sistema responde 409 y la cita conserva su fecha original
  Escenario: Cita ya atendida
    Dado que la cita está ATENDIDA
    Cuando intento cancelarla o reprogramarla
    Entonces el sistema responde 422
```

##### HU-026 — Registrar la llegada del paciente (check-in)

Como recepcionista, quiero registrar la llegada del paciente (check-in), para que el veterinario sepa que la mascota está en espera.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 8 |
| Microservicio | ms-agenda |
| Endpoint | `PATCH /api/citas/{id}/llegada` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Registrar la llegada pasa una cita programada para hoy a EN_ESPERA. |

```gherkin
# language: es
Característica: HU-026 registrar la llegada del paciente (check-in)
  Escenario: Llegada válida
    Dado que la cita está PROGRAMADA para hoy
    Cuando registro la llegada
    Entonces la cita queda EN_ESPERA con la hora de llegada y aparece así en la agenda del veterinario
  Escenario: Cita de otra fecha
    Dado que la cita es de otro día
    Cuando intento registrar la llegada
    Entonces el sistema responde 422
  Escenario: Cita cancelada
    Dado que la cita está CANCELADA
    Cuando intento registrar la llegada
    Entonces el sistema responde 422
```

##### HU-027 — Ver mi agenda del día

Como veterinario, quiero ver mi agenda del día, para que organice mis consultas.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 8 |
| Microservicio | ms-agenda |
| Endpoint | `GET /api/citas/mi-agenda?fecha=` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El veterinario ve únicamente sus citas de la fecha, ordenadas por hora y con su estado. |

```gherkin
# language: es
Característica: HU-027 ver mi agenda del día
  Escenario: Agenda del día
    Dado que tengo 3 citas hoy
    Cuando consulto mi agenda
    Entonces el sistema devuelve las 3 ordenadas por hora con su estado
  Escenario: Citas de otro veterinario
    Dado que otro veterinario tiene citas hoy
    Cuando consulto mi agenda
    Entonces no aparecen sus citas
  Escenario: Rol no autorizado
    Dado que soy cajero
    Cuando consulto este endpoint
    Entonces el sistema responde 403
```

##### HU-028 — Agendar servicios de baño y estética con cupos propios

Como personal de peluquería, quiero agendar servicios de baño y estética con cupos propios, para que organice el trabajo del área de peluquería.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Media · 16 |
| Microservicio | ms-agenda |
| Endpoint | `POST /api/peluqueria/citas · PUT /api/peluqueria/horario` |
| Reglas de negocio | RN-03 |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Peluquería agenda baño y estética con cupos propios, que el administrador configura y son independientes de los veterinarios. |

```gherkin
# language: es
Característica: HU-028 agendar servicios de baño y estética con cupos propios
  Escenario: Agendamiento válido
    Dado que peluquería tiene cupo y la franja está libre
    Cuando agendo un baño para una mascota activa
    Entonces el sistema responde 201 y se consume un cupo de peluquería
  Escenario: Sin cupos
    Dado que peluquería agotó sus cupos del día
    Cuando intento agendar
    Entonces el sistema responde 409 "No hay cupos disponibles"
  Escenario: Cupos independientes
    Dado que un veterinario agotó sus cupos del día
    Cuando agendo un servicio de peluquería con cupo disponible
    Entonces el sistema responde 201
```

##### HU-029 — Registrar el servicio realizado con sus observaciones

Como personal de peluquería, quiero registrar el servicio realizado con sus observaciones, para que quede constancia y pase a facturación.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Media · 8 |
| Microservicio | ms-agenda |
| Endpoint | `PATCH /api/peluqueria/citas/{id}/servicio · GET /api/peluqueria/items-pendientes` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El servicio realizado queda con observaciones y disponible como ítem pendiente de facturar. |

```gherkin
# language: es
Característica: HU-029 registrar el servicio realizado con sus observaciones
  Escenario: Registro válido
    Dado que la cita de peluquería está PROGRAMADA
    Cuando registro el servicio realizado con observaciones
    Entonces queda REALIZADA y aparece como ítem pendiente de facturar
  Escenario: Cita cancelada
    Dado que la cita de peluquería está CANCELADA
    Cuando intento registrar el servicio
    Entonces el sistema responde 422
  Escenario: Doble registro
    Dado que el servicio ya está REALIZADO
    Cuando intento registrarlo de nuevo
    Entonces el sistema responde 409
```

##### HU-030 — Recibir un recordatorio de la cita 24 horas antes

Como propietario, quiero recibir un recordatorio de la cita 24 horas antes, para que no la olvide.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Baja · 16 |
| Microservicio | ms-agenda |
| Endpoint | `POST /api/recordatorios/ejecucion` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Una ejecución diaria externa envía por correo un recordatorio de las citas del día siguiente, sin duplicarlo (D12). |

```gherkin
# language: es
Característica: HU-030 recibir un recordatorio de la cita 24 horas antes
  Escenario: Recordatorio enviado
    Dado que una cita PROGRAMADA es para mañana y el propietario tiene correo
    Cuando se ejecuta el proceso diario
    Entonces el propietario recibe el correo y la cita queda marcada como recordada
  Escenario: Sin duplicados
    Dado que el recordatorio de la cita ya fue enviado
    Cuando el proceso se ejecuta de nuevo
    Entonces no se envía otro correo
  Escenario: Cita cancelada
    Dado que la cita de mañana está CANCELADA
    Cuando se ejecuta el proceso diario
    Entonces no se envía recordatorio
  Escenario: Falla del correo
    Dado que el servicio de correo falla
    Cuando se ejecuta el proceso diario
    Entonces se registra el error y la cita queda pendiente para un nuevo intento
  Escenario: Clave incorrecta
    Dado que la petición no trae la clave del proceso en la cabecera X-Clave-Proceso
    Cuando se llama al endpoint
    Entonces el sistema responde 403 y no envía correos
```

#### Épica 4: Historia clínica y vacunación

##### HU-031 — Abrir una consulta a partir de una cita en espera

Como veterinario, quiero abrir una consulta a partir de una cita en espera, para que la atención quede vinculada a la cita y a la mascota.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `POST /api/consultas` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | La consulta se abre desde una cita EN_ESPERA y queda vinculada a la cita y a la mascota; una cita admite una sola consulta. |

```gherkin
# language: es
Característica: HU-031 abrir una consulta a partir de una cita en espera
  Escenario: Apertura válida
    Dado que la cita está EN_ESPERA
    Cuando abro la consulta
    Entonces el sistema responde 201, la consulta queda ABIERTA y vinculada a la cita y a la mascota
  Escenario: Cita no disponible
    Dado que la cita no está EN_ESPERA
    Cuando intento abrir la consulta
    Entonces el sistema responde 422
  Escenario: Consulta duplicada
    Dado que la cita ya tiene consulta
    Cuando intento abrir otra
    Entonces el sistema responde 409
  Escenario: Agenda no disponible
    Dado que ms-agenda no responde
    Cuando intento abrir la consulta
    Entonces el sistema responde 503 y no crea la consulta
```

##### HU-032 — Registrar los signos vitales antes de la consulta

Como auxiliar veterinario, quiero registrar los signos vitales antes de la consulta, para que el veterinario los tenga disponibles.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 8 |
| Microservicio | ms-historia-clinica |
| Endpoint | `PUT /api/citas/{citaId}/signos-vitales` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El auxiliar registra los signos vitales por cita antes de la consulta, y se incorporan a ella al abrirla. |

```gherkin
# language: es
Característica: HU-032 registrar los signos vitales antes de la consulta
  Escenario: Registro válido
    Dado que la cita está EN_ESPERA
    Cuando registro peso, temperatura, frecuencia cardíaca y frecuencia respiratoria
    Entonces el sistema responde 200 y los datos quedan visibles en la consulta
  Escenario: Valores inválidos
    Dado que el peso o la temperatura no son mayores que cero
    Cuando intento registrar
    Entonces el sistema responde 400
  Escenario: Consulta cerrada
    Dado que la consulta de esa cita está CERRADA
    Cuando intento modificar los signos vitales
    Entonces el sistema responde 422
```

##### HU-033 — Registrar anamnesis, examen físico, diagnóstico y plan de tratamiento

Como veterinario, quiero registrar anamnesis, examen físico, diagnóstico y plan de tratamiento, para que quede documentada la atención.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 24 |
| Microservicio | ms-historia-clinica |
| Endpoint | `PUT /api/consultas/{id}/registro-clinico` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El veterinario registra anamnesis, examen físico, diagnóstico y plan mientras la consulta está abierta. |

```gherkin
# language: es
Característica: HU-033 registrar anamnesis, examen físico, diagnóstico y plan de tratamiento
  Escenario: Registro válido
    Dado que la consulta está ABIERTA
    Cuando registro anamnesis, examen físico, diagnóstico y plan de tratamiento
    Entonces el sistema responde 200 y los datos quedan en la consulta
  Escenario: Consulta cerrada
    Dado que la consulta está CERRADA
    Cuando intento modificarla
    Entonces el sistema responde 422 "La consulta está cerrada"
  Escenario: Rol no autorizado
    Dado que soy auxiliar veterinario
    Cuando intento registrar el diagnóstico
    Entonces el sistema responde 403
```

##### HU-034 — Consultar la historia clínica completa de una mascota

Como veterinario, quiero consultar la historia clínica completa de una mascota, para que tome decisiones con sus antecedentes.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `GET /api/historias/{mascotaId}` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Devuelve consultas, vacunas, laboratorio, fórmulas, hospitalizaciones y adjuntos de la mascota, ordenados por fecha, con sus alergias desde ms-pacientes (D7). |

```gherkin
# language: es
Característica: HU-034 consultar la historia clínica completa de una mascota
  Escenario: Historia completa
    Dado que la mascota tiene consultas, vacunas y fórmulas
    Cuando consulto su historia
    Entonces el sistema devuelve todos sus antecedentes ordenados del más reciente al más antiguo
  Escenario: Mascota sin antecedentes
    Dado que la mascota no tiene registros clínicos
    Cuando consulto su historia
    Entonces el sistema responde 200 con las listas vacías
  Escenario: Pacientes no disponible
    Dado que ms-pacientes no responde
    Cuando consulto la historia
    Entonces el sistema devuelve los datos clínicos con el aviso "Alergias no disponibles"
  Escenario: Rol no autorizado
    Dado que soy cajero
    Cuando consulto la historia
    Entonces el sistema responde 403
```

##### HU-035 — Cerrar la consulta

Como veterinario, quiero cerrar la consulta, para que quede inmodificable y pase a facturación.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `PATCH /api/consultas/{id}/cierre · GET /api/consultas/items-pendientes · PATCH /api/citas/{id}/atencion (ms-agenda)` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | La consulta con diagnóstico se cierra, queda inmodificable y pasa a ítems pendientes de facturar. |

```gherkin
# language: es
Característica: HU-035 cerrar la consulta
  Escenario: Cierre válido
    Dado que la consulta está ABIERTA y tiene diagnóstico
    Cuando la cierro
    Entonces queda CERRADA, la cita pasa a ATENDIDA y la consulta aparece como pendiente de facturar
  Escenario: Sin diagnóstico
    Dado que la consulta no tiene diagnóstico
    Cuando intento cerrarla
    Entonces el sistema responde 422
  Escenario: Cierre repetido
    Dado que la consulta ya está CERRADA
    Cuando intento cerrarla de nuevo
    Entonces el sistema responde 422
```

##### HU-036 — Definir el esquema de vacunación por especie

Como veterinario, quiero definir el esquema de vacunación por especie, para que se estandarice la vacunación.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `POST /api/esquemas-vacunacion · GET /api/esquemas-vacunacion?especie=` |
| Reglas de negocio | RN-01 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Cada especie tiene por vacuna un número de dosis y el intervalo en días entre ellas. |

```gherkin
# language: es
Característica: HU-036 definir el esquema de vacunación por especie
  Escenario: Esquema válido
    Dado que soy veterinario
    Cuando defino para "Canino" la vacuna "Rabia" con 2 dosis e intervalo de 365 días
    Entonces el sistema responde 201 y el esquema queda disponible
  Escenario: Esquema duplicado
    Dado que ya existe esa vacuna para la especie
    Cuando intento crearla
    Entonces el sistema responde 409
  Escenario: Datos inválidos
    Dado que las dosis son menores que 1 o el intervalo no es mayor que 0
    Cuando intento crear el esquema
    Entonces el sistema responde 400
```

##### HU-037 — Registrar la vacuna aplicada con lote y fecha

Como veterinario, quiero registrar la vacuna aplicada con lote y fecha, para que lleve el control de vacunación.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Alta · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `POST /api/vacunas` |
| Reglas de negocio | RN-01, RN-04 (por D11) |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Al registrar una dosis con lote y fecha se calcula la próxima según el esquema y se descuenta del lote en ms-farmacia (D11). |

```gherkin
# language: es
Característica: HU-037 registrar la vacuna aplicada con lote y fecha
  Escenario: Dosis con siguiente programada
    Dado que el esquema de "Rabia" para "Canino" tiene 2 dosis cada 365 días
    Cuando registro la dosis 1 con fecha 2026-11-12
    Entonces el sistema responde 201 con próxima dosis 2027-11-12 y descuenta 1 unidad del lote
  Escenario: Última dosis
    Dado que la mascota recibe la última dosis del esquema
    Cuando la registro
    Entonces el esquema queda completo y sin próxima dosis
  Escenario: Sin esquema
    Dado que la vacuna no tiene esquema para la especie
    Cuando intento registrarla
    Entonces el sistema responde 422
  Escenario: Lote sin stock
    Dado que el lote no tiene unidades disponibles
    Cuando intento registrar la dosis
    Entonces el sistema responde 409 y no registra la dosis
  Escenario: Esquema completo
    Dado que la mascota ya recibió todas las dosis del esquema
    Cuando intento registrar otra dosis
    Entonces el sistema responde 422
  Escenario: Farmacia sin configurar
    Dado que FARMACIA_URL no está configurada mientras ms-farmacia no se implementa (Sprint 3)
    Cuando registro la dosis
    Entonces la dosis se guarda marcada con inventario sin descontar
  Escenario: Fecha futura
    Dado que la fecha de aplicación es futura
    Cuando intento registrar la dosis
    Entonces el sistema responde 400
```

##### HU-038 — Consultar las vacunas próximas a vencer o vencidas

Como recepcionista, quiero consultar las vacunas próximas a vencer o vencidas, para que contacte a los propietarios a tiempo.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Media · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `GET /api/vacunas/alertas?dias=30` |
| Reglas de negocio | RN-01 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Lista como PROXIMA las dosis dentro de la ventana (30 días por defecto) y como VENCIDA las de fecha pasada. |

```gherkin
# language: es
Característica: HU-038 consultar las vacunas próximas a vencer o vencidas
  Escenario: Dosis próxima
    Dado que una próxima dosis es dentro de 10 días
    Cuando consulto las alertas
    Entonces aparece como PROXIMA con los datos de la mascota
  Escenario: Dosis vencida
    Dado que una próxima dosis era hace 5 días
    Cuando consulto las alertas
    Entonces aparece como VENCIDA
  Escenario: Fuera de la ventana
    Dado que una próxima dosis es dentro de 60 días
    Cuando consulto las alertas con la ventana por defecto
    Entonces no aparece
```

##### HU-039 — Consultar el carné de vacunación de mi mascota

Como propietario, quiero consultar el carné de vacunación de mi mascota, para que sepa cuándo debo vacunarla.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Media · 8 |
| Microservicio | ms-historia-clinica |
| Endpoint | `GET /api/portal/mascotas/{mascotaId}/carne` |
| Reglas de negocio | RN-01 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El propietario ve vacunas aplicadas y próximas dosis solo de sus mascotas. |

```gherkin
# language: es
Característica: HU-039 consultar el carné de vacunación de mi mascota
  Escenario: Consulta válida
    Dado que la mascota es mía y tiene vacunas
    Cuando consulto el carné
    Entonces veo cada vacuna con dosis, fecha y próxima dosis
  Escenario: Mascota ajena
    Dado que la mascota no es mía
    Cuando consulto el carné
    Entonces el sistema responde 403
  Escenario: Sin vacunas
    Dado que la mascota no tiene vacunas
    Cuando consulto el carné
    Entonces el sistema responde 200 con una lista vacía
```

##### HU-040 — Adjuntar archivos como radiografías o PDF a la historia clínica

Como veterinario, quiero adjuntar archivos como radiografías o PDF a la historia clínica, para que se conserven las evidencias.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 2 · Baja · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `POST /api/historias/{mascotaId}/adjuntos · GET /api/adjuntos/{id}` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Adjunta PDF o imágenes de hasta 5 MB, almacenados en GridFS (D13). |

```gherkin
# language: es
Característica: HU-040 adjuntar archivos como radiografías o PDF a la historia clínica
  Escenario: Adjunto válido
    Dado que tengo un PDF de 2 MB
    Cuando lo adjunto a la historia
    Entonces el sistema responde 201 y se puede descargar desde la historia
  Escenario: Archivo muy grande
    Dado que el archivo pesa más de 5 MB
    Cuando intento adjuntarlo
    Entonces el sistema responde 413
  Escenario: Tipo no permitido
    Dado que el archivo no es PDF ni imagen
    Cuando intento adjuntarlo
    Entonces el sistema responde 415
```

### Sprint 3 · 17 nov 2026 – 23 nov 2026

#### Épica 5: Laboratorio, fórmulas y hospitalización

##### HU-041 — Crear órdenes de laboratorio desde la consulta

Como veterinario, quiero crear órdenes de laboratorio desde la consulta, para que solicite los exámenes necesarios.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Alta · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `POST /api/consultas/{id}/ordenes-laboratorio` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El veterinario crea órdenes desde una consulta abierta y quedan PENDIENTES. |

```gherkin
# language: es
Característica: HU-041 crear órdenes de laboratorio desde la consulta
  Escenario: Orden válida
    Dado que la consulta está ABIERTA
    Cuando ordeno un "Hemograma"
    Entonces el sistema responde 201 y la orden queda PENDIENTE
  Escenario: Consulta cerrada
    Dado que la consulta está CERRADA
    Cuando intento crear la orden
    Entonces el sistema responde 422
  Escenario: Sin examen
    Dado que no indico el examen
    Cuando intento crear la orden
    Entonces el sistema responde 400
```

##### HU-042 — Consultar las órdenes pendientes

Como personal de laboratorio, quiero consultar las órdenes pendientes, para que las procese en el orden correcto.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Alta · 8 |
| Microservicio | ms-historia-clinica |
| Endpoint | `GET /api/ordenes-laboratorio?estado=PENDIENTE&page=&size=` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Laboratorio ve las órdenes pendientes, de la más antigua a la más reciente. |

```gherkin
# language: es
Característica: HU-042 consultar las órdenes pendientes
  Escenario: Órdenes pendientes
    Dado que existen órdenes pendientes y completadas
    Cuando consulto las pendientes
    Entonces el sistema devuelve solo las pendientes, primero la más antigua
  Escenario: Sin pendientes
    Dado que no hay órdenes pendientes
    Cuando consulto
    Entonces el sistema responde 200 con una lista vacía
  Escenario: Rol no autorizado
    Dado que soy recepcionista
    Cuando consulto las órdenes
    Entonces el sistema responde 403
```

##### HU-043 — Registrar los resultados de un examen

Como personal de laboratorio, quiero registrar los resultados de un examen, para que el veterinario pueda consultarlos.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Alta · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `PUT /api/ordenes-laboratorio/{id}/resultado · GET /api/ordenes-laboratorio/items-pendientes` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Registrar el resultado completa la orden y genera un ítem pendiente de facturar. |

```gherkin
# language: es
Característica: HU-043 registrar los resultados de un examen
  Escenario: Resultado válido
    Dado que la orden está PENDIENTE
    Cuando registro el resultado
    Entonces la orden queda COMPLETADA con fecha y responsable, y aparece como pendiente de facturar
  Escenario: Orden ya completada
    Dado que la orden está COMPLETADA
    Cuando intento registrar otro resultado
    Entonces el sistema responde 409
  Escenario: Sin resultado
    Dado que el resultado está vacío
    Cuando intento guardarlo
    Entonces el sistema responde 400
```

##### HU-044 — Ver los resultados de laboratorio en la historia clínica

Como veterinario, quiero ver los resultados de laboratorio en la historia clínica, para que ajuste el diagnóstico.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Media · 8 |
| Microservicio | ms-historia-clinica |
| Endpoint | `GET /api/historias/{mascotaId}/resultados-laboratorio` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El veterinario ve los exámenes de la mascota con su estado y resultado. |

```gherkin
# language: es
Característica: HU-044 ver los resultados de laboratorio en la historia clínica
  Escenario: Con resultados
    Dado que la mascota tiene un hemograma completado
    Cuando consulto los resultados
    Entonces veo el examen, su resultado y su fecha
  Escenario: Orden pendiente
    Dado que una orden sigue PENDIENTE
    Cuando consulto los resultados
    Entonces aparece como pendiente y sin resultado
  Escenario: Rol no autorizado
    Dado que soy cajero
    Cuando consulto los resultados
    Entonces el sistema responde 403
```

##### HU-045 — Emitir una fórmula médica con medicamento, dosis, frecuencia, duración y cantidad

Como veterinario, quiero emitir una fórmula médica con medicamento, dosis, frecuencia, duración y cantidad, para que la mascota reciba su tratamiento.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Alta · 24 |
| Microservicio | ms-historia-clinica |
| Endpoint | `POST /api/formulas · PATCH /api/formulas/{id}/anulacion` |
| Reglas de negocio | RN-02 |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | La fórmula tiene uno o más ítems con medicamento, dosis, frecuencia, duración y cantidad, además de su vigencia; el veterinario puede anular el saldo pendiente (D8, D10). |

```gherkin
# language: es
Característica: HU-045 emitir una fórmula médica con medicamento, dosis, frecuencia, duración y cantidad
  Escenario: Emisión válida
    Dado que la consulta está ABIERTA y el medicamento existe en ms-farmacia
    Cuando emito la fórmula con "Metronidazol 250 mg", 14 tabletas y vigencia hasta 2026-12-12
    Entonces el sistema responde 201 y la fórmula queda VIGENTE con 14 tabletas pendientes
  Escenario: Medicamento inexistente
    Dado que el medicamento no existe en ms-farmacia
    Cuando intento emitir la fórmula
    Entonces el sistema responde 422
  Escenario: Datos inválidos
    Dado que la cantidad no es mayor que 0 o la vigencia ya pasó
    Cuando intento emitir la fórmula
    Entonces el sistema responde 400
  Escenario: Anulación
    Dado que la fórmula está VIGENTE
    Cuando el veterinario la anula indicando el motivo
    Entonces queda ANULADA, sus cantidades pendientes dejan de poder despacharse y la operación se audita
  Escenario: Anulación sin motivo
    Dado que no indico el motivo
    Cuando intento anular la fórmula
    Entonces el sistema responde 400
```

##### HU-046 — Consultar si una fórmula está vigente

Como personal de farmacia, quiero consultar si una fórmula está vigente, para que sepa si puedo despachar.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Alta · 8 |
| Microservicio | ms-historia-clinica |
| Endpoint | `GET /api/formulas/{id}/vigencia` |
| Reglas de negocio | RN-02 |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Devuelve si la fórmula se puede despachar, su estado y las cantidades pendientes por ítem. |

```gherkin
# language: es
Característica: HU-046 consultar si una fórmula está vigente
  Escenario: Fórmula vigente
    Dado que la fórmula no está vencida ni anulada y tiene pendientes
    Cuando consulto la vigencia
    Entonces el sistema indica que es despachable y muestra las cantidades pendientes
  Escenario: Fórmula vencida
    Dado que su fecha de vigencia ya pasó
    Cuando consulto la vigencia
    Entonces el sistema indica VENCIDA y no despachable
  Escenario: Fórmula anulada
    Dado que la fórmula fue anulada
    Cuando consulto la vigencia
    Entonces el sistema indica ANULADA y no despachable
  Escenario: Fórmula despachada
    Dado que no quedan cantidades pendientes
    Cuando consulto la vigencia
    Entonces el sistema indica DESPACHADA y no despachable
  Escenario: Fórmula inexistente
    Dado que la fórmula no existe
    Cuando consulto la vigencia
    Entonces el sistema responde 404
```

##### HU-047 — Ordenar la hospitalización de una mascota con motivo y cubículo

Como veterinario, quiero ordenar la hospitalización de una mascota con motivo y cubículo, para que quede internada bajo control.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Media · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `POST /api/hospitalizaciones` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Se interna a la mascota con motivo y cubículo libre; una mascota no tiene dos hospitalizaciones activas. |

```gherkin
# language: es
Característica: HU-047 ordenar la hospitalización de una mascota con motivo y cubículo
  Escenario: Orden válida
    Dado que el cubículo C-01 está libre
    Cuando ordeno la hospitalización con motivo
    Entonces el sistema responde 201, queda ACTIVA y el cubículo pasa a ocupado
  Escenario: Cubículo ocupado
    Dado que el cubículo C-01 está ocupado
    Cuando intento hospitalizar en C-01
    Entonces el sistema responde 409
  Escenario: Mascota ya hospitalizada
    Dado que la mascota tiene una hospitalización ACTIVA
    Cuando intento hospitalizarla de nuevo
    Entonces el sistema responde 409
```

##### HU-048 — Registrar evoluciones y medicamentos administrados durante la hospitalización

Como auxiliar veterinario, quiero registrar evoluciones y medicamentos administrados durante la hospitalización, para que haya seguimiento continuo del paciente.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Media · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `POST /api/hospitalizaciones/{id}/evoluciones` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El auxiliar registra evoluciones y medicamentos administrados, que deben provenir de una fórmula ya despachada (D11). |

```gherkin
# language: es
Característica: HU-048 registrar evoluciones y medicamentos administrados durante la hospitalización
  Escenario: Evolución válida
    Dado que la hospitalización está ACTIVA
    Cuando registro la evolución con observaciones y signos vitales
    Entonces el sistema responde 201 y queda con fecha y responsable
  Escenario: Medicamento administrado válido
    Dado que el medicamento fue despachado para una fórmula de la mascota
    Cuando registro su administración
    Entonces queda registrado en la evolución
  Escenario: Medicamento sin despacho
    Dado que el medicamento no ha sido despachado para la mascota
    Cuando intento registrar su administración
    Entonces el sistema responde 422
  Escenario: Hospitalización cerrada
    Dado que la mascota ya fue dada de alta
    Cuando intento registrar una evolución
    Entonces el sistema responde 422
```

##### HU-049 — Dar de alta hospitalaria

Como veterinario, quiero dar de alta hospitalaria, para que se libere el cubículo y se facture la estancia.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Media · 16 |
| Microservicio | ms-historia-clinica |
| Endpoint | `PATCH /api/hospitalizaciones/{id}/alta · GET /api/hospitalizaciones/items-pendientes` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El alta libera el cubículo y genera un ítem pendiente de facturar con los días de estancia. |

```gherkin
# language: es
Característica: HU-049 dar de alta hospitalaria
  Escenario: Alta válida
    Dado que la hospitalización está ACTIVA desde hace 3 días
    Cuando doy de alta con indicaciones
    Entonces queda FINALIZADA, se libera el cubículo y aparece un ítem pendiente de 3 días de estancia
  Escenario: Alta repetida
    Dado que la hospitalización ya está FINALIZADA
    Cuando intento dar de alta
    Entonces el sistema responde 422
  Escenario: Rol no autorizado
    Dado que soy auxiliar veterinario
    Cuando intento dar de alta
    Entonces el sistema responde 403
```

##### HU-050 — Consultar los resultados de laboratorio y fórmulas de mi mascota

Como propietario, quiero consultar los resultados de laboratorio y fórmulas de mi mascota, para que siga su tratamiento.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Baja · 8 |
| Microservicio | ms-historia-clinica |
| Endpoint | `GET /api/portal/mascotas/{mascotaId}/resultados-formulas` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El propietario ve resultados completados y fórmulas solo de sus mascotas. |

```gherkin
# language: es
Característica: HU-050 consultar los resultados de laboratorio y fórmulas de mi mascota
  Escenario: Consulta válida
    Dado que la mascota es mía
    Cuando consulto sus resultados y fórmulas
    Entonces veo los resultados completados y las fórmulas con su estado
  Escenario: Orden pendiente
    Dado que una orden sigue PENDIENTE
    Cuando consulto
    Entonces no aparece resultado para esa orden
  Escenario: Mascota ajena
    Dado que la mascota no es mía
    Cuando consulto
    Entonces el sistema responde 403
```

#### Épica 6: Farmacia, inventario y proveedores

##### HU-051 — Registrar medicamentos e insumos con código, presentación, precio y stock mínimo

Como personal de farmacia, quiero registrar medicamentos e insumos con código, presentación, precio y stock mínimo, para que se mantenga el catálogo de farmacia.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Alta · 16 |
| Microservicio | ms-farmacia |
| Endpoint | `POST /api/medicamentos · GET /api/medicamentos?texto=&page=&size=` |
| Reglas de negocio | RN-04 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Se registran medicamentos e insumos con código único, presentación, precio y stock mínimo. |

```gherkin
# language: es
Característica: HU-051 registrar medicamentos e insumos con código, presentación, precio y stock mínimo
  Escenario: Registro válido
    Dado que el código no existe
    Cuando registro el medicamento con presentación, precio y stock mínimo
    Entonces el sistema responde 201 con stock inicial 0
  Escenario: Código duplicado
    Dado que el código ya existe
    Cuando intento registrarlo
    Entonces el sistema responde 409
  Escenario: Datos inválidos
    Dado que el precio o el stock mínimo son negativos
    Cuando intento registrarlo
    Entonces el sistema responde 400
```

##### HU-052 — Registrar lotes con fecha de vencimiento

Como personal de farmacia, quiero registrar lotes con fecha de vencimiento, para que controle la caducidad de los medicamentos.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Alta · 16 |
| Microservicio | ms-farmacia |
| Endpoint | `POST /api/medicamentos/{id}/lotes` |
| Reglas de negocio | RN-04 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Cada lote registra cantidad y vencimiento, aumenta el stock y genera una entrada en el kardex. |

```gherkin
# language: es
Característica: HU-052 registrar lotes con fecha de vencimiento
  Escenario: Lote válido
    Dado que el medicamento existe
    Cuando registro el lote L-100 con 50 unidades y vencimiento futuro
    Entonces el sistema responde 201, el stock aumenta en 50 y el kardex registra la entrada
  Escenario: Lote vencido
    Dado que la fecha de vencimiento ya pasó
    Cuando intento registrar el lote
    Entonces el sistema responde 422
  Escenario: Lote duplicado
    Dado que el lote L-100 ya existe para ese medicamento
    Cuando intento registrarlo
    Entonces el sistema responde 409
  Escenario: Cantidad inválida
    Dado que la cantidad no es mayor que 0
    Cuando intento registrar el lote
    Entonces el sistema responde 400
```

##### HU-053 — Despachar los medicamentos de una fórmula

Como personal de farmacia, quiero despachar los medicamentos de una fórmula, para que la mascota reciba su tratamiento.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Alta · 24 |
| Microservicio | ms-farmacia |
| Endpoint | `POST /api/despachos` |
| Reglas de negocio | RN-02, RN-04 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Rechaza el despacho sin fórmula vigente o por más de lo formulado y descuenta stock del lote que vence primero. |

```gherkin
# language: es
Característica: HU-053 despachar los medicamentos de una fórmula
  Escenario: Despacho válido
    Dado que la fórmula F-010 está vigente con 14 tabletas y hay stock
    Cuando despacho 14 tabletas
    Entonces se descuentan del lote que vence primero y la fórmula queda Despachada
  Escenario: Fórmula no vigente
    Dado que la fórmula F-010 está vencida
    Cuando intento despachar
    Entonces el sistema responde 422 "No se puede despachar sin fórmula vigente"
  Escenario: Cantidad mayor a la formulada
    Dado que la fórmula tiene 14 tabletas pendientes
    Cuando intento despachar 20
    Entonces el sistema responde 422
  Escenario: Stock insuficiente
    Dado que solo hay 10 tabletas en inventario
    Cuando intento despachar 14
    Entonces el sistema responde 409 "Stock insuficiente" y no modifica el inventario
  Escenario: Despacho parcial
    Dado que la fórmula tiene 14 tabletas pendientes
    Cuando despacho 6
    Entonces la fórmula queda PARCIAL con 8 tabletas pendientes
  Escenario: Varios lotes con FEFO
    Dado que el lote A vence primero con 4 tabletas y el lote B tiene 20
    Cuando despacho 10
    Entonces se descuentan 4 del lote A y 6 del lote B, y el kardex registra ambos movimientos
  Escenario: Lote vencido
    Dado que el único lote con stock ya está vencido
    Cuando intento despachar
    Entonces el sistema responde 409 "Stock insuficiente" y no usa el lote vencido
  Escenario: Fórmula anulada
    Dado que la fórmula está anulada
    Cuando intento despachar
    Entonces el sistema responde 422 "No se puede despachar sin fórmula vigente"
  Escenario: Historia clínica no disponible
    Dado que ms-historia-clinica no responde
    Cuando intento despachar
    Entonces el sistema responde 503 y no modifica el inventario
  Escenario: Falla al actualizar la fórmula
    Dado que el stock se descontó y falla la actualización de la fórmula
    Cuando termina el despacho
    Entonces el sistema revierte el descuento, registra la reversión en el kardex y responde 503
```

##### HU-054 — Recibir alertas de stock mínimo

Como personal de farmacia, quiero recibir alertas de stock mínimo, para que reponga a tiempo.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Alta · 8 |
| Microservicio | ms-farmacia |
| Endpoint | `GET /api/alertas/stock-minimo` |
| Reglas de negocio | RN-04 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Lista los productos cuyo stock disponible, sin contar lotes vencidos, es igual o menor que su mínimo. |

```gherkin
# language: es
Característica: HU-054 recibir alertas de stock mínimo
  Escenario: Stock en el mínimo
    Dado que el stock mínimo es 20 y un despacho deja el stock en 20
    Cuando consulto las alertas
    Entonces aparece el medicamento con su stock y su mínimo
  Escenario: Stock suficiente
    Dado que el stock está por encima del mínimo
    Cuando consulto las alertas
    Entonces no aparece el medicamento
  Escenario: Lotes vencidos
    Dado que el mínimo es 20, hay 30 unidades y 15 pertenecen a un lote vencido
    Cuando consulto las alertas
    Entonces aparece el medicamento con stock disponible 15
```

##### HU-055 — Consultar el kardex de un medicamento

Como personal de farmacia, quiero consultar el kardex de un medicamento, para que tenga trazabilidad de sus movimientos.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Media · 16 |
| Microservicio | ms-farmacia |
| Endpoint | `GET /api/medicamentos/{id}/kardex?desde=&hasta= · POST /api/medicamentos/{id}/ajustes` |
| Reglas de negocio | RN-04 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El kardex muestra entradas, despachos y ajustes por lote con su saldo; los ajustes exigen motivo y nunca dejan stock negativo (D9). |

```gherkin
# language: es
Característica: HU-055 consultar el kardex de un medicamento
  Escenario: Consulta del kardex
    Dado que el medicamento tiene entradas y despachos
    Cuando consulto su kardex
    Entonces veo cada movimiento con fecha, tipo, lote, cantidad, saldo y responsable
  Escenario: Ajuste válido
    Dado que el lote tiene 10 unidades
    Cuando registro un ajuste de -2 con motivo "Rotura"
    Entonces el lote queda con 8 unidades y el kardex registra el ajuste
  Escenario: Ajuste que deja stock negativo
    Dado que el lote tiene 10 unidades
    Cuando intento un ajuste de -11
    Entonces el sistema responde 409 "Stock insuficiente" y no modifica el inventario
  Escenario: Ajuste sin motivo
    Dado que no indico el motivo
    Cuando intento registrar el ajuste
    Entonces el sistema responde 400
```

##### HU-056 — Consultar los lotes próximos a vencer

Como personal de farmacia, quiero consultar los lotes próximos a vencer, para que evite pérdidas.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Media · 8 |
| Microservicio | ms-farmacia |
| Endpoint | `GET /api/lotes/proximos-vencer?dias=60` |
| Reglas de negocio | RN-04 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Lista los lotes con stock que vencen dentro de la ventana (60 días por defecto), primero los más cercanos. |

```gherkin
# language: es
Característica: HU-056 consultar los lotes próximos a vencer
  Escenario: Lote próximo
    Dado que un lote con stock vence en 30 días
    Cuando consulto
    Entonces aparece con su medicamento, cantidad y vencimiento
  Escenario: Fuera de la ventana
    Dado que un lote vence en 120 días
    Cuando consulto con la ventana por defecto
    Entonces no aparece
  Escenario: Lote sin stock
    Dado que un lote próximo a vencer tiene 0 unidades
    Cuando consulto
    Entonces no aparece
```

##### HU-057 — Registrar proveedores de medicamentos e insumos

Como administrador, quiero registrar proveedores de medicamentos e insumos, para que gestione el abastecimiento.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Media · 8 |
| Microservicio | ms-farmacia |
| Endpoint | `POST /api/proveedores · GET /api/proveedores` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El administrador registra proveedores con NIT único y datos de contacto. |

```gherkin
# language: es
Característica: HU-057 registrar proveedores de medicamentos e insumos
  Escenario: Registro válido
    Dado que soy administrador y el NIT no existe
    Cuando registro el proveedor
    Entonces el sistema responde 201
  Escenario: NIT duplicado
    Dado que el NIT ya existe
    Cuando intento registrarlo
    Entonces el sistema responde 409
  Escenario: Rol no autorizado
    Dado que soy personal de farmacia
    Cuando intento registrar un proveedor
    Entonces el sistema responde 403
```

##### HU-058 — Generar órdenes de compra a un proveedor

Como personal de farmacia, quiero generar órdenes de compra a un proveedor, para que reponga el inventario.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Media · 16 |
| Microservicio | ms-farmacia |
| Endpoint | `POST /api/ordenes-compra · GET /api/ordenes-compra` |
| Reglas de negocio | RN-04 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Farmacia genera órdenes a un proveedor con productos del catálogo y cantidades mayores que cero. |

```gherkin
# language: es
Característica: HU-058 generar órdenes de compra a un proveedor
  Escenario: Orden válida
    Dado que el proveedor y los medicamentos existen
    Cuando genero la orden con 100 unidades
    Entonces el sistema responde 201 y la orden queda ENVIADA, sin modificar el stock
  Escenario: Proveedor inexistente
    Dado que el proveedor no existe
    Cuando intento generar la orden
    Entonces el sistema responde 404
  Escenario: Cantidad inválida
    Dado que una cantidad no es mayor que 0
    Cuando intento generar la orden
    Entonces el sistema responde 400
```

##### HU-059 — Consultar y confirmar las órdenes de compra que me asignan

Como proveedor, quiero consultar y confirmar las órdenes de compra que me asignan, para que despache los pedidos a tiempo.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Baja · 16 |
| Microservicio | ms-farmacia |
| Endpoint | `GET /api/proveedor/ordenes-compra · PATCH /api/proveedor/ordenes-compra/{id}/confirmacion` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El proveedor ve y confirma únicamente las órdenes que le asignan. |

```gherkin
# language: es
Característica: HU-059 consultar y confirmar las órdenes de compra que me asignan
  Escenario: Consulta
    Dado que tengo 2 órdenes asignadas y otro proveedor tiene 1
    Cuando consulto mis órdenes
    Entonces veo solo mis 2 órdenes
  Escenario: Confirmación
    Dado que mi orden está ENVIADA
    Cuando la confirmo
    Entonces la orden queda CONFIRMADA
  Escenario: Orden ajena
    Dado que la orden es de otro proveedor
    Cuando intento confirmarla
    Entonces el sistema responde 403
  Escenario: Confirmación repetida
    Dado que la orden ya está CONFIRMADA
    Cuando intento confirmarla de nuevo
    Entonces el sistema responde 422
```

##### HU-060 — Registrar la recepción de la mercancía de una orden

Como personal de farmacia, quiero registrar la recepción de la mercancía de una orden, para que se actualice el inventario.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 3 · Media · 16 |
| Microservicio | ms-farmacia |
| Endpoint | `POST /api/ordenes-compra/{id}/recepciones` |
| Reglas de negocio | RN-04 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Cada recepción crea lotes con vencimiento, aumenta el stock y actualiza la orden sin superar lo pedido. |

```gherkin
# language: es
Característica: HU-060 registrar la recepción de la mercancía de una orden
  Escenario: Recepción total
    Dado que la orden CONFIRMADA pide 100 unidades
    Cuando recibo 100 con lote y vencimiento
    Entonces se crea el lote, el stock aumenta en 100, el kardex registra la entrada y la orden queda RECIBIDA
  Escenario: Recepción parcial
    Dado que la orden pide 100 unidades
    Cuando recibo 60
    Entonces la orden queda RECIBIDA_PARCIAL con 40 pendientes
  Escenario: Cantidad excedida
    Dado que quedan 40 unidades pendientes
    Cuando intento recibir 50
    Entonces el sistema responde 422 y no modifica el inventario
  Escenario: Orden sin confirmar
    Dado que la orden sigue ENVIADA
    Cuando intento recibir mercancía
    Entonces el sistema responde 422
  Escenario: Lote vencido
    Dado que la mercancía tiene fecha de vencimiento pasada
    Cuando intento recibirla
    Entonces el sistema responde 422
```

### Sprint 4 · 24 nov 2026 – 1 dic 2026

#### Épica 7: Facturación y caja

##### HU-061 — Generar la factura de una atención con consulta, laboratorio, medicamentos, hospitalización y peluquería

Como cajero, quiero generar la factura de una atención con consulta, laboratorio, medicamentos, hospitalización y peluquería, para que cobre todos los servicios en un solo documento.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Alta · 32 |
| Microservicio | ms-facturacion |
| Endpoint | `POST /api/facturas` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Consolida en una factura los ítems pendientes de la atención desde los demás servicios, con precios de la tarifa vigente, y evita cobrarlos dos veces. |

```gherkin
# language: es
Característica: HU-061 generar la factura de una atención con consulta, laboratorio, medicamentos, hospitalización y peluquería
  Escenario: Factura consolidada
    Dado que la atención tiene consulta, hemograma, medicamentos, estancia y baño pendientes
    Cuando genero la factura
    Entonces el sistema responde 201, incluye los 5 conceptos con su precio y los marca como facturados
  Escenario: Sin ítems pendientes
    Dado que la atención no tiene ítems pendientes
    Cuando intento generar la factura
    Entonces el sistema responde 422
  Escenario: Sin cobro doble
    Dado que un ítem ya fue facturado
    Cuando genero otra factura de la misma atención
    Entonces ese ítem no se incluye
  Escenario: Servicio no disponible
    Dado que uno de los microservicios de origen no responde
    Cuando intento generar la factura
    Entonces el sistema responde 503, no crea la factura y no marca ningún ítem como facturado
  Escenario: Sin tarifa
    Dado que un servicio no tiene tarifa vigente
    Cuando intento generar la factura
    Entonces el sistema responde 422 e indica el servicio
```

##### HU-062 — Configurar las tarifas de los servicios

Como administrador, quiero configurar las tarifas de los servicios, para que las facturas usen precios actualizados.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Alta · 8 |
| Microservicio | ms-facturacion |
| Endpoint | `POST /api/tarifas · PUT /api/tarifas/{id} · GET /api/tarifas` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El administrador configura el precio de los servicios; un cambio solo afecta las facturas nuevas. |

```gherkin
# language: es
Característica: HU-062 configurar las tarifas de los servicios
  Escenario: Configuración válida
    Dado que soy administrador
    Cuando fijo la tarifa de "Consulta general" en 60000
    Entonces el sistema responde 200 o 201 y las facturas nuevas usan ese precio
  Escenario: Cambio de tarifa
    Dado que existe una factura con la tarifa anterior
    Cuando cambio la tarifa
    Entonces la factura existente conserva su precio
  Escenario: Precio inválido
    Dado que el precio es negativo
    Cuando intento guardar
    Entonces el sistema responde 400
  Escenario: Rol no autorizado
    Dado que soy cajero
    Cuando intento cambiar una tarifa
    Entonces el sistema responde 403
```

##### HU-063 — Aplicar impuestos y descuentos a la factura

Como cajero, quiero aplicar impuestos y descuentos a la factura, para que el total sea correcto.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Alta · 16 |
| Microservicio | ms-facturacion |
| Endpoint | `PATCH /api/facturas/{id}/ajustes` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El total es el subtotal menos el descuento más el impuesto configurable, calculado sobre el valor con descuento. |

```gherkin
# language: es
Característica: HU-063 aplicar impuestos y descuentos a la factura
  Escenario: Cálculo correcto
    Dado que el subtotal es 100000 y el impuesto configurado es 19 %
    Cuando aplico un descuento del 10 %
    Entonces el total es 107100
  Escenario: Descuento excesivo
    Dado que el descuento es mayor que el subtotal
    Cuando intento aplicarlo
    Entonces el sistema responde 422
  Escenario: Factura con pagos
    Dado que la factura ya tiene pagos
    Cuando intento cambiar el descuento
    Entonces el sistema responde 422
```

##### HU-064 — Registrar pagos en efectivo, tarjeta o transferencia, incluso combinados

Como cajero, quiero registrar pagos en efectivo, tarjeta o transferencia, incluso combinados, para que se cierre la factura.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Alta · 16 |
| Microservicio | ms-facturacion |
| Endpoint | `POST /api/facturas/{id}/pagos` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Registra pagos en efectivo, tarjeta o transferencia, incluso combinados, con una caja abierta; la factura se cierra cuando el saldo llega a cero. |

```gherkin
# language: es
Característica: HU-064 registrar pagos en efectivo, tarjeta o transferencia, incluso combinados
  Escenario: Pago combinado
    Dado que la factura tiene saldo 100000 y mi caja está abierta
    Cuando pago 60000 en efectivo y 40000 con tarjeta
    Entonces el sistema registra ambos medios y la factura queda PAGADA
  Escenario: Pago mayor al saldo
    Dado que el saldo es 100000
    Cuando intento pagar 120000
    Entonces el sistema responde 422
  Escenario: Caja cerrada
    Dado que no tengo una caja abierta
    Cuando intento registrar un pago
    Entonces el sistema responde 422 "Debe abrir la caja"
  Escenario: Medio inválido
    Dado que el medio de pago no es efectivo, tarjeta ni transferencia
    Cuando intento registrar el pago
    Entonces el sistema responde 400
```

##### HU-065 — Anular una factura indicando el motivo

Como cajero, quiero anular una factura indicando el motivo, para que se corrijan errores sin perder trazabilidad.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Media · 16 |
| Microservicio | ms-facturacion |
| Endpoint | `PATCH /api/facturas/{id}/anulacion` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | La anulación exige motivo, conserva la factura y devuelve sus ítems a pendientes; si tiene pagos, solo procede con la caja de esos pagos abierta. |

```gherkin
# language: es
Característica: HU-065 anular una factura indicando el motivo
  Escenario: Anulación válida
    Dado que la factura no tiene pagos
    Cuando la anulo indicando el motivo
    Entonces queda ANULADA, sus ítems vuelven a pendientes y la operación se audita
  Escenario: Sin motivo
    Dado que no indico el motivo
    Cuando intento anular
    Entonces el sistema responde 400
  Escenario: Pagos en caja abierta
    Dado que la factura tiene pagos en una caja abierta
    Cuando la anulo indicando el motivo
    Entonces queda ANULADA y los pagos se marcan como revertidos en el cuadre de la caja
  Escenario: Pagos en caja cerrada
    Dado que los pagos pertenecen a una caja cerrada
    Cuando intento anular
    Entonces el sistema responde 422
  Escenario: Anulación repetida
    Dado que la factura ya está ANULADA
    Cuando intento anularla
    Entonces el sistema responde 422
```

##### HU-066 — Abrir y cerrar la caja de mi turno

Como cajero, quiero abrir y cerrar la caja de mi turno, para que cuadre el dinero recibido.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Media · 16 |
| Microservicio | ms-facturacion |
| Endpoint | `POST /api/cajas/apertura · PATCH /api/cajas/{id}/cierre` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Cada cajero tiene una caja abierta a la vez; el cierre compara el efectivo esperado y el contado. |

```gherkin
# language: es
Característica: HU-066 abrir y cerrar la caja de mi turno
  Escenario: Apertura válida
    Dado que no tengo una caja abierta
    Cuando abro la caja con base de 100000
    Entonces el sistema responde 201 y la caja queda ABIERTA
  Escenario: Doble apertura
    Dado que ya tengo una caja abierta
    Cuando intento abrir otra
    Entonces el sistema responde 409
  Escenario: Cierre con diferencia
    Dado que el efectivo esperado es 500000
    Cuando cierro la caja con 480000 contados
    Entonces la caja queda CERRADA con una diferencia de -20000
  Escenario: Caja ajena
    Dado que la caja es de otro cajero
    Cuando intento cerrarla
    Entonces el sistema responde 403
```

##### HU-067 — Consultar y descargar mis facturas

Como propietario, quiero consultar y descargar mis facturas, para que lleve el control de mis gastos.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Media · 8 |
| Microservicio | ms-facturacion |
| Endpoint | `GET /api/portal/facturas · GET /api/portal/facturas/{id}/pdf` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | El propietario consulta y descarga en PDF únicamente sus facturas. |

```gherkin
# language: es
Característica: HU-067 consultar y descargar mis facturas
  Escenario: Consulta
    Dado que tengo 2 facturas
    Cuando consulto mis facturas
    Entonces veo las 2 con fecha, total, saldo y estado
  Escenario: Descarga
    Dado que una factura es mía
    Cuando la descargo
    Entonces recibo un PDF con su detalle
  Escenario: Factura ajena
    Dado que la factura es de otro propietario
    Cuando intento descargarla
    Entonces el sistema responde 403
```

##### HU-068 — Registrar abonos a facturas a crédito

Como cajero, quiero registrar abonos a facturas a crédito, para que se controle la cartera.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Baja · 16 |
| Microservicio | ms-facturacion |
| Endpoint | `POST /api/facturas/{id}/abonos` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Los abonos reducen el saldo de una factura a crédito, sin superarlo, y requieren una caja abierta. |

```gherkin
# language: es
Característica: HU-068 registrar abonos a facturas a crédito
  Escenario: Abono válido
    Dado que la factura a crédito tiene saldo 200000
    Cuando registro un abono de 50000
    Entonces el saldo queda en 150000 y la factura sigue CREDITO
  Escenario: Abono final
    Dado que el saldo es 50000
    Cuando registro un abono de 50000
    Entonces el saldo queda en 0 y la factura queda PAGADA
  Escenario: Abono excesivo
    Dado que el saldo es 50000
    Cuando intento abonar 60000
    Entonces el sistema responde 422
  Escenario: Factura de contado
    Dado que la factura no es a crédito
    Cuando intento registrar un abono
    Entonces el sistema responde 422
```

##### HU-069 — Consultar los ingresos por período, servicio y medio de pago

Como administrador, quiero consultar los ingresos por período, servicio y medio de pago, para que tome decisiones financieras.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Media · 16 |
| Microservicio | ms-facturacion |
| Endpoint | `GET /api/reportes/ingresos?desde=&hasta=&servicio=&medioPago=` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Totaliza los pagos del período por servicio y medio de pago, sin contar los revertidos. |

```gherkin
# language: es
Característica: HU-069 consultar los ingresos por período, servicio y medio de pago
  Escenario: Reporte del período
    Dado que hay pagos en efectivo y tarjeta en noviembre
    Cuando consulto noviembre
    Entonces veo el total general, por servicio y por medio de pago
  Escenario: Pago revertido
    Dado que una factura fue anulada y sus pagos se revirtieron
    Cuando consulto el período
    Entonces esos pagos no se suman
  Escenario: Fechas inválidas
    Dado que la fecha inicial es posterior a la final
    Cuando consulto
    Entonces el sistema responde 400
  Escenario: Rol no autorizado
    Dado que soy cajero
    Cuando consulto el reporte
    Entonces el sistema responde 403
```

##### HU-070 — Enviar la factura por correo al propietario

Como cajero, quiero enviar la factura por correo al propietario, para que tenga el soporte del pago.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Baja · 8 |
| Microservicio | ms-facturacion |
| Endpoint | `POST /api/facturas/{id}/envio-correo` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Gabriel Ramirez · Alejandro Gonzalez · Bryan Garcia |
| Criterio principal | Envía el PDF de la factura al correo del propietario; una falla del correo no altera la factura (D12). |

```gherkin
# language: es
Característica: HU-070 enviar la factura por correo al propietario
  Escenario: Envío válido
    Dado que el propietario tiene correo
    Cuando envío la factura
    Entonces el propietario recibe el PDF y queda registrada la fecha de envío
  Escenario: Sin correo
    Dado que el propietario no tiene correo registrado
    Cuando intento enviar la factura
    Entonces el sistema responde 422
  Escenario: Falla del correo
    Dado que el servicio de correo no responde
    Cuando intento enviar la factura
    Entonces el sistema responde 502 y la factura no cambia
```

#### Épica 8: Portal del propietario, reportes y despliegue

##### HU-071 — Ver un panel con mis mascotas, próximas citas y vacunas pendientes

Como propietario, quiero ver un panel con mis mascotas, próximas citas y vacunas pendientes, para que tenga toda la información en un solo lugar.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Media · 16 |
| Microservicio | ms-pacientes · ms-agenda · ms-historia-clinica |
| Endpoint | `GET /api/portal/mascotas · GET /api/portal/citas · GET /api/portal/vacunas/pendientes` |
| Reglas de negocio | RN-01 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El frontend compone el panel con los datos del propietario autenticado; si un servicio falla, muestra el resto. |

```gherkin
# language: es
Característica: HU-071 ver un panel con mis mascotas, próximas citas y vacunas pendientes
  Escenario: Panel completo
    Dado que tengo mascotas, citas próximas y vacunas pendientes
    Cuando abro el panel
    Entonces veo mis mascotas, próximas citas y vacunas pendientes
  Escenario: Datos propios
    Dado que otros propietarios tienen datos en el sistema
    Cuando abro el panel
    Entonces solo veo los míos
  Escenario: Servicio no disponible
    Dado que ms-agenda no responde
    Cuando abro el panel
    Entonces veo mascotas y vacunas, y el aviso de que las citas no están disponibles
```

##### HU-072 — Ver un tablero con citas atendidas, ocupación por veterinario, ventas de farmacia e ingresos

Como administrador, quiero ver un tablero con citas atendidas, ocupación por veterinario, ventas de farmacia e ingresos, para que tome decisiones sobre la clínica.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Media · 16 |
| Microservicio | ms-agenda · ms-farmacia · ms-facturacion |
| Endpoint | `GET /api/reportes/citas · GET /api/reportes/ocupacion · GET /api/reportes/ventas-farmacia · GET /api/reportes/ingresos` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El frontend compone el tablero con citas atendidas, ocupación, ventas de farmacia e ingresos del período. |

```gherkin
# language: es
Característica: HU-072 ver un tablero con citas atendidas, ocupación por veterinario, ventas de farmacia e ingresos
  Escenario: Tablero completo
    Dado que soy administrador y hay datos en el período
    Cuando abro el tablero
    Entonces veo citas atendidas, ocupación por veterinario, ventas de farmacia e ingresos
  Escenario: Servicio no disponible
    Dado que ms-farmacia no responde
    Cuando abro el tablero
    Entonces veo los demás indicadores y el aviso de ventas no disponibles
  Escenario: Rol no autorizado
    Dado que no soy administrador
    Cuando llamo a los reportes
    Entonces el sistema responde 403
```

##### HU-073 — Consultar la ocupación de cupos por veterinario

Como administrador, quiero consultar la ocupación de cupos por veterinario, para que redistribuya la agenda.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Baja · 8 |
| Microservicio | ms-agenda |
| Endpoint | `GET /api/reportes/ocupacion?veterinarioId=&desde=&hasta=` |
| Reglas de negocio | RN-03 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Calcula por veterinario el porcentaje de cupos usados en el período, sin contar citas canceladas. |

```gherkin
# language: es
Característica: HU-073 consultar la ocupación de cupos por veterinario
  Escenario: Ocupación calculada
    Dado que un veterinario tiene 8 cupos diarios y 6 citas activas ese día
    Cuando consulto la ocupación
    Entonces el sistema muestra 75 %
  Escenario: Citas canceladas
    Dado que 2 de esas citas fueron canceladas
    Cuando consulto la ocupación
    Entonces el sistema muestra 50 %
  Escenario: Sin cupos configurados
    Dado que el veterinario no tiene horario en el período
    Cuando consulto la ocupación
    Entonces el sistema muestra 0 % sin error
```

##### HU-074 — Desplegar los 5 microservicios en Render desde GitHub

Como equipo de desarrollo, quiero desplegar los 5 microservicios en Render desde GitHub, para que el sistema esté disponible en la nube.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Alta · 24 |
| Microservicio | Todos los microservicios |
| Endpoint | `GET /actuator/health (en cada URL de Render)` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Los 5 microservicios se despliegan en Render desde main con su Dockerfile y responden en su endpoint de salud. |

```gherkin
# language: es
Característica: HU-074 desplegar los 5 microservicios en Render desde GitHub
  Escenario: Despliegue automático
    Dado que se integra un cambio a main
    Cuando Render construye el microservicio
    Entonces la nueva versión queda publicada
  Escenario: Servicios disponibles
    Dado que los 5 microservicios están desplegados
    Cuando consulto /actuator/health de cada uno
    Entonces todos responden 200 con estado UP
  Escenario: Despliegue fallido
    Dado que la imagen no se puede construir
    Cuando Render intenta desplegar
    Entonces la versión anterior sigue en servicio
```

##### HU-075 — Configurar MongoDB en la nube con variables de entorno en Render

Como equipo de desarrollo, quiero configurar MongoDB en la nube con variables de entorno en Render, para que no haya credenciales en el código.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Alta · 8 |
| Microservicio | Todos los microservicios |
| Endpoint | `No aplica (configuración)` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Cada microservicio se conecta a su base en MongoDB Atlas mediante variables de entorno de Render, sin credenciales en el código. |

```gherkin
# language: es
Característica: HU-075 configurar MongoDB en la nube con variables de entorno en Render
  Escenario: Conexión a Atlas
    Dado que MONGODB_URI está configurada en Render
    Cuando el microservicio arranca
    Entonces se conecta a su base en Atlas y /actuator/health responde UP
  Escenario: Variable ausente
    Dado que MONGODB_URI no está configurada en Render
    Cuando el microservicio arranca
    Entonces /actuator/health no responde UP y el problema queda visible en los registros
  Escenario: Sin credenciales en el código
    Dado que reviso el repositorio
    Cuando busco cadenas de conexión de Atlas
    Entonces no encuentro ninguna
```

##### HU-076 — Publicar el Swagger UI de cada microservicio desplegado

Como equipo de desarrollo, quiero publicar el Swagger UI de cada microservicio desplegado, para que se puedan probar las APIs en línea.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Alta · 8 |
| Microservicio | Todos los microservicios |
| Endpoint | `GET /swagger-ui.html (en cada URL de Render)` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Swagger UI de cada microservicio desplegado es accesible y permite probar endpoints con un token. |

```gherkin
# language: es
Característica: HU-076 publicar el Swagger UI de cada microservicio desplegado
  Escenario: Swagger publicado
    Dado que los microservicios están desplegados
    Cuando abro Swagger UI de cada uno
    Entonces veo su documentación
  Escenario: Prueba en línea
    Dado que tengo un token válido
    Cuando ejecuto un endpoint permitido para mi rol
    Entonces recibo la respuesta del servicio desplegado
  Escenario: Endpoints protegidos
    Dado que no registro un token
    Cuando ejecuto un endpoint protegido
    Entonces el sistema responde 401
```

##### HU-077 — Crear pruebas unitarias y de integración con JUnit y Mockito para las reglas de negocio

Como equipo de desarrollo, quiero crear pruebas unitarias y de integración con JUnit y Mockito para las reglas de negocio, para que se garantice la calidad.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Alta · 24 |
| Microservicio | Todos los microservicios |
| Endpoint | `No aplica (pruebas)` |
| Reglas de negocio | RN-01, RN-02, RN-03, RN-04 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | RN-01 a RN-04 tienen pruebas unitarias con Mockito y pruebas de integración, derivadas de sus escenarios Gherkin. |

```gherkin
# language: es
Característica: HU-077 crear pruebas unitarias y de integración con JUnit y Mockito para las reglas de negocio
  Escenario: Pruebas de RN-01
    Dado que existe un esquema de vacunación
    Cuando ejecuto las pruebas de ms-historia-clinica
    Entonces se valida el cálculo de la próxima dosis y las alertas
  Escenario: Pruebas de RN-02
    Dado que existen fórmulas vigentes, vencidas y anuladas
    Cuando ejecuto las pruebas de ms-farmacia
    Entonces solo se despachan las vigentes y nunca más de lo formulado
  Escenario: Pruebas de RN-03
    Dado que un veterinario tiene cupos configurados
    Cuando ejecuto las pruebas de ms-agenda
    Entonces se rechazan las citas sin cupo y en franjas ocupadas
  Escenario: Pruebas de RN-04
    Dado que existen varios lotes de un medicamento
    Cuando ejecuto las pruebas de ms-farmacia
    Entonces se aplica FEFO, el stock nunca queda negativo y se generan las alertas de mínimo
```

##### HU-078 — Configurar GitHub Actions para compilar y probar con Maven en cada pull request

Como equipo de desarrollo, quiero configurar GitHub Actions para compilar y probar con Maven en cada pull request, para que no se integren cambios con errores.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Media · 16 |
| Microservicio | Todos los microservicios |
| Endpoint | `No aplica (.github/workflows/ci.yml)` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Cada pull request compila y prueba con Maven los 5 microservicios en Java 17. |

```gherkin
# language: es
Característica: HU-078 configurar GitHub Actions para compilar y probar con Maven en cada pull request
  Escenario: Cambios correctos
    Dado que abro un pull request sin errores
    Cuando se ejecuta GitHub Actions
    Entonces el pipeline termina en verde
  Escenario: Prueba fallida
    Dado que un cambio rompe una prueba
    Cuando se ejecuta GitHub Actions
    Entonces el pipeline termina en rojo e identifica el microservicio
```

##### HU-079 — Manejar los errores de forma global con respuestas JSON estándar

Como equipo de desarrollo, quiero manejar los errores de forma global con respuestas JSON estándar, para que las APIs se consuman de forma consistente.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Media · 8 |
| Microservicio | Todos los microservicios |
| Endpoint | `Todos los endpoints` |
| Reglas de negocio | No aplica |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | Todos los errores responden en JSON con timestamp, status, error, mensaje, ruta y detalles. |

```gherkin
# language: es
Característica: HU-079 manejar los errores de forma global con respuestas JSON estándar
  Escenario: Error de validación
    Dado que envío un dato inválido
    Cuando llamo al endpoint
    Entonces el sistema responde 400 en JSON estándar con el detalle por campo
  Escenario: Regla de negocio
    Dado que una petición viola una regla de negocio
    Cuando llamo al endpoint
    Entonces el sistema responde 422 en JSON estándar con el mensaje de la regla
  Escenario: Error inesperado
    Dado que ocurre un error interno
    Cuando llamo al endpoint
    Entonces el sistema responde 500 en JSON estándar sin exponer detalles internos
  Escenario: Servicio dependiente caído
    Dado que otro microservicio no responde
    Cuando llamo a un endpoint que lo necesita
    Entonces el sistema responde 503 en JSON estándar
```

##### HU-080 — Validar de extremo a extremo el flujo completo de atención en la nube

Como administrador, quiero validar de extremo a extremo el flujo completo de atención en la nube, para que se confirme que el sistema está listo para la entrega.

| Elemento | Detalle |
|---|---|
| Sprint · Prioridad · Puntos | Sprint 4 · Alta · 16 |
| Microservicio | Todos los microservicios |
| Endpoint | `Flujo completo sobre las URL de Render` |
| Reglas de negocio | RN-01, RN-02, RN-03, RN-04 |
| Desarrolla · Prueba · Verifica | Alejandro Gonzalez · Gabriel Ramirez · Bryan Garcia |
| Criterio principal | El flujo cita, consulta, laboratorio, fórmula, despacho, hospitalización y factura funciona en la nube. |

```gherkin
# language: es
Característica: HU-080 validar de extremo a extremo el flujo completo de atención en la nube
  Escenario: Flujo completo
    Dado que los 5 microservicios están desplegados
    Cuando recorro cita, consulta, laboratorio, fórmula, despacho, hospitalización y facturación con cada rol
    Entonces cada paso responde correctamente y la factura consolida todos los servicios
  Escenario: Reglas de negocio en la nube
    Dado que uso el sistema desplegado
    Cuando intento agendar sin cupo, despachar sin fórmula vigente y dejar stock negativo, y registro una vacuna
    Entonces se rechazan las tres operaciones inválidas y se calcula la próxima dosis
  Escenario: Aceptación
    Dado que el flujo terminó sin errores
    Cuando se presenta en la Sprint Review
    Entonces el Product Owner acepta la entrega
```
