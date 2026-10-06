# Control de Citas - Clínica Veterinaria

Sistema backend RESTful para el control, digitalización y automatización de citas médicas de una clínica veterinaria. Permite a los usuarios gestionar propietarios, mascotas, agenda de veterinarios, citas médicas y expedientes clínicos, implementando autenticación JWT stateless y control de acceso granular basado en roles (`ADMIN`, `VET`, `CLIENTE`).

---
## Características principales

### Autenticación y autorización

- Registro e inicio de sesión con validación de credenciales y contraseñas (mín. 6 caracteres)
- Autenticación JWT stateless mediante encabezado `Authorization: Bearer <token>`
- Hash seguro de contraseñas con **BCrypt** (nunca texto plano)
- Asignación obligatoria y automática del rol `CLIENTE` en el registro público (sin escalamiento de privilegios)
- Tres roles estrictos: `ADMIN`, `VET` y `CLIENTE` con protección mediante Spring Security a nivel de rutas y métodos (`@PreAuthorize`)
- Validación de propiedad: un `CLIENTE` no puede acceder, agendar o cancelar recursos correspondientes a otros usuarios

### Gestión de citas y reglas de negocio obligatorias
- **Regla 1 (Disponibilidad del veterinario)**: Duración fija de 30 minutos por cita `[inicio, inicio + 30 min)`. Validación estricta que impide solapamientos temporales con citas activas del mismo veterinario.
- **Regla 2 (Límite diario de citas pendientes)**: Un cliente no puede tener más de 2 citas en estado `PENDIENTE` en un mismo día calendario. Las citas `COMPLETADA` o `CANCELADA` no contabilizan.
- **Regla 3 (Cancelación con anticipación)**: Solo se permite cancelar una cita si faltan más de 2 horas para su hora programada, validado con la hora actual del servidor.
- **Regla 4 (Finalización atómica con expediente)**: Relación 1:1 estricta entre cita y expediente. Al registrar un expediente clínico, la cita pasa automáticamente a estado `COMPLETADA` bajo una transacción atómica (`@Transactional`).

### Mascotas y expedientes clínicos
- Registro de mascotas con validación de especie (`PERRO`, `GATO`, `AVE`, `OTRO`)
- Autoasignación de mascotas al cliente autenticado (o selección de cliente por parte de `ADMIN`)
- Consulta de agenda veterinaria con filtros por fecha y/o veterinario (`VET` y `ADMIN`)
- Historial clínico por mascota accesible para veterinarios, administradores y el cliente dueño

### Base de datos
- Auto-inicialización y actualización de esquema mediante Hibernate/JPA (`ddl-auto: update`)
- Sembrado automático de usuarios iniciales mediante `DataInitializer` al arrancar
- Tablas: `usuarios`, `mascotas`, `citas_medicas`, `expedientes_clinicos`
- Claves primarias, foráneas, constraints únicos e índices optimizados
- Unicidad de `email` en `usuarios` y unicidad de `cita_id` en `expedientes_clinicos`

---

## Stack tecnológico

### Backend
- **Java:** 21 (LTS)
- **Spring Boot:** 3.2.5
- **Spring Security:** 6.x (Stateless, Bearer Token)
- **Spring Data JPA & Hibernate:** 6.x
- **JWT:** JJWT 0.12.5 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- **Validación:** Jakarta Bean Validation / Hibernate Validator
- **Lombok:** Simplificación de código boiler-plate

### Base de datos
- **PostgreSQL:** 16 (Base de datos relacional principal)
- **H2 Database:** Motor en memoria (Modo PostgreSQL para pruebas y desarrollo ágil)

### Herramientas
- **Apache Maven:** 3.9.x con Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Docker & Docker Compose:** Orquestación del contenedor PostgreSQL
- **Git:** Control de versiones

---

## Prerrequisitos

- **Java JDK 21** o superior instalado y configurado en `JAVA_HOME`
- **Git**
- **Docker** y **Docker Compose** (opcional, para ejecutar PostgreSQL en contenedor)

---

## Instalación

```bash
# Clonar repositorio
git clone <repository-url>
cd veterinaria-spring-boot
```

### Configuración de variables de entorno

El sistema soporta configuración externa sin credenciales quemadas en el código:

