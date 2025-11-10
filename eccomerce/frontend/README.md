# E-commerce Frontend

Frontend de la plataforma de comercio electrónico, desarrollado con React, Vite, TypeScript, Zustand, Axios y TailwindCSS. Incluye un sistema avanzado de carrito híbrido que combina localStorage para UX instantánea con persistencia en base de datos.

## 🌐 URLs de Producción

- **Aplicación Frontend**: [https://test-repo-rosy.vercel.app](https://test-repo-rosy.vercel.app)
- **Panel de Administración**: [https://test-repo-rosy.vercel.app/admin](https://test-repo-rosy.vercel.app/admin)

## 👤 Guía para Usuarios

Este frontend implementa todas las interfaces de usuario descritas en el [Manual de Usuario](../manual-usuario-ecommerce.md). Los usuarios pueden:

- **Crear y gestionar cuentas**: Registro, login, perfil, recuperación de contraseñas
- **Navegar por productos**: Explorar catálogo, búsqueda, filtros, ver detalles
- **Gestionar carrito**: Añadir productos, ajustar cantidades, checkout
- **Realizar pagos**: Integración segura con MercadoPago
- **Gestionar pedidos**: Ver historial y estado de pedidos, reintentar pagos pendientes
- **Personalizar experiencia**: Cambiar entre tema claro y oscuro

Para instrucciones detalladas sobre cada funcionalidad, consulta el [Manual de Usuario](../manual-usuario-ecommerce.md) o la [versión PDF](../manual-usuario-ecommerce.pdf).

## 📊 Datos de Prueba Disponibles

### Script Completo de Datos de Prueba
Para desarrollo y testing, el backend incluye un script completo que pobla la base de datos con datos realistas:

```bash
# Ejecutar desde el directorio backend/
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d ecommerce -f insert-test-data-complete.sql
```

### 👥 Usuarios de Prueba
- **Administradores** (password: `1234`):
  - `admin1@example.com` / `admin2@example.com` / `admin3@example.com`
- **Usuarios Famosos** (password: `1234`):
  - `mickey@disney.com` (Mickey Mouse)
  - `superman@dc.com` (Superman)
  - `harry@hogwarts.com` (Harry Potter)
  - `luke@starwars.com` (Luke Skywalker)
  - `frodo@middleearth.com` (Frodo Baggins)
  - `wonderwoman@dc.com` (Wonder Woman)
  - `ironman@marvel.com` (Iron Man)

### 📦 Productos Disponibles (30 total)
- **Electrónicos**: iPhone 15 Pro, MacBook Air M3, Samsung Galaxy S24, Sony WH-1000XM5, iPad Pro
- **Ropa**: Polo Lacoste, Jeans Levi's, Vestido de Noche, Nike Air Max, Chaqueta de Cuero
- **Libros**: Señor de los Anillos, Harry Potter, 1984, Cien Años de Soledad, Código Da Vinci
- **Hogar**: Cafetera Espresso, Juego de Sábanas, Lámpara de Mesa, Set de Ollas, Aspiradora Robot
- **Deportes**: Bicicleta de Montaña, Pelota de Fútbol, Mancuernas, Raqueta de Tenis, Colchoneta Yoga
- **Belleza**: Perfume Chanel No.5, Crema Hidratante La Mer, Paleta de Sombras, Cepillo Eléctrico, Aceite Argán

### ⭐ Sistema de Reseñas
Cada producto incluye entre 7-10 reseñas con ratings variados (3-5 estrellas) escritas por los usuarios famosos, creando un catálogo realista para testing.

### 🔧 Funcionalidades para Testing
- **Carrito Híbrido**: Prueba operaciones instantáneas y sincronización
- **Sistema de Reviews**: Verifica display de ratings y comentarios
- **Búsqueda y Filtros**: Testea catálogo con datos reales
- **Panel Admin**: Gestiona productos y usuarios con datos poblados
- **Checkout**: Procesa pagos con productos variados

## Tecnologías

- **React 18** - Framework UI con hooks modernos
- **TypeScript** - Tipado estático para mayor robustez
- **Vite** - Herramienta de build rápida y dev server optimizado
- **TailwindCSS** - Framework CSS utilitario con diseño personalizado
- **Zustand** - Gestión de estado global ligera y eficiente
- **Axios** - Cliente HTTP con interceptores de autenticación
- **React Router** - Enrutamiento SPA con navegación protegida
- **React Hook Form** - Manejo de formularios con validación
- **React Hot Toast** - Sistema de notificaciones moderno
- **Headless UI** - Componentes accesibles y personalizables
- **Heroicons** - Iconografía consistente y moderna

## 🛒 Sistema de Carrito Híbrido

### Arquitectura del Carrito
El sistema combina las mejores prácticas de UX y persistencia:

```
Usuario → localStorage (instantáneo) ↔ API Backend → Base de datos
                    ↓
              Sincronización automática
```

### Funcionalidades del Carrito
- **Operaciones Instantáneas**: Agregar, remover y actualizar productos sin delays
- **Persistencia Automática**: Los datos se guardan tanto local como en servidor
- **Sesiones de Invitados**: Carritos persistentes sin necesidad de registro
- **Sincronización Multi-dispositivo**: Acceso al carrito desde cualquier dispositivo
- **Recuperación Inteligente**: Fallback automático a datos locales si falla el backend
- **Transferencia Seamless**: Carritos de invitados se fusionan al autenticarse

### Implementación Técnica
- **Zustand Store**: Gestión centralizada del estado del carrito
- **localStorage**: Cache local con expiración de 24h
- **Session Management**: IDs únicos para sesiones de invitados
- **Optimistic Updates**: UI se actualiza inmediatamente, sincronización en background
- **Error Handling**: Graceful degradation con fallback a datos locales

## 🖼️ Componente de Imagen

### Descripción
Componente React optimizado para mostrar imágenes públicas de Cloudinary con manejo de errores.

### Características
- **Manejo Simplificado**: Acceso directo a imágenes públicas de Cloudinary
- **Carga Optimizada**: Transiciones suaves y manejo de estados de carga
- **Manejo de Errores**: Fallback elegante con imágenes placeholder
- **Responsive**: Adaptación automática a diferentes tamaños de pantalla

### Uso Básico
```tsx
import SignedImage from '@/components/SignedImage';

// Imagen pública con manejo de errores
<SignedImage
  src="https://res.cloudinary.com/.../products/image.jpg"
  alt="Producto"
  className="w-full h-48 object-cover"
/>

// Con imagen de fallback personalizada
<SignedImage
  src={product.imageUrl}
  alt={product.name}
  className="rounded-lg shadow-md"
  fallbackSrc="/custom-placeholder.jpg"
/>
```

### Funcionamiento Interno
1. **Carga Directa**: Utiliza las URLs públicas de Cloudinary directamente
2. **Manejo de Estados**: Muestra indicadores de carga con transiciones suaves
3. **Error Handling**: Detecta errores de carga y aplica imagen de fallback
4. **Optimización**: Gestiona la opacidad durante la carga para mejor UX

## 🔑 Páginas de Reset de Contraseña

### ForgotPassword (/forgot-password)
Página dedicada para que los usuarios soliciten el reset de su contraseña por email.

### Características
- **Validación de Email**: Verificación de formato y presencia
- **Feedback Visual**: Estados de carga y mensajes de éxito/error
- **Navegación Fluida**: Enlaces a login y opción de reenviar
- **Responsive**: Diseño adaptativo para móvil y desktop
- **Accesibilidad**: Labels apropiados y navegación por teclado

### ResetPassword (/reset-password)
Página segura para cambiar la contraseña usando el token recibido por email.

### Características
- **Validación de Token**: Verificación automática del token en URL
- **Validación de Contraseña**: Longitud mínima y coincidencia
- **Seguridad**: Token se valida antes de mostrar formulario
- **Redirección**: Automática a login después de éxito
- **Manejo de Errores**: Mensajes claros para tokens expirados/inválidos

### Integración con Backend
- **API Calls**: Uso de `authAPI.forgotPassword()` y `authAPI.resetPassword()`
- **Notificaciones**: Sistema de notificaciones integrado
- **Validación**: Validación completa en frontend y backend
- **Seguridad**: Tokens con expiración y verificación completa

## Variables de Entorno

Crea un archivo `.env` en la raíz del directorio frontend:

```env
VITE_API_BASE_URL=http://localhost:8081
VITE_APP_NAME=E-commerce Platform
VITE_APP_VERSION=1.0.0
```

## Inicio Rápido

### Prerrequisitos
- Backend corriendo en `http://localhost:8081`
- Base de datos poblada con datos de prueba

### Instalación
```bash
npm install
```

### Desarrollo
```bash
npm run dev
```
Inicia el servidor de desarrollo en `http://localhost:5173`

### Base de Datos con Datos de Prueba
Para tener una experiencia completa de desarrollo, asegúrate de poblar la base de datos:

```bash
# Desde el directorio backend/
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d ecommerce -f insert-test-data-complete.sql
```

**¿Qué incluye?**
- 👥 **10 usuarios** (3 admins + 7 personajes famosos)
- 📦 **30 productos** con imágenes y descripciones
- ⭐ **60 reseñas** con ratings variados
- 🛒 **Sistema completo** listo para testing del carrito híbrido

### Producción
```bash
# Build optimizado
npm run build

# Preview del build
npm run preview

# Despliegue en Vercel
El frontend está desplegado en Vercel, una plataforma gratuita que facilita el despliegue de aplicaciones React.
URL de producción: [https://test-repo-rosy.vercel.app](https://test-repo-rosy.vercel.app)
```

### Testing
```bash
# Ejecutar tests
npm run test

# Tests con coverage
npm run test:coverage
```

## Estructura del Proyecto

```
src/
├── components/          # Componentes reutilizables
│   ├── Footer.tsx      # Pie de página con enlaces
│   ├── Navbar.tsx      # Barra de navegación principal
│   ├── ProtectedRoute.tsx # Componente de ruta protegida
│   ├── MercadoPagoWallet.tsx # Integración MercadoPago
│   ├── ScrollMemory.tsx # Memoria de scroll por página
│   └── ScrollToTop.tsx # Botón de scroll al inicio
├── contexts/           # Contextos de React
│   └── ThemeContext.tsx # Contexto de tema oscuro/claro
├── pages/              # Páginas principales de la aplicación
│   ├── Home.tsx        # Página de inicio con productos destacados
│   ├── Products.tsx    # Catálogo completo con filtros
│   ├── ProductDetail.tsx # Vista detallada con reseñas
│   ├── Cart.tsx        # Gestión avanzada del carrito
│   ├── Checkout.tsx    # Proceso de pago con MercadoPago
│   ├── Orders.tsx      # Historial de pedidos del usuario
│   ├── Profile.tsx     # Gestión del perfil y configuración
│   ├── ProfileEdit.tsx # Edición de perfil de usuario
│   ├── Login.tsx       # Inicio de sesión con validación
│   ├── Register.tsx    # Registro con verificación email
│   ├── Admin.tsx       # Panel administrativo completo
│   ├── SearchResults.tsx # Resultados de búsqueda inteligente
│   ├── Success.tsx     # Página de éxito de pago
│   ├── Failure.tsx     # Página de fallo de pago
│   ├── Pending.tsx     # Página de pago pendiente
│   └── Categories.tsx  # Exploración de categorías
├── services/           # Servicios y configuración API
│   └── api.ts          # Configuración Axios con interceptores
├── stores/             # Stores de Zustand para estado global
│   ├── cartStore.ts    # 🛒 Carrito híbrido avanzado
│   ├── productStore.ts # Gestión de productos y catálogo
│   ├── profileStore.ts # Estado del perfil de usuario
│   └── userStore.ts    # Estado de autenticación
├── index.css           # Estilos globales con Tailwind
├── main.tsx           # Punto de entrada de la aplicación
└── App.tsx            # Componente raíz con routing
```

## Páginas Principales

### 🏠 Home
- Productos destacados y promociones
- Categorías principales con navegación rápida
- Búsqueda global desde la barra superior
- **Datos de prueba**: Productos destacados disponibles inmediatamente

### 🛍️ Products & ProductDetail
- Catálogo completo con filtros avanzados (precio, categoría, rating)
- Vista detallada con galería de imágenes
- Sistema de reseñas y calificaciones
- Información completa del producto
- **Datos de prueba**: 30 productos con 60 reseñas realistas

### ⭐ Sistema de Reseñas
- Las reseñas se publican inmediatamente después de ser creadas (APPROVED por defecto)
- Mostrador de rating promedio y distribución de estrellas
- Listado de comentarios con información del usuario
- **Panel Admin**: Tab dedicado para gestión de reseñas
  - Tabla con todas las reseñas (Producto, Usuario, Rating, Comentario, Estado)
  - Paginación para navegación eficiente
  - Botón "Eliminar" para ocultar reseñas
  - Modal de confirmación con campo obligatorio de razón (5-500 caracteres)
  - Validación automática y notificaciones de éxito/error
  - Recarga automática después de ocultar reseña

### 🛒 Cart (Sistema Híbrido)
- **Vista del carrito** con operaciones instantáneas
- **Gestión de cantidades** con actualización en tiempo real
- **Remoción de items** con confirmación
- **Cálculo automático** de subtotales y totales
- **Persistencia automática** local y en servidor
- **Datos de prueba**: Sistema listo para testing con productos disponibles

### 💳 Checkout
- Proceso de pago seguro con MercadoPago
- Validación completa de datos de envío
- Integración con wallet de MercadoPago
- Estados de pago: pendiente, aprobado, rechazado
- **Datos de prueba**: Usuarios con direcciones para testing completo

### 👤 Profile & Orders
- Gestión completa del perfil de usuario
- Historial de pedidos con estados detallados
- Información de envío y facturación
- **Datos de prueba**: Perfiles completos con direcciones temáticas

### 🔧 Admin Panel
- Dashboard con métricas y estadísticas
- Gestión completa de productos y categorías
- Administración de usuarios y pedidos
- Sistema de auditoría y logs
- **Datos de prueba**: Panel poblado con usuarios y productos para gestión

## API Integration

### Autenticación
- JWT tokens con refresh automático
- Interceptores Axios para manejo de tokens
- Rutas protegidas con React Router

### Carrito Híbrido
```typescript
// Ejemplo de uso del store de carrito
import { useCartStore } from '../stores/cartStore';

// Operaciones instantáneas con sincronización automática
const addToCart = (product) => {
  useCartStore.getState().addItem(product);
  // ✅ Se actualiza localStorage inmediatamente
  // ✅ Se sincroniza con backend en background
};
```

### Gestión de Estado
- Zustand para estado global eficiente
- Persistencia automática en localStorage
- Sincronización con backend cuando está disponible

## 🎨 Diseño y UX

### Tema Oscuro/Claro
- Toggle automático basado en preferencias del sistema
- Persistencia de preferencia del usuario
- Transiciones suaves entre temas

### Responsive Design
- Mobile-first approach con TailwindCSS
- Breakpoints optimizados para todos los dispositivos
- Navegación adaptativa (hamburger menu en móvil)

### Accesibilidad
- Componentes Headless UI para accesibilidad
- Navegación por teclado completa
- Contraste adecuado y tamaños de fuente legibles

## 🔧 Desarrollo

### Scripts Disponibles
```json
{
  "dev": "Vite dev server",
  "build": "Build optimizado para producción",
  "preview": "Preview del build de producción",
  "lint": "ESLint para calidad de código",
  "test": "Vitest para testing",
  "type-check": "Verificación de tipos TypeScript"
}
```

### Convenciones de Código
- TypeScript estricto para type safety
- ESLint + Prettier para consistencia
- Componentes funcionales con hooks
- Nombres descriptivos en español
- Comentarios en código complejo

## 🚀 Despliegue

### Build de Producción
```bash
npm run build
```
Genera archivos optimizados en `dist/`

### Variables de Producción
```env
VITE_API_BASE_URL=https://api.tu-dominio.com
VITE_APP_NAME=Tu E-commerce
```

### Hosting Recomendado
- **Vercel** - Para frontend estático
- **Netlify** - Con funciones serverless
- **Railway** - Full-stack con backend
- **AWS S3 + CloudFront** - Para alta escalabilidad

La aplicación se conecta al backend Spring Boot mediante Axios. Los endpoints principales incluyen:

- Autenticación: `/auth/login`, `/auth/register`
- Productos: `/products`, `/categories`
- Carrito: `/cart`
- Pedidos: `/orders`
- Pagos: `/payments`

## Estilos

Utiliza TailwindCSS para un diseño moderno y responsive. Incluye:

- Tema oscuro/claro
- Componentes personalizados
- Animaciones suaves
- Diseño mobile-first