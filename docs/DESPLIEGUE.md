# Despliegue en Render y MongoDB Atlas

Guía de HU-074, HU-075, HU-076 y HU-080. Requiere tu cuenta en Render y en MongoDB Atlas, y que `main` tenga el código.

## 1. MongoDB Atlas

1. Usa tu clúster o crea uno gratuito M0.
2. En **Database Access**, crea un usuario con permiso de lectura y escritura.
3. En **Network Access**, agrega `0.0.0.0/0`. Los servicios gratuitos de Render no tienen una IP fija; por eso la contraseña debe ser larga y aleatoria.
4. En **Connect > Drivers**, copia la cadena `mongodb+srv://...`, sin nombre de base de datos. Si la contraseña tiene símbolos, codifícalos para URL.

Cada servicio crea su propia base al escribir: `vetcare_pacientes`, `vetcare_agenda`, `vetcare_historia`, `vetcare_farmacia` y `vetcare_facturacion`.

## 2. Claves para la nube

Genera claves nuevas; no reutilices las de tu equipo. En una carpeta que no sea el repositorio:

```powershell
java "C:\Users\bygam\Veterinaria App\scripts\GenerarClavesJwt.java"
```

Esto crea un archivo `.env` en esa carpeta con las claves y el administrador inicial. Bórralo cuando termines. Para `VETCARE_ADMIN_CORREO`, usa el correo real del administrador.

## 3. Render

1. En Render, entra a **New > Blueprint** y conecta el repositorio `vetcare`. Render lee `render.yaml` y propone los 5 servicios.
2. Escribe las variables que pide:

| Variable | Servicios | Valor |
|---|---|---|
| `MONGODB_URI` | Todos | Cadena de Atlas |
| `JWT_PUBLIC_KEY` | Todos | La misma clave pública en todos |
| `JWT_PRIVATE_KEY` | ms-pacientes | Clave privada |
| `VETCARE_ADMIN_CORREO`, `VETCARE_ADMIN_CONTRASENA` | ms-pacientes | Primer administrador |
| `PACIENTES_URL` | ms-agenda, ms-historia-clinica, ms-facturacion | URL de ms-pacientes |
| `AGENDA_URL` | ms-historia-clinica, ms-facturacion | URL de ms-agenda |
| `HISTORIA_URL` | ms-farmacia, ms-facturacion | URL de ms-historia-clinica |
| `FARMACIA_URL` | ms-historia-clinica, ms-facturacion | URL de ms-farmacia |
| `VETCARE_CLAVE_PROCESO` | ms-agenda | Clave aleatoria para los recordatorios |
| `VETCARE_CORREO_REMITENTE`, `SPRING_MAIL_*` | ms-agenda, ms-facturacion | Servidor de correo. Sin esto, HU-030 y HU-070 no envían correos |

Las URL tienen la forma `https://vetcare-ms-pacientes.onrender.com`. Si Render asigna otra, usa la que muestra el panel y actualiza las variables.

3. Espera a que los 5 servicios queden en verde. Con el plan gratuito, cada servicio se duerme tras 15 minutos sin tráfico y tarda cerca de un minuto en despertar.

## 4. Verificación

Comprueba en cada servicio:

- `https://<servicio>.onrender.com/actuator/health` responde `UP` (HU-074 y HU-075).
- `https://<servicio>.onrender.com/swagger-ui.html` muestra su documentación (HU-076).

Después ejecuta el flujo completo (HU-080). **Crea datos de prueba en la base de datos**, identificados con el prefijo `Flujo`:

```powershell
$env:VETCARE_URL_PACIENTES = "https://vetcare-ms-pacientes.onrender.com"
$env:VETCARE_URL_AGENDA = "https://vetcare-ms-agenda.onrender.com"
$env:VETCARE_URL_HISTORIA = "https://vetcare-ms-historia-clinica.onrender.com"
$env:VETCARE_URL_FARMACIA = "https://vetcare-ms-farmacia.onrender.com"
$env:VETCARE_URL_FACTURACION = "https://vetcare-ms-facturacion.onrender.com"
$env:VETCARE_ADMIN_CORREO = "correo del administrador"
$env:VETCARE_ADMIN_CONTRASENA = "contraseña del administrador"
python scripts\flujo_completo.py
```

El script termina con `RESULTADO: TODO OK` o con la lista de pasos fallidos.

## 5. Recordatorios

En GitHub, ve a **Settings > Secrets and variables > Actions** y crea `AGENDA_URL` y `VETCARE_CLAVE_PROCESO`. El proceso diario se ejecuta desde `main`. Mientras esos secretos no existan, no envía nada.