| Variable | Valor por Defecto | Descripción |
|---|---|---|
| `PORT` | `8081` | Puerto HTTP del backend |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/veterinaria_db` | URL JDBC de conexión a PostgreSQL |
| `DATABASE_USERNAME` | `postgres` | Usuario de base de datos |
| `DATABASE_PASSWORD` | `admin` | Contraseña de PostgreSQL |
| `JWT_SECRET` | Clave HMAC SHA-256 de 256 bits | Llave secreta para firma JWT |
| `JWT_EXPIRATION` | `86400000` (24 horas en ms) | Duración de validez del token |

---

## Estructura del proyecto

```text
veterinaria-spring-boot/
├── .gitignore
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/veterinaria/
    │   │   ├── config/
    │   │   │   └── DataInitializer.java          # Carga de usuarios semilla
    │   │   ├── controller/
    │   │   │   ├── AuthController.java           # /api/v1/auth/*
    │   │   │   ├── CitaController.java           # /api/v1/citas/*
    │   │   │   ├── ExpedienteController.java     # /api/v1/expedientes/*
    │   │   │   └── MascotaController.java        # /api/v1/mascotas/*
    │   │   ├── dto/
    │   │   │   ├── auth/                         # RegisterRequest, LoginRequest, AuthResponse
    │   │   │   ├── cita/                         # CreateCitaRequest, CitaResponse
    │   │   │   ├── error/                        # ErrorResponse (formato estándar)
    │   │   │   ├── expediente/                   # CreateExpedienteRequest, ExpedienteResponse
    │   │   │   └── mascota/                      # CreateMascotaRequest, MascotaResponse
    │   │   ├── entity/
    │   │   │   ├── CitaMedica.java               # Entidad cita_medica
    │   │   │   ├── ExpedienteClinico.java        # Entidad expediente_clinico
    │   │   │   ├── Mascota.java                  # Entidad mascota
    │   │   │   └── Usuario.java                  # Entidad usuario
    │   │   ├── enums/
    │   │   │   ├── Especie.java                  # PERRO, GATO, AVE, OTRO
    │   │   │   ├── EstadoCita.java               # PENDIENTE, COMPLETADA, CANCELADA
    │   │   │   └── Rol.java                      # ADMIN, VET, CLIENTE
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java   # @RestControllerAdvice centralizado
    │   │   │   └── *.java                        # Excepciones de negocio tipadas
    │   │   ├── repository/
    │   │   │   ├── CitaMedicaRepository.java     # Consultas de solapamiento y límites
    │   │   │   ├── ExpedienteClinicoRepository.java
    │   │   │   ├── MascotaRepository.java
    │   │   │   └── UsuarioRepository.java
    │   │   ├── security/
    │   │   │   ├── CustomUserDetails.java
    │   │   │   ├── CustomUserDetailsService.java
    │   │   │   ├── JwtAccessDeniedHandler.java   # 403 Forbidden JSON
    │   │   │   ├── JwtAuthenticationEntryPoint.java # 401 Unauthorized JSON
    │   │   │   ├── JwtAuthenticationFilter.java  # Extractor Bearer JWT
    │   │   │   ├── JwtProperties.java
    │   │   │   ├── JwtTokenProvider.java         # Generador/validador JJWT
    │   │   │   ├── SecurityConfig.java           # Filtros y reglas de autorización
    │   │   │   └── SecurityUtils.java            # Extracción del usuario autenticado
    │   │   ├── service/
    │   │   │   ├── AuthService.java              # Registro y autenticación BCrypt
    │   │   │   ├── CitaService.java              # Reglas 1, 2 y 3
    │   │   │   ├── ExpedienteService.java        # Regla 4 (transaccional)
    │   │   │   └── MascotaService.java           # Gestión y propiedad de mascotas
    │   │   └── VeterinariaApplication.java
    │   └── resources/
    │       ├── application.yml
    │       └── application-postgres.yml
    └── test/
        └── java/com/example/veterinaria/         # 27 tests unitarios y de integración E2E
```

---

## Ejecución

### Desarrollo (Base de datos H2 en memoria)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\mvnw.cmd spring-boot:run
```
Servidor disponible en: `http://localhost:8081/api/v1`

### Producción (PostgreSQL con Docker)

```powershell
# 1. Iniciar contenedor de PostgreSQL
docker compose up -d

# 2. Iniciar la aplicación con el perfil postgres
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/veterinaria_db"
$env:DATABASE_USERNAME = "postgres"
$env:DATABASE_PASSWORD = "postgrespassword"
.\mvnw.cmd spring-boot:run
```

