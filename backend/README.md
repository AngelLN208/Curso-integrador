# Sistema de Gestión de Citas Médicas
### Clínica Stella Maris — Lima, Perú

![Java](https://img.shields.io/badge/Java-22-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue)
![Maven](https://img.shields.io/badge/Maven-3.9.16-red)
![Tests](https://img.shields.io/badge/Tests-19%20passing-brightgreen)

---

## Descripción

Sistema web de gestión de citas médicas desarrollado para la Clínica Stella Maris,
institución privada de salud ubicada en Pueblo Libre, Lima. El sistema digitaliza y
centraliza los procesos de registro de pacientes, programación de citas, control de
pagos y atención médica.

**Curso:** Curso Integrador — UTP 2026
**Docente:** Gutierrez Marin, Glicerio Jesus

**Equipo:**
- Lizarme Navarro, Miguel Angel Adrian
- Vilca Salazar, Josías Joaquín
- Giron Garcia, Richard Smith
- Chugnas Lupuchi, Diego Augusto

---

##  Arquitectura

El sistema aplica los siguientes principios y patrones:

- **MVC** — Separación en capas Model, View (API), Controller
- **DAO** — Repositorios con Spring Data JPA
- **SOLID** — Principios aplicados en servicios y controladores
- **TDD** — 19 tests unitarios con JUnit 5 y Mockito
src/
├── model/          → Entidades JPA (14 modelos)
├── repository/     → Capa DAO (14 repositorios)
├── service/        → Lógica de negocio (8 servicios)
├── controller/     → Endpoints REST (9 controladores)
├── dto/            → Objetos de transferencia de datos
├── config/         → Configuración (Security, Swagger, Data)
├── security/       → JWT (JwtUtil, JwtAuthFilter)
└── common/         → ApiResponse, GlobalExceptionHandler, Enums

---

##  Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 22 | Lenguaje principal |
| Spring Boot | 3.3.0 | Framework backend |
| Spring Security | 6.x | Autenticación y autorización |
| Spring Data JPA | 3.3.0 | Capa DAO |
| PostgreSQL | 15+ | Base de datos |
| JWT (JJWT) | 0.12.5 | Tokens de autenticación |
| Swagger (SpringDoc) | 2.5.0 | Documentación de API |
| Google Guava | 33.2.0 | Utilidades Java |
| Apache Commons Lang3 | 3.14.0 | Utilidades de texto |
| Apache POI | 5.2.5 | Exportación a Excel |
| Logback | incluido | Logging estructurado |
| JUnit 5 | incluido | Tests unitarios |
| Mockito | incluido | Mocking en tests |
| Maven | 3.9.16 | Gestión de dependencias |

---

##  Requisitos previos

- Java 22 (JDK)
- Maven 3.9+
- PostgreSQL 12+
- VS Code (recomendado)

---

## Consideraciones de seguridad

En el avance actual, el sistema implementa autenticación con JWT, cifrado de contraseñas con BCrypt, validaciones de entrada y control de acceso por roles mediante Spring Security.

Para la entrega final, las credenciales sensibles como la contraseña de la base de datos, la clave secreta JWT y las credenciales de correo serán externalizadas mediante variables de entorno, evitando que estos valores queden escritos directamente en el archivo `application.properties`.

##  Instalación y ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/AngelLN208/Curso-integrador.git
cd Curso-integrador
```

### 2. Crear la base de datos

```sql
CREATE DATABASE clinica_stella_maris;
```

### 3. Configurar la conexión

Edita `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/clinica_Aviva_Integrador
spring.datasource.username=postgres
spring.datasource.password=TU_PASSWORD
```

### 4. Ejecutar el proyecto

```bash
mvn spring-boot:run
```

Al iniciar, el sistema crea automáticamente:
- Todas las tablas en PostgreSQL
- Usuario administrador: `admin@clinica.pe` / `Admin123*`
- Usuario recepcionista: `recepcion@clinica.pe` / `Recep123*`
- 10 especialidades médicas iniciales

### 5. Acceder a Swagger UI
http://localhost:8080/swagger-ui.html

---

##  Autenticación

El sistema usa **JWT (JSON Web Tokens)**. Para usar los endpoints:

**1. Hacer login:**
```http
POST /api/auth/login
{
  "username": "admin@clinica.pe",
  "password": "Admin123*"
}
```

**2. Copiar el token de la respuesta**

**3. En Swagger:** clic en **Authorize** → pegar el token

---

##  Roles del sistema

| Rol | Acceso |
|---|---|
| `ROLE_ADMINISTRADOR` | Médicos, especialidades, horarios, seguros, auditoría |
| `ROLE_RECEPCIONISTA` | Pacientes, citas, pagos, comprobantes |
| `ROLE_MEDICO` | Triaje, consultas, historial médico |

---

## Endpoints principales

### Autenticación
| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Login y obtención de token JWT |

### Pacientes
| Método | Endpoint | Descripción | RF |
|---|---|---|---|
| POST | `/api/pacientes` | Registrar paciente | RF-01 |
| PUT | `/api/pacientes/{id}` | Actualizar paciente | RF-02 |
| GET | `/api/pacientes/buscar?criterio=` | Buscar por DNI/nombre | RF-03 |
| GET | `/api/pacientes` | Listar todos | RF-04 |

### Citas Médicas
| Método | Endpoint | Descripción | RF |
|---|---|---|---|
| POST | `/api/citas` | Registrar cita | RF-05 |
| PUT | `/api/citas/{id}/reprogramar` | Reprogramar cita | RF-06 |
| GET | `/api/citas/buscar` | Buscar por filtros | RF-07 |
| GET | `/api/citas` | Listar citas | RF-08 |
| PUT | `/api/citas/{id}/cancelar` | Cancelar cita | RF-09 |

### Pagos
| Método | Endpoint | Descripción | RF |
|---|---|---|---|
| POST | `/api/pagos` | Registrar pago | RF-14 |
| GET | `/api/pagos/paciente/{id}` | Pagos del paciente | RF-35 |

### Atención Médica
| Método | Endpoint | Descripción | RF |
|---|---|---|---|
| POST | `/api/atencion/triaje` | Registrar triaje | RF-22 |
| POST | `/api/atencion/consulta` | Registrar consulta | RF-23 |
| GET | `/api/atencion/historial/{id}` | Historial del paciente | RF-26 |

### Administración
| Método | Endpoint | Descripción | RF |
|---|---|---|---|
| POST | `/api/medicos` | Registrar médico | RF-37 |
| POST | `/api/horarios` | Asignar horario | RF-38 |
| POST | `/api/especialidades` | Registrar especialidad | RF-39 |
| POST | `/api/admin/seguros` | Registrar seguro | RF-49 |
| GET | `/api/admin/auditoria/cita/{id}` | Auditoría por cita | RF-42 |

---

##  Modelo de base de datos

El sistema gestiona las siguientes entidades:
usuarios          → Cuentas de acceso con roles
pacientes         → Datos personales de pacientes
medicos           → Médicos y sus especialidades
especialidades    → Especialidades médicas
horarios_medico   → Disponibilidad semanal de médicos
citas_medicas     → Citas con estados (PENDIENTE/CONFIRMADA/CANCELADA...)
pagos             → Pagos asociados a citas
comprobantes      → Comprobantes generados automáticamente
triajes           → Signos vitales antes de consulta
consultas_medicas → Diagnóstico y tratamiento
auditoria_citas   → Historial de cambios en citas
notificaciones    → Notificaciones a pacientes
seguros_medicos   → Seguros disponibles en la clínica
paciente_seguros  → Relación paciente-seguro

---

## ✅ Requerimientos implementados

### Requerimientos Funcionales (RF)
- ✅ RF-01 al RF-13 — Gestión completa de pacientes y citas
- ✅ RF-14 al RF-18 — Pagos y comprobantes automáticos
- ✅ RF-21 al RF-27 — Portal médico (triaje, consulta, historial)
- ✅ RF-37 al RF-43 — Portal administrador
- ✅ RF-44 al RF-48 — Notificaciones automáticas
- ✅ RF-49 al RF-50 — Gestión de seguros médicos

### Requerimientos No Funcionales (RNF)
- ✅ RNF-01 — Contraseñas cifradas con BCrypt
- ✅ RNF-02 — Autenticación con JWT (expira 24h)
- ✅ RNF-03 — Control de acceso por rol
- ✅ RNF-04 — Logs sin datos sensibles
- ✅ RNF-07 — Scheduler de notificaciones cada 60s
- ✅ RNF-08 — Comprobante generado antes de respuesta HTTP
- ✅ RNF-09 — Respuestas uniformes (success, data, message, status)
- ✅ RNF-11 — Diseño modular del backend
- ✅ RNF-12 — Manejo centralizado de excepciones
- ✅ RNF-13 — Enumeraciones para estados
- ✅ RNF-14 — Compatible con Java 22
- ✅ RNF-15 — Compatible con PostgreSQL 12+

---

##  Tests

```bash
mvn test
```
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

| Clase de test | Tests | Cobertura |
|---|---|---|
| `AuthServiceTest` | 2 | RF-40, RNF-02 |
| `CitaServiceTest` | 6 | RF-05, RF-06, RF-09, RF-10 |
| `EspecialidadServiceTest` | 4 | RF-39 |
| `PacienteServiceTest` | 7 | RF-01, RF-02, RF-03, RF-04 |

---

##  Estructura del proyecto
Curso-integrador/
├── src/
│   ├── main/
│   │   ├── java/pe/edu/utp/clinica/
│   │   │   ├── ClinicaApplication.java
│   │   │   ├── config/
│   │   │   ├── security/
│   │   │   ├── common/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── controller/
│   │   │   └── dto/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback-spring.xml
│   └── test/
│       └── java/pe/edu/utp/clinica/
│           └── service/
│               ├── AuthServiceTest.java
│               ├── CitaServiceTest.java
│               ├── EspecialidadServiceTest.java
│               └── PacienteServiceTest.java
├── pom.xml
├── .gitignore
└── README.md

---

##  Librerías utilizadas (rúbrica)

| Librería | Uso en el proyecto |
|---|---|
| **Google Guava** | Utilidades de colecciones y validaciones |
| **Apache Commons Lang3** | Manipulación de strings y objetos |
| **Apache POI** | Exportación de reportes a Excel |
| **Logback** | Logging estructurado con rotación diaria |

---

##  Control de versiones

Historial de commits principales:

| Commit | Descripción |
|---|---|
| `feat: estructura base` | Arquitectura MVC, entidades, repositorios, servicios y controladores |
| `feat: scheduler y data initializer` | Notificaciones automáticas e inicializador de datos |
| `fix: java 22 y logback` | Compatibilidad con Java 22 |
| `test: tests unitarios TDD` | 19 tests con JUnit 5 y Mockito |

---

*Sistema desarrollado como proyecto integrador — UTP 2026*