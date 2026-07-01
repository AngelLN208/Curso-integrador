# 🏥 Clínica Stella Maris — Sistema de Gestión de Citas Médicas

> Proyecto Integrador I: Sistemas Software — Universidad Tecnológica del Perú  
> Sexto Ciclo · 2026

---

## 📋 Descripción

Sistema web full-stack para la gestión integral de citas médicas de la Clínica Stella Maris. Permite a pacientes registrarse, agendar y gestionar sus citas en línea; a recepcionistas administrar el flujo diario; a médicos revisar su agenda y registrar consultas; y a administradores supervisar el sistema completo.

---

## 👥 Equipo

| Integrante | Rol |
|---|---|
| Giron Garcia, Richard Smith | Desarrollador Full-Stack |
| Lizarme Navarro, Miguel Angel Adrian | Desarrollador Full-Stack |
| Vilca Salazar, Josías Joaquín | Desarrollador Full-Stack |
| Chugnas Lupuchi, Diego Augusto | Desarrollador Full-Stack |

**Docente:** Gutierrez Marin, Glicerio Jesus  
**Curso:** Curso Integrador I: Sistemas Software  
**Carrera:** Ingeniería de Sistemas e Informática / Ingeniería de Software  
**Ciclo:** Sexto Ciclo — UTP 2026

---

## 🏗️ Arquitectura

Curso-integrador/
├── backend/          # API REST — Spring Boot 3.3 + Java 22
│   └── src/main/
│       ├── java/     # Controladores, servicios, repositorios, modelos
│       └── resources/
│           └── application.properties
├── frontend/         # Portal de empleados (Recepcionista, Admin, Médico)
│   ├── views/        # HTML por módulo
│   ├── js/           # Lógica por rol
│   └── css/
└── portal/           # Portal de pacientes (autoservicio)
├── views/        # dashboard, citas, directorio, chatbot, perfil
├── js/
└── css/

---

## 🛠️ Stack tecnológico

### Backend
| Tecnología | Versión | Uso |
|---|---|---|
| Java | 22 | Lenguaje principal |
| Spring Boot | 3.3.0 | Framework principal |
| Spring Security + JWT | — | Autenticación y autorización |
| Spring Data JPA / Hibernate 6 | — | ORM |
| PostgreSQL | 16 | Base de datos |
| Lombok | — | Reducción de boilerplate |
| Maven | 3.x | Gestión de dependencias |
| Gemini API (Google) | gemini-2.5-flash | Chatbot con IA |

### Frontend
| Tecnología | Uso |
|---|---|
| HTML5 + CSS3 + Vanilla JS | Interfaces sin frameworks |
| Bootstrap Icons 1.11 | Iconografía |
| Inter (Google Fonts) | Tipografía |

---

## ⚙️ Requisitos previos

- Java 22+
- Maven 3.8+
- PostgreSQL 14+
- Node.js (solo para Live Server de desarrollo frontend)
- VS Code + extensión Live Server

---

## 🚀 Instalación y ejecución

### 1. Clonar el repositorio
```bash
git clone https://github.com/AngelLN208/Curso-integrador.git
cd Curso-integrador
```

### 2. Configurar la base de datos
Crear la base de datos en PostgreSQL:
```sql
CREATE DATABASE clinica_stella_maris;
```

### 3. Configurar `application.properties`
Editar `backend/src/main/resources/application.properties`:
```properties
spring.datasource.username=postgres
spring.datasource.password=TU_PASSWORD_AQUI
```

### 4. Configurar la API key del chatbot (opcional)
```powershell
# Obtener key gratis en: https://aistudio.google.com/app/apikey
$env:GEMINI_API_KEY="tu-key-aqui"
```
Sin esta variable, el chatbot funciona en modo demostración.

### 5. Levantar el backend
```powershell
cd backend
mvn spring-boot:run
```
El backend arranca en `http://localhost:8080`.  
La primera ejecución crea todas las tablas y datos de prueba automáticamente.

### 6. Levantar el frontend
Abrir la carpeta `frontend/` en VS Code y lanzar con Live Server.  
Abrir la carpeta `portal/` en una instancia separada de VS Code y lanzar con Live Server.

> **Importante:** abrir `frontend/` y `portal/` como carpetas raíz separadas en VS Code, no la raíz del monorepo, por dependencias de rutas absolutas.

---

## 👤 Credenciales de prueba

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | admin@stellamaris.pe | admin123 |
| Recepcionista | recepcion@stellamaris.pe | recep123 |
| Médico | medico@stellamaris.pe | medico123 |
| Paciente | (registrarse desde el portal) | — |

---

## 🌐 URLs del sistema

| Portal | URL |
|---|---|
| API REST (backend) | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Portal empleados (frontend) | http://127.0.0.1:[puerto]/views/login.html |
| Portal pacientes | http://127.0.0.1:[puerto]/views/login.html |

*El puerto lo asigna Live Server automáticamente (típicamente 5500 para frontend y 5501 para portal).*

---

## 📦 Módulos del sistema

### Portal de Recepcionista
- Registro y gestión de citas
- Registro de pagos y cobros
- Gestión de pacientes
- Dashboard con métricas diarias

### Portal de Administrador
- Dashboard general con KPIs
- Gestión de médicos, especialidades y horarios
- Gestión de seguros médicos
- Reportes y auditoría
- Control de accesos

### Portal de Médico
- Agenda del día
- Registro de consultas y diagnósticos
- Historial de pacientes
- Gestión de horarios propios

### Portal de Paciente (autoservicio)
- Registro y login propio
- Dashboard personal con próxima cita y pagos pendientes
- Directorio de médicos con filtros (especialidad, nombre, día, fecha)
- Agendar, reprogramar y cancelar citas
- Pago de citas con descuento por seguro
- Comprobante de pago / boleta
- Calificación post-consulta de médicos
- Historial médico en PDF
- Chatbot con IA (Gemini) que orienta y puede agendar citas
- Perfil editable (incluyendo correo y contraseña)

---

## 🔐 Seguridad

- Autenticación basada en JWT (expiración: 24 horas)
- Roles: `ROLE_ADMINISTRADOR`, `ROLE_RECEPCIONISTA`, `ROLE_MEDICO`, `ROLE_PACIENTE`
- Validación de ownership en todas las operaciones del portal de paciente
- Contraseñas hasheadas con BCrypt
- Validación de tarjeta de crédito con algoritmo de Luhn
- CORS configurado para `localhost:*` (desarrollo)

---

## 📝 Variables de entorno

| Variable | Descripción | Requerida |
|---|---|---|
| `GEMINI_API_KEY` | API key de Google Gemini para el chatbot con IA | No (modo demo si no se configura) |

---

## 📄 Documentación adicional

- `docs/Manual-Tecnico.docx` — Arquitectura, modelo de datos, endpoints de la API
- `docs/Manual-Usuario.docx` — Guía de uso por rol con capturas de pantalla

---

## 📃 Licencia

Proyecto académico — Universidad Tecnológica del Perú 2026.  
Uso exclusivo para fines educativos.