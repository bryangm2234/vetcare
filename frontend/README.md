# VetCare: interfaz web

Interfaz en React, Vite y TypeScript para los 10 roles del sistema. Consume directamente las API de los 5 microservicios.

## Requisitos

- Node.js 22 o posterior.
- Los microservicios en ejecución. Sus URL se configuran con las variables de [.env.example](.env.example).

## Ejecutar en PowerShell

```powershell
cd frontend
npm install
npm run dev
```

Abre `http://localhost:5173` e inicia sesión con un usuario creado en `ms-pacientes`. El primer administrador está en el archivo `.env` de la raíz del repositorio.

## Probar y compilar

```powershell
npm test
npm run build
```

## Sesión

El token se guarda en `sessionStorage`, así que se borra al cerrar la pestaña. Dura 30 minutos; al expirar, la interfaz vuelve al inicio de sesión. Cerrar sesión elimina el token (D4).

El menú solo oculta opciones: quien protege los datos es el backend, que valida el rol en cada endpoint.

## Errores comunes

| Síntoma | Causa | Solución |
|---|---|---|
| "No se pudo conectar con el servidor" | El microservicio no está en ejecución o su URL es incorrecta | Arráncalo o revisa `VITE_URL_*` |
| Error de CORS en la consola | El origen no está permitido | Agrega el origen a `VETCARE_CORS_ORIGENES` en cada microservicio |