### Ejecución de Pruebas

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\mvnw.cmd test
```

## Endpoints de la API (`/api/v1`)

### Autenticación
| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Público | Registro de clientes (rol forzado a `CLIENTE`) |
| `POST` | `/api/v1/auth/login` | Público | Autenticación y obtención de JWT Bearer |

### Mascotas
| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/mascotas/mis-mascotas` | `CLIENTE` | Obtener mascotas del cliente autenticado |
| `POST` | `/api/v1/mascotas` | `CLIENTE`, `ADMIN` | Registrar mascota (autoasignada a cliente o asignada por admin) |
| `GET` | `/api/v1/mascotas/{id}` | `VET`, `ADMIN` | Consultar detalle de mascota por ID |

### Citas Médicas
| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/citas` | `CLIENTE`, `ADMIN` | Crear cita con estado `PENDIENTE` (valida disponibilidad y límite de 2) |
| `PATCH` | `/api/v1/citas/{id}/cancelar` | `CLIENTE`, `ADMIN` | Cancelar cita (valida que falten más de 2 horas) |
| `GET` | `/api/v1/citas/agenda` | `VET`, `ADMIN` | Consultar agenda con filtros (`fecha`, `veterinarioId`). `CLIENTE` recibe 403 |

### Expedientes Clínicos
| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/expedientes` | `VET`, `ADMIN` | Registrar expediente y finalizar cita a `COMPLETADA` |
| `GET` | `/api/v1/expedientes/mascota/{mascotaId}` | `VET`, `CLIENTE`, `ADMIN` | Consultar historial clínico de una mascota |

---

### Códigos de respuesta habituales

- `200` OK
- `201` Created
- `400` Bad Request (violación de reglas de negocio o error de validación)
- `401` Unauthorized (token JWT ausente, inválido o expirado)
- `403` Forbidden (acceso a recurso ajeno o permisos insuficientes por rol)
- `404` Not Found (recurso no encontrado)
- `409` Conflict (correo ya registrado o cita con expediente previo)
- `500` Server Error (error interno no controlado, sin stack trace)

---

## Base de datos

### Esquema Relacional

```sql
-- Usuarios
CREATE TABLE usuarios (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  telefono VARCHAR(20),
  email VARCHAR(150) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN', 'VET', 'CLIENTE'))
);

CREATE UNIQUE INDEX idx_usuario_email ON usuarios (email);

-- Mascotas
CREATE TABLE mascotas (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  especie VARCHAR(20) NOT NULL CHECK (especie IN ('PERRO', 'GATO', 'AVE', 'OTRO')),
  raza VARCHAR(100),
  edad INTEGER NOT NULL CHECK (edad >= 0),
  cliente_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX idx_mascota_cliente ON mascotas (cliente_id);

-- Citas Médicas
CREATE TABLE citas_medicas (
  id BIGSERIAL PRIMARY KEY,
  mascota_id BIGINT NOT NULL REFERENCES mascotas(id) ON DELETE CASCADE,
  veterinario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
  fecha_hora TIMESTAMP NOT NULL,
  motivo VARCHAR(500) NOT NULL,
  estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'COMPLETADA', 'CANCELADA'))
);

CREATE INDEX idx_cita_vet_fecha ON citas_medicas (veterinario_id, fecha_hora);
CREATE INDEX idx_cita_mascota ON citas_medicas (mascota_id);
CREATE INDEX idx_cita_estado ON citas_medicas (estado);

-- Expedientes Clínicos (Relación 1:1 estricta con cita)
CREATE TABLE expedientes_clinicos (
  id BIGSERIAL PRIMARY KEY,
  cita_id BIGINT UNIQUE NOT NULL REFERENCES citas_medicas(id) ON DELETE CASCADE,
  diagnostico TEXT NOT NULL,
  tratamiento TEXT NOT NULL,
  peso_kg NUMERIC(6,2) NOT NULL CHECK (peso_kg > 0),
  fecha_registro TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_expediente_cita ON expedientes_clinicos (cita_id);
```

### Inicialización automática

Al arrancar el sistema (`DataInitializer`), se crean de manera automática tres cuentas para pruebas:

| Rol | Correo Electrónico | Contraseña |
|---|---|---|
| **ADMIN** | `admin@veterinaria.com` | `admin123` |
| **VET** | `vet@veterinaria.com` | `vet123` |
| **CLIENTE** | `cliente@veterinaria.com` | `cliente123` |

### Seguridad
- Contraseñas cifradas con **BCrypt**
- Tokens **JWT** con firma criptográfica HMAC-SHA256
- Validación sintáctica con Jakarta Bean Validation y validación lógica en capa de servicio
- Manejo centralizado de excepciones con respuestas en formato JSON sin filtración de stack traces
- Integridad referencial y transacciones atómicas con `@Transactional`
