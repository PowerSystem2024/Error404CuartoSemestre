# E-commerce Backend

Backend del sistema de e-commerce profesional desarrollado con Spring Boot 3.x y Java 17. Incluye un sistema avanzado de carrito híbrido que combina localStorage para UX instantánea con persistencia en base de datos.

## 🌐 URLs de Producción

- **API URL**: [https://testrepo-production-3e94.up.railway.app](https://testrepo-production-3e94.up.railway.app)
- **Documentación API (Swagger)**: [https://testrepo-production-3e94.up.railway.app/swagger-ui.html](https://testrepo-production-3e94.up.railway.app/swagger-ui.html)
- **API Docs JSON**: [https://testrepo-production-3e94.up.railway.app/v3/api-docs](https://testrepo-production-3e94.up.railway.app/v3/api-docs)

## Relación con el Manual de Usuario

Este componente backend implementa toda la lógica de negocio y APIs necesarias para soportar las funcionalidades descritas en el [Manual de Usuario](../manual-usuario-ecommerce.md), incluyendo:

- Sistema completo de autenticación y gestión de usuarios
- Catálogo de productos con búsqueda y filtrado
- Carrito de compras con persistencia híbrida
- Procesamiento de pagos a través de MercadoPago
- Gestión de pedidos y direcciones
- Notificaciones por email
- Panel de administración

## Tecnologías Utilizadas

- **Framework**: Spring Boot 3.3.0
- **Java**: JDK 17
- **Base de Datos**: PostgreSQL 15+
- **Autenticación**: JWT con Spring Security
- **Documentación**: Swagger/OpenAPI 3.0
- **ORM**: Spring Data JPA con Hibernate
- **Build Tool**: Maven
- **Email**: JavaMail con templates Thymeleaf
- **Auditoría**: Sistema completo de logging de operaciones
- **Pagos**: Integración con MercadoPago
- **Imágenes**: Cloudinary para gestión de imágenes
- **Cache**: Spring Cache con Redis (opcional)

## Arquitectura

El proyecto sigue los principios SOLID y DRY con la siguiente estructura:

```
src/main/java/com/ecommerce/
├── config/           # Configuraciones de Spring
│   ├── CacheConfig.java
│   ├── CorsConfig.java
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   └── WebConfig.java
├── controller/       # Controladores REST
│   ├── auth/        # Autenticación (AuthController, ProfileController)
│   ├── catalog/     # Catálogo (ProductController, CategoryController)
│   ├── order/       # Gestión de órdenes (OrderController)
│   ├── payment/     # Procesamiento de pagos (ShoppingCartController, PaymentController)
│   ├── admin/       # Endpoints administrativos (AdminController)
│   └── public/      # Endpoints públicos (PublicController)
├── service/         # Lógica de negocio
│   ├── interfaces/  # Interfaces de servicios
│   │   ├── ShoppingCartService.java
│   │   ├── ProductService.java
│   │   └── ...
│   └── impl/        # Implementaciones
│       ├── ShoppingCartServiceImpl.java
│       └── ...
├── repository/      # Repositorios JPA
│   ├── ShoppingCartRepository.java
│   ├── CartItemRepository.java
│   └── ...
├── model/           # Entidades y modelos
│   ├── entity/      # Entidades JPA
│   │   ├── ShoppingCart.java
│   │   ├── CartItem.java
│   │   └── ...
│   ├── enums/       # Enumeraciones
│   └── ...
├── dto/             # Data Transfer Objects
│   ├── request/     # DTOs de entrada
│   ├── response/    # DTOs de salida
│   └── ...
├── security/        # Configuración de seguridad
│   ├── jwt/         # Utilidades JWT
│   └── ...
├── exception/       # Manejo de excepciones
├── event/           # Eventos del sistema
├── utils/           # Utilidades
└── templates/       # Templates de email
    └── email/
```

## 🚀 Sistema de Carrito Híbrido Avanzado

### Características Principales
- **UX Instantánea**: localStorage para operaciones inmediatas sin delays
- **Persistencia Multi-dispositivo**: Sincronización automática con base de datos
- **Sesiones de Invitados**: Carritos persistentes sin autenticación usando sessionId
- **Sincronización en Background**: Cambios se sincronizan automáticamente
- **Recuperación Automática**: Fallback a datos locales si el backend falla

## ⭐ Sistema de Gestión de Reseñas Avanzado

### Características
- **Auto-publicación**: Las reseñas se publican inmediatamente con estado `APPROVED`
- **Moderación Administrativa**: Admins pueden ocultar reseñas que violen políticas
- **Soft Delete con Auditoría**: Las reseñas se preservan en BD para auditoría completa
- **Razón de Ocultamiento**: Campo de 5-500 caracteres para registrar por qué se oculta
- **Endpoints RESTful**: API completa en `AdminProductReviewController`
- **DTOs Validados**: `HideReviewRequest` con validaciones automáticas

### Endpoints de Revisión (Admin)
```
GET    /admin/reviews              - Obtener todas las reseñas
GET    /admin/reviews/pending      - Reseñas pendientes (compatibilidad)
GET    /admin/reviews/{id}         - Detalles de una reseña
PUT    /admin/reviews/{id}/approve - Aprobar reseña
DELETE /admin/reviews/{id}/reject  - Rechazar reseña
DELETE /admin/reviews/{id}         - Ocultar reseña con razón
DELETE /admin/reviews/{id}/eliminar - Eliminar permanentemente (hard delete)
GET    /admin/reviews/stats/{productId} - Estadísticas de reseñas
```

### Campos en ProductReview
- `id`: Identificador único
- `rating`: Calificación 1-5 estrellas
- `comment`: Texto de la reseña
- `status`: APPROVED, REJECTED, PENDING
- `hiddenReason`: Razón de ocultamiento (5-500 caracteres, nullable)
- `user`: Usuario que escribió la reseña
- `product`: Producto reseñado
- `active`: false cuando está ocultada (soft delete)
- `deletedAt`: Timestamp de cuándo se ocultó
- `deletedBy`: ID del admin que la ocultó
- `createdAt`: Timestamp de creación
- `updatedAt`: Timestamp de última actualización

### Sistema de Atributos de Variante

El sistema soporta dos tipos de atributos para productos variantes:

#### Atributos Globales 🌍
- Disponibles para **todas las categorías**
- Ejemplos: Color, Material, Peso, Dimensiones
- Configurados una vez, aplicables universalmente

#### Atributos Específicos por Categoría 🎯
- Solo disponibles para **categorías seleccionadas**
- Ejemplos: Tamaño de Pantalla (Electrónicos), Talla de Ropa (Ropa), ISBN (Libros)
- Mayor flexibilidad y relevancia por categoría

#### Configuración en Admin Panel
- Radio buttons para elegir alcance (Global vs Específico)
- Selector múltiple de categorías para atributos específicos
- Validación automática de selección de categorías
- Visualización clara del alcance en la tabla de atributos

#### Datos de Prueba Incluidos
```sql
-- Script: insert-test-data-complete.sql
-- Total de registros insertados: 177+

-- 👥 USUARIOS (10 total)
- 3 Administradores: admin1, admin2, admin3 (password: password)
- 7 Usuarios Famosos: Mickey Mouse, Superman, Harry Potter, Luke Skywalker, Frodo Baggins, Wonder Woman, Iron Man

-- 🏠 DIRECCIONES (11 total)
- Múltiples direcciones por usuario con ubicaciones temáticas
- Admins: Buenos Aires, Argentina
- Mickey Mouse: Magic Kingdom, Orlando
- Superman: Metropolis, USA
- Harry Potter: Privet Drive, Reino Unido
- Luke Skywalker: Tatooine, Galaxia
- Frodo Baggins: Bag End, Tierra Media
- Wonder Woman: Themyscira Island, USA
- Iron Man: Malibu Point, USA

-- 📂 CATEGORÍAS (6 total)
- Electrónicos: iPhone, MacBook, Samsung, Sony, iPad
- Ropa: Polo Lacoste, Jeans Levi's, Vestidos, Nike, Chaquetas
- Libros: Señor de los Anillos, Harry Potter, Clásicos literarios
- Hogar: Cafetera, Sábanas, Lámparas, Ollas, Aspiradora
- Deportes: Bicicleta, Pelota Fútbol, Mancuernas, Raqueta, Yoga
- Belleza: Chanel No.5, La Mer Crème, Paletas, Cepillos, Aceite

-- 📦 PRODUCTOS (30 total)
- 5 productos por categoría con descripciones detalladas
- Precios realistas con precios de oferta
- Stock variado (15-100 unidades)
- Algunos productos marcados como destacados

-- 🖼️ IMÁGENES (32 total)
- Al menos 1 imagen por producto
- URLs de ejemplo para desarrollo
- Sistema preparado para integración con Cloudinary

-- ⭐ RESEÑAS (60 total)
- 7-10 reseñas por producto con ratings variados (3-5 estrellas)
- Comentarios realistas y detallados
- Fechas distribuidas en el tiempo
- Usuarios famosos como reviewers

-- 🔧 ATRIBUTOS DE VARIANTE (6 total)
- Color (global): Rojo, Azul, Verde, Negro, Blanco
- Material (global): Algodón, Poliéster, Lana, Cuero
- Peso (global): en kg
- Talla (específico para ropa): XS, S, M, L, XL
- Tamaño Pantalla (específico para electrónicos): 13.3", 15.6", 24"
- ISBN (específico para libros): campo de texto

-- 📋 VARIACIONES (7 total)
- iPhone 15 Pro: 3 variantes (256GB Negro, Azul, Blanco)
- Camisa Polo: 4 variantes (Tallas S, M, L, XL)
- Valores de atributos asociados a cada variante
```

### Endpoints de Carrito

#### Para Usuarios Autenticados
```http
GET    /cart                    # Obtener carrito del usuario
POST   /cart/items              # Agregar item al carrito
PUT    /cart/items/{itemId}     # Actualizar cantidad
DELETE /cart/items/{itemId}     # Remover item
DELETE /cart                    # Vaciar carrito
```

#### Para Usuarios Invitados (Guest Cart)
```http
GET    /guest-cart?sessionId={sessionId}           # Obtener carrito de invitado
POST   /guest-cart/items?sessionId={sessionId}      # Agregar item
PUT    /guest-cart/items/{itemId}?sessionId={sessionId}  # Actualizar cantidad
DELETE /guest-cart/items/{itemId}?sessionId={sessionId}  # Remover item
DELETE /guest-cart?sessionId={sessionId}           # Vaciar carrito
```

### Funcionamiento del Sistema Híbrido

1. **Primera Carga**: Se intenta cargar desde localStorage para UX instantánea
2. **Sincronización**: En background se sincroniza con el backend
3. **Operaciones**: Los cambios se aplican primero en localStorage, luego se sincronizan
4. **Fallback**: Si el backend falla, se mantienen los datos locales
5. **Sesiones**: Los invitados usan sessionId único generado automáticamente

## 🖼️ Sistema de Imágenes Privadas con Cloudinary

### Características Principales
- **Imágenes Privadas**: Acceso seguro solo desde la aplicación web (`access_mode: authenticated`)
- **URLs Firmadas**: Generación automática de URLs temporales con expiración (15 minutos)
- **Endpoint Seguro**: `/api/images/signed-url` para obtener URLs válidas
- **Configuración Automática**: Las imágenes se suben como privadas automáticamente

### Funcionamiento
1. **Upload**: Las imágenes se suben a Cloudinary con `access_mode: authenticated`
2. **Almacenamiento**: Las imágenes quedan privadas y no accesibles directamente
3. **Acceso**: El frontend solicita URLs firmadas al endpoint `/api/images/signed-url`
4. **Expiración**: Las URLs firmadas expiran automáticamente después de 15 minutos
5. **Seguridad**: Solo usuarios autenticados pueden acceder a las imágenes

### Endpoint de Imágenes
```http
GET /api/images/signed-url?publicId={publicId}&expirationMinutes=15
```

### Configuración en .env
```env
# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

## 🔑 Sistema de Reset de Contraseña

### Características Principales
- **Solicitud por Email**: Los usuarios pueden solicitar reset desde cualquier dispositivo
- **Tokens Seguros**: Tokens únicos con expiración automática (24 horas)
- **Validación Completa**: Verificación de email, contraseña y token
- **Plantillas HTML**: Emails atractivos con Thymeleaf
- **Auditoría**: Registro completo de todas las operaciones

### Endpoints de Reset
```http
POST /auth/forgot-password     # Solicitar reset de contraseña
POST /auth/reset-password      # Cambiar contraseña con token
```

### Funcionamiento
1. **Solicitud**: Usuario ingresa email y solicita reset
2. **Generación**: Se crea token único y se envía por email
3. **Validación**: Token se verifica antes de permitir cambio
4. **Cambio**: Nueva contraseña se establece y token se invalida
5. **Auditoría**: Toda la operación queda registrada

### Configuración en .env
```env
# Email SMTP (requerido para reset de contraseña)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password_here
```

## Ejecución del Proyecto

### Prerrequisitos
- JDK 17 instalado y configurado en `JAVA_HOME`
- PostgreSQL corriendo
- Maven 3.6+

### Configuración
1. Configura las variables de entorno en `.env`
2. Ejecuta el script completo de configuración de base de datos:

   ```bash
   # Windows con PostgreSQL instalado
   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d ecommerce -f database-setup-complete.sql
   ```

   Este script crea automáticamente todas las tablas, usuarios de prueba y datos básicos.

3. **OPCIONAL - Datos de Prueba Completos**: Para poblar con datos exhaustivos de desarrollo:

   ```bash
   # Ejecutar script completo de datos de prueba
   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d ecommerce -f insert-test-data-complete.sql
   ```

   **¿Qué incluye este script?**
   - ✅ **30 productos** completos con imágenes y descripciones
   - ✅ **60 reseñas** con ratings variados (7-10 por producto)
   - ✅ **10 usuarios** (3 admins + 7 personajes famosos)
   - ✅ **6 categorías** principales con productos asignados
   - ✅ **Atributos y variantes** de producto completos
   - ✅ **Direcciones** temáticas para cada usuario
   - ✅ **Datos realistas** para testing completo de funcionalidades

### Ejecutar
```bash
mvn spring-boot:run
```

El servidor iniciará en `http://localhost:8081`

### Documentación API
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## Entidades Principales

- **User**: Usuarios del sistema con roles (USER/ADMIN)
- **Category**: Categorías jerárquicas de productos
- **Product**: Productos con variantes, imágenes y reseñas
- **Order**: Órdenes de compra con estados completos
- **ShoppingCart**: Carritos de compra con soporte para usuarios y sesiones
- **CartItem**: Items individuales del carrito
- **AuditLog**: Registro completo de operaciones del sistema
- **EmailVerificationToken**: Tokens para verificación de email
- **MercadoPagoTransaction**: Transacciones de pago
- **ProductReview**: Reseñas y calificaciones de productos

## Configuración de Base de Datos

### Desarrollo (Perfil `dev`)
```yaml
spring:
  datasource:
    url: ${DB_DEV_URL}          # jdbc:postgresql://localhost:5432/ecommerce_dev
    username: ${DB_DEV_USERNAME} # postgres
    password: ${DB_DEV_PASSWORD} # 1234
  jpa:
    hibernate:
      ddl-auto: validate  # Solo valida esquema (los datos persisten entre reinicios)
    show-sql: true     # Muestra SQL en logs
```

**ℹ️ IMPORTANTE**: En desarrollo usamos `validate` en lugar de `create` para preservar los datos en cada reinicio del proyecto. Esto permite:
- ✅ Mantener datos entre reinicios
- ✅ Detectar cambios en el schema
- ✅ Evitar pérdida accidental de datos
- ⚠️ Requiere cargar datos iniciales manualmente (ver `insert-test-data-complete.sql`)

### Testing (Perfil `test`)
```yaml
spring:
  datasource:
    url: ${DB_TEST_URL}          # jdbc:postgresql://localhost:5432/ecommerce_test
    username: ${DB_TEST_USERNAME} # postgres
    password: ${DB_TEST_PASSWORD} # 1234
  jpa:
    hibernate:
      ddl-auto: create-drop  # Crea y elimina tablas por test
```

### Producción (Perfil `prod`)
```yaml
spring:
  datasource:
    url: ${DB_PROD_URL}          # jdbc:postgresql://localhost:5432/ecommerce_prod
    username: ${DB_PROD_USERNAME} # postgres
    password: ${DB_PROD_PASSWORD} # 1234
  jpa:
    hibernate:
      ddl-auto: validate  # Solo valida esquema existente (SEGURO)
    show-sql: false      # No muestra SQL por seguridad
```

**ℹ️ IMPORTANTE**: En producción SIEMPRE usar `validate` para máxima seguridad y evitar pérdida de datos.

**Nota**: Las configuraciones específicas de cada perfil están definidas en el archivo `.env` con variables separadas por entorno para mantener la separación de responsabilidades.

## API Endpoints

### Base URL
```
http://localhost:8081/api
```

**Nota**: Los endpoints de la API tienen el prefijo `/api`, pero Swagger UI está disponible tanto en:
- `http://localhost:8081/swagger-ui.html` (sin /api)
- `http://localhost:8081/api/swagger-ui.html` (con /api)

El puerto (8081) y el context-path (/api) están configurados en el archivo `.env`.

### Autenticación (`/auth`)

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/auth/login` | Iniciar sesión y obtener token JWT | No requerida |
| POST | `/auth/register` | Registrar nuevo usuario (email, username opcional, password) | No requerida |
| POST | `/auth/refresh` | Generar nuevo token JWT usando token existente | Token JWT |
| POST | `/auth/forgot-password` | Solicitar reset de contraseña | No requerida |
| POST | `/auth/reset-password` | Resetear contraseña | Token temporal |
| GET | `/auth/verification-status` | Estado de verificación de email | Token JWT |

#### Ejemplos de Request/Response

**Login:**
```json
POST /api/auth/login
{
  "identifier": "user@example.com",
  "password": "password123"
}

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```
```

**Registro:**
```json
POST /api/auth/register
{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "password123"
}
```

**Reset de Contraseña:**

**1. Solicitar reset:**
```json
POST /api/auth/forgot-password
{
  "email": "user@example.com"
}

Response:
```json
{
  "message": "Se ha enviado un email con las instrucciones para resetear tu contraseña"
}
```

**2. URL enviada en el email:**
```
http://localhost:5173/reset-password?<token>
```
*Nota: El token se incluye directamente en la URL sin parámetro nombrado.*

**3. Resetear contraseña:**
```json
POST /api/auth/reset-password
{
  "token": "uuid-del-token",
  "newPassword": "nueva_password_123"
}

Response:
```json
{
  "message": "Contraseña actualizada exitosamente"
}
```

### Productos (`/products`)

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| GET | `/products` | Listar productos paginados | No requerida |
| GET | `/products/{id}` | Obtener producto por ID | No requerida |
| GET | `/products/search` | Buscar productos | No requerida |
| GET | `/products/category/{categoryId}` | Productos por categoría | No requerida |
| GET | `/products/featured` | Productos destacados | No requerida |
| POST | `/products` | Crear producto | Admin |
| PUT | `/products/{id}` | Actualizar producto | Admin |
| DELETE | `/products/{id}` | Eliminar producto | Admin |

#### Parámetros de Query para `/products`:
- `page` (int): Página (default: 0)
- `size` (int): Tamaño de página (default: 20)
- `sort` (string): Campo de ordenamiento (default: "name")
- `direction` (string): Dirección (ASC/DESC, default: "ASC")

#### Ejemplo de búsqueda:
```
GET /api/products/search?q=laptop&page=0&size=10
```

### Categorías (`/categories`)

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| GET | `/categories` | Listar todas las categorías | No requerida |
| GET | `/categories/root` | Categorías raíz | No requerida |
| GET | `/categories/{id}/subcategories` | Subcategorías | No requerida |
| POST | `/categories` | Crear categoría | Admin |
| PUT | `/categories/{id}` | Actualizar categoría | Admin |
| DELETE | `/categories/{id}` | Eliminar categoría | Admin |

### Órdenes (`/orders`)

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| GET | `/orders` | Listar órdenes del usuario | User |
| GET | `/orders/{id}` | Obtener orden por ID | User/Admin |
| POST | `/orders` | Crear nueva orden | User |
| PUT | `/orders/{id}/status` | Actualizar estado | Admin |
| GET | `/orders/admin/all` | Listar todas las órdenes | Admin |

### Carrito de Compras (`/cart`)

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| GET | `/cart` | Obtener carrito del usuario | User |
| POST | `/cart/items` | Agregar item al carrito | User |
| PUT | `/cart/items/{itemId}` | Actualizar cantidad | User |
| DELETE | `/cart/items/{itemId}` | Remover item | User |
| DELETE | `/cart` | Vaciar carrito | User |

### Pagos (`/payments`)

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/payments/create-preference` | Crear preferencia de pago | User |
| GET | `/payments/transaction/{id}` | Obtener transacción | User/Admin |
| POST | `/payments/webhook` | Webhook de MercadoPago | No requerida |

### Auditoría (`/audit`) - Solo Admin

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| GET | `/audit/logs` | Listar logs de auditoría | Admin |
| GET | `/audit/logs/{id}` | Obtener log específico | Admin |
| GET | `/audit/stats` | Estadísticas de auditoría | Admin |

#### Parámetros de Query para `/audit/logs`:
- `page` (int): Página (default: 0)
- `size` (int): Tamaño (default: 20)
- `sort` (string): Campo (default: "timestamp")
- `direction` (string): Dirección (default: "DESC")
- `userId` (Long): Filtrar por usuario
- `action` (string): Filtrar por acción
- `entityType` (string): Filtrar por tipo de entidad

### Email (`/email`)

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/email/verify` | Verificar email | Token temporal |
| POST | `/email/resend-verification` | Reenviar verificación | User |

## Seguridad

- **Autenticación JWT**: Tokens de acceso con expiración configurable
- **Roles**: USER y ADMIN con permisos granulares
- **Protección de endpoints**: `@PreAuthorize` annotations
- **CORS**: Configurado para desarrollo con orígenes permitidos desde variable de entorno `CORS_ALLOWED_ORIGINS`
- **Auditoría**: Logging completo de todas las operaciones
- **Rate Limiting**: Protección contra abuso de endpoints públicos

### Headers Requeridos
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

### Manejo de Errores

La API retorna errores en formato consistente:

```json
{
  "timestamp": "2024-01-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/login",
  "errors": [
    {
      "field": "email",
      "message": "Email is required"
    }
  ]
}
```

## Instalación y Ejecución

### Prerrequisitos
- JDK 17
- PostgreSQL 15+
- Maven 3.8+

### Configuración
1. **Crear bases de datos PostgreSQL**
   
   **Opción A - Script automático:**
   ```bash
   # Ejecutar el script incluido
   psql -U postgres -f setup-databases.sql
   ```
   
   **Opción B - Manual:**
   ```sql
   -- Crear usuario con contraseña 1234
   CREATE USER postgres WITH PASSWORD '1234';
   
   -- Crear bases de datos
   CREATE DATABASE ecommerce OWNER postgres;
   
   ```

3. **Poblar con Datos de Prueba Completos (Recomendado para Desarrollo)**
   
   ```bash
   # Ejecutar script de datos completos
   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d ecommerce -f insert-test-data-complete.sql
   ```
   
   **Datos incluidos:**
   - 👥 **10 usuarios** (3 admins + 7 personajes famosos)
   - 📦 **30 productos** con imágenes y descripciones detalladas
   - ⭐ **60 reseñas** con ratings variados
   - 📂 **6 categorías** principales
   - 🔧 **Sistema completo de atributos y variantes**
   - 🏠 **Direcciones temáticas** para testing

2. **Configurar variables de entorno**
   - Copia el archivo `.env.example` al archivo `.env`
   - Configura las variables según tu entorno
   - Elige el perfil activo modificando `SPRING_PROFILES_ACTIVE`

3. **Ejecutar la aplicación**
   ```bash
   # Para desarrollo (base de datos: ecommerce_dev)
   mvn spring-boot:run -Dspring.profiles.active=dev

   # Para testing (base de datos: ecommerce_test)
   mvn spring-boot:run -Dspring.profiles.active=test

   # Para producción (base de datos: ecommerce_prod)
   mvn spring-boot:run -Dspring.profiles.active=prod
   ```

   **O cambiar el perfil en el archivo `.env`:**
   ```bash
   # En .env cambiar:
   SPRING_PROFILES_ACTIVE=dev    # usa ecommerce_dev
   SPRING_PROFILES_ACTIVE=test   # usa ecommerce_test
   SPRING_PROFILES_ACTIVE=prod   # usa ecommerce_prod

   # Luego ejecutar:
   mvn spring-boot:run
   ```

### Variables de Entorno

#### Archivo .env
```bash
# Base de datos (configurada por perfil)
# dev: ecommerce_dev, test: ecommerce_test, prod: ecommerce_prod
# Usuario: postgres, Password: 1234

# Database Configuration by Profile
DB_DEV_URL=jdbc:postgresql://localhost:5432/ecommerce_dev
DB_DEV_USERNAME=postgres
DB_DEV_PASSWORD=1234
DB_DEV_DRIVER=org.postgresql.Driver

DB_TEST_URL=jdbc:postgresql://localhost:5432/ecommerce_test
DB_TEST_USERNAME=postgres
DB_TEST_PASSWORD=1234
DB_TEST_DRIVER=org.postgresql.Driver

DB_PROD_URL=jdbc:postgresql://localhost:5432/ecommerce_prod
DB_PROD_USERNAME=postgres
DB_PROD_PASSWORD=1234
DB_PROD_DRIVER=org.postgresql.Driver

# Application
app.base-url=http://localhost:8081
app.frontend-url=http://localhost:3000
app.api.verify-email-path=/api/auth/verify-email
app.frontend.login-path=/login
app.email.verification.expiry-hours=24
app.email.from=noreply@ecommerce.com

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001,http://127.0.0.1:3000,http://127.0.0.1:3001,https://localhost:3000,https://localhost:3001

# Hibernate/JPA
HIBERNATE_DDL_AUTO=update
HIBERNATE_SHOW_SQL=true
HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
HIBERNATE_FORMAT_SQL=true
SQL_INIT_MODE=never

# Email SMTP
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password_here
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_SSL_TRUST=smtp.gmail.com
MAIL_DEBUG=true

# JWT
JWT_SECRET=development-secret-key-for-jwt-tokens-very-long-and-secure-key-for-development-minimum-256-bits
JWT_EXPIRATION=86400000

# Server
SERVER_PORT=8081
SERVER_CONTEXT_PATH=/api

# Spring Profiles
# Opciones: dev, test, prod
# Cada perfil usa su propia base de datos PostgreSQL con password 1234
SPRING_PROFILES_ACTIVE=dev

# Swagger/OpenAPI
SWAGGER_API_DOCS_PATH=/v3/api-docs
SWAGGER_UI_PATH=/swagger-ui.html
SWAGGER_UI_ENABLED=true

# MercadoPago
MP_ACCESS_TOKEN=TEST_ACCESS_TOKEN
MP_PUBLIC_KEY=TEST_PUBLIC_KEY
MP_WEBHOOK_SECRET=test_webhook_secret
MP_WEBHOOK_URL=http://localhost:8081/api/payments/webhook
MP_SUCCESS_URL=http://localhost:8081/payment/success
MP_FAILURE_URL=http://localhost:8081/payment/failure
MP_PENDING_URL=http://localhost:8081/payment/pending

# Cloudinary
CLOUDINARY_CLOUD_NAME=test_cloud
CLOUDINARY_API_KEY=test_api_key
CLOUDINARY_API_SECRET=test_api_secret
CLOUDINARY_FOLDER_PRODUCTS=ecommerce/products
CLOUDINARY_FOLDER_USERS=ecommerce/users
CLOUDINARY_FOLDER_CATEGORIES=ecommerce/categories
CLOUDINARY_SECURE=true

# Logging
LOGGING_LEVEL_COM_ECOMMERCE=DEBUG
LOGGING_LEVEL_COM_ECOMMERCE_SERVICE=DEBUG
LOGGING_LEVEL_COM_ECOMMERCE_CONTROLLER=INFO
LOGGING_LEVEL_COM_ECOMMERCE_EXCEPTION=WARN
LOGGING_LEVEL_COM_ECOMMERCE_CONFIG=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG
LOGGING_LEVEL_ORG_HIBERNATE_SQL=DEBUG
LOGGING_LEVEL_ORG_HIBERNATE_TYPE=TRACE
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_MAIL=DEBUG
LOGGING_LEVEL_ROOT=INFO
LOGGING_PATTERN_CONSOLE=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
LOGGING_PATTERN_FILE=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{userId:-anonymous}] [%X{sessionId:-no-session}] [%X{requestId:-no-request}] %logger{36} - %msg%n

# Management/Actuator
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when-authorized
```

**Nota**: Este archivo `.env` contiene todas las configuraciones necesarias. Las variables están organizadas por funcionalidad y cada perfil de Spring Boot usa sus propias variables de base de datos específicas.

**Nota**: Este archivo `.env` contiene todas las configuraciones necesarias. Modifica las variables según tu entorno local.

### Perfiles de Spring Boot

El proyecto incluye diferentes perfiles de configuración con archivos específicos:

- **`dev`** (`application-dev.yml`): 
  - ✅ Base de datos: `ecommerce_dev`
  - ✅ Logs detallados activados (DEBUG)
  - ✅ Hot reload habilitado
  - ✅ `ddl-auto: update`
  - ✅ SQL queries visibles en logs
  - ✅ Health checks con detalles completos

- **`test`** (`application-test.yml`): 
  - ✅ Base de datos: `ecommerce_test`
  - ✅ Configuración optimizada para testing
  - ✅ `ddl-auto: create-drop`
  - ✅ Logs reducidos (solo WARN/ERROR)
  - ✅ Health checks deshabilitados

- **`prod`** (`application-prod.yml`): 
  - ✅ Base de datos: `ecommerce_prod`
  - ✅ Configuración optimizada para performance
  - ✅ `ddl-auto: validate`
  - ✅ Logs de producción (INFO/WARN)
  - ✅ Información de errores limitada por seguridad

**Todos los perfiles usan:**
- ✅ PostgreSQL como base de datos
- ✅ Usuario: `postgres`
- ✅ Contraseña: `1234`
- ✅ Puerto: `5432`

**Para cambiar de perfil:**

1. **Opción 1 - Archivo .env (recomendado):**
   ```bash
   # En .env cambiar:
   SPRING_PROFILES_ACTIVE=dev    # Base: ecommerce_dev
   SPRING_PROFILES_ACTIVE=test   # Base: ecommerce_test
   SPRING_PROFILES_ACTIVE=prod   # Base: ecommerce_prod
   ```

2. **Opción 2 - Parámetro de ejecución:**
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=dev
   mvn spring-boot:run -Dspring.profiles.active=test
   mvn spring-boot:run -Dspring.profiles.active=prod
   ```

3. **Opción 3 - Variable del sistema:**
   ```bash
   SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
   ```

## Documentación API

- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **API Docs**: `http://localhost:8081/v3/api-docs`
- **Swagger con contexto API**: `http://localhost:8081/api/swagger-ui.html`

## Testing

```bash
# Ejecutar tests unitarios
mvn test

# Ejecutar tests de integración
mvn verify

# Ejecutar con cobertura
mvn test jacoco:report
```

## Despliegue

### Railway
El backend está desplegado en Railway, una plataforma que facilita el despliegue de aplicaciones Spring Boot.

URL de producción: [https://testrepo-production-3e94.up.railway.app](https://testrepo-production-3e94.up.railway.app)

## Monitoreo y Logs

- **Logs de aplicación**: Configurados con Logback
- **Auditoría completa**: Todas las operaciones críticas se registran
- **Métricas**: Endpoints de actuator disponibles en `/actuator/*`
- **Health checks**: `/actuator/health`

## 🔄 Cambios Recientes (Octubre 2025)

### Correcciones de Base de Datos y Productos
- ✅ **Configuración Hibernate Dev**: Cambiado `ddl-auto` de `create` a `validate` para preservar datos entre reinicios
- ✅ **Campo Redundante Removido**: Eliminado campo `active` de entidad `Product` (se usa solo `status` enum)
- ✅ **Queries Actualizadas**: Removidas referencias a `p.active` en `ProductRepository`
- ✅ **Campo Featured Order**: Agregado `featuredOrder` a `Product` para ordenar productos destacados consistentemente
  - Los productos destacados ahora se ordenan por `featuredOrder` en lugar de ID
  - Evita que se vayan al final cuando son editados
  - Compatible con edición en Admin Panel

### Modelos y DTOs Actualizados
- `ProductRequest`: Nuevo campo `featuredOrder` (entero, opcional)
- `ProductServiceImpl`: Lógica actualizada para manejar `featuredOrder` en creación y actualización
- `ProductRepository`: Queries mejoradas con ordenamiento explícito

### Migraciones de Base de Datos
- Ejecutar: `backend/src/main/resources/migration-featured-order.sql`
  ```sql
  ALTER TABLE products ADD COLUMN featured_order INTEGER DEFAULT NULL;
  CREATE INDEX idx_featured_order ON products(featured, featured_order ASC, id DESC) 
  WHERE featured = true AND deleted_at IS NULL;
  ```

## Próximas Funcionalidades

- [x] Sistema de autenticación JWT
- [x] Gestión de productos y categorías
- [x] Carrito de compras persistente
- [x] Sistema de órdenes
- [x] Integración con MercadoPago
- [x] Gestión de imágenes con Cloudinary
- [x] Sistema de reseñas y calificaciones
- [x] Panel administrativo completo
- [x] Sistema de auditoría completo
- [x] Verificación de email
- [x] **Datos de Prueba Completos** (30 productos, 60 reseñas, 10 usuarios)
- [x] Sistema de atributos y variantes de producto
- [ ] Tests unitarios e integración
- [ ] CI/CD con GitHub Actions
- [ ] Notificaciones push
- [ ] Sistema de cupones/descuentos

## Equipo de Desarrollo

Proyecto integrador del 4° semestre de Tecnicatura en Programación.

## Licencia

Este proyecto es de uso educativo.
