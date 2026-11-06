# VetCare

Sistema web para gestionar una clínica veterinaria, con 5 microservicios Spring Boot y MongoDB. Cada microservicio tiene su propia base de datos y se comunica con los demás por HTTP y JSON.

La especificación de las 80 historias de usuario está en [docs/specs/HU-VetCare-SDD.md](docs/specs/HU-VetCare-SDD.md) y es la fuente de verdad del proyecto.

## Microservicios

| Servicio | Responsabilidad | Base de datos | Puerto local |
|---|---|---|---|
| [ms-pacientes](ms-pacientes/README.md) | Usuarios, inicio de sesión, propietarios y mascotas | vetcare_pacientes | 8081 |
| [ms-agenda](ms-agenda/README.md) | Horarios, cupos, citas y peluquería | vetcare_agenda | 8082 |
| [ms-historia-clinica](ms-historia-clinica/README.md) | Consultas, vacunas, laboratorio, fórmulas y hospitalización | vetcare_historia | 8083 |
| [ms-farmacia](ms-farmacia/README.md) | Medicamentos, inventario y proveedores | vetcare_farmacia | 8084 |
| [ms-facturacion](ms-facturacion/README.md) | Tarifas, facturas, pagos y caja | vetcare_facturacion | 8085 |

## Tecnologías

Java 17, Spring Boot 4.1, Spring Security, Spring Data MongoDB, springdoc-openapi, Maven Wrapper, JUnit 5, Mockito, GitHub Actions, Docker, Render y MongoDB Atlas. El frontend usará React + Vite + TypeScript en `frontend/` a partir de la Etapa 4.

## Empezar

Instala el JDK 17, crea el archivo `.env` local con `java scripts/GenerarClavesJwt.java` y ejecuta en cada microservicio:

```powershell
.\mvnw.cmd verify
```

Las variables de entorno están en [.env.example](.env.example). Nunca guardes credenciales en el repositorio.
