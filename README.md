\# Sistema de Gestión de Citas Médicas — Clínica Stella Maris

> Proyecto académico — Curso Integrador · UTP 2026



\## Estructura del repositorio



| Carpeta | Contenido |

|---|---|

| `backend/` | API REST — Spring Boot 3 + Java 22 + PostgreSQL |

| `frontend/` | Panel interno — Recepcionista y Administrador (HTML + JS) |

| `portal/` | Portal paciente — Registro, citas, historial, chatbot (HTML + JS) |



\## Cómo levantar el proyecto



\### Backend

```bash

cd backend

mvn spring-boot:run

\# API en http://localhost:8080

\# Swagger en http://localhost:8080/swagger-ui.html

```



\### Frontend (panel interno)

Abrir `frontend/` con Live Server en VS Code → http://localhost:5500



\### Portal paciente

Abrir `portal/` con Live Server en VS Code → http://localhost:5501



\## Tecnologías

\- Backend: Spring Boot 3.3 · Java 22 · Maven · PostgreSQL · JWT

\- Frontend: HTML5 · CSS3 · Vanilla JS · Bootstrap 5

\- IA: Anthropic Claude API (chatbot)

\- Despliegue: Render (backend) · Neon (DB) · Netlify (frontends)

