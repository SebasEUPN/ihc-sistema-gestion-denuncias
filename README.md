# Sistema de Gestión y Seguimiento de Denuncias — Backend (Prototipo 1)
> **Fines didácticos.** Prototipo académico del curso de Interacción Humano-Computador (IHC).
> Los datos son ficticios.

## Versión en la nube (Render + Aiven)

Esta versión está preparada para publicarse en **Render** con una base **MySQL** gratuita de **Aiven**
(la misma base la comparten los dos prototipos).

Cambios respecto a la versión local con SQL Server:

- Driver `mysql-connector-j` en lugar de `mssql-jdbc`.
- La estructura está en `src/main/resources/db/schema-mysql.sql` y se crea sola al arrancar
  (reúne los scripts 01, 02 y 04, y la ampliación de columnas del 03). Los scripts de `database/`
  se conservan como referencia de la versión SQL Server.
- Los archivos (adjuntos del chat, fotos y documentos) se guardan en la tabla `archivo_almacenado`,
  porque el disco de Render es temporal y cada prototipo corre en un servidor distinto.
- La conexión se configura con variables de entorno:

| Variable | Ejemplo |
|---|---|
| `DB_URL` | `jdbc:mysql://HOST:PUERTO/defaultdb?sslMode=REQUIRED` |
| `DB_USER` | `avnadmin` |
| `DB_PASSWORD` | contraseña del servicio en Aiven |

Render construye la imagen con el `Dockerfile` incluido.

Proyecto académico IHC. Backend en **Spring Boot (Java)** con base de datos **SQL Server**,
más el frontend del prototipo separado en HTML, CSS y JS dentro de `static/`.

## 1. Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17 o superior |
| Maven | 3.9+ (o el que trae IntelliJ / NetBeans / Eclipse) |
| SQL Server | 2019 / 2022 / 2025 (se administra con SSMS 22) |

## 2. Puesta en marcha

1. **Crear la base de datos.** En SSMS abre `database/01_crear_base_datos.sql` y ejecútalo (F5).
   Crea la base `BD_Denuncias` con sus 14 tablas.
2. **Permitir la conexión desde Java** (solo la primera vez):
   - *SQL Server Configuration Manager* → Protocolos → habilitar **TCP/IP** (puerto 1433) y reiniciar el servicio.
   - En SSMS: clic derecho en el servidor → Propiedades → Seguridad → **Modo de autenticación de SQL Server y Windows**.
   - Tener un inicio de sesión SQL (por ejemplo `sa`) con contraseña.
3. **Configurar la conexión.** En `src/main/resources/application.properties` cambia
   `spring.datasource.username` y `spring.datasource.password`.
4. **Ejecutar.** Desde el IDE ejecuta `SistemaDenunciasApplication`, o por consola:
   ```
   mvn spring-boot:run
   ```
5. Abre **http://localhost:8080**

En el primer arranque la clase `DatosIniciales` carga los datos de demostración
(los mismos que tenía el prototipo HTML).

## 3. Usuarios de demostración

| Código | Perfil | Estado |
|---|---|---|
| `PNP-2045` | Funcionario policial | Activo |
| `PNP-2081` | Funcionario policial | Activo |
| `PNP-2094` | Funcionario policial | Activo |
| `FN-1023` | Funcionario fiscal | Activo |
| `FN-1044` | Funcionario fiscal | Deshabilitado (no puede ingresar) |
| `ADM-0001` | Administrador | Activo |

Todos usan la contraseña definida en `app.demo.password` (archivo `application.properties`).
En la base de datos se guarda cifrada con BCrypt.

## 4. Estructura del proyecto

```
sistema-denuncias/
├── pom.xml                              Dependencias (Maven)
├── database/01_crear_base_datos.sql     Script de SQL Server (base compartida por ambos prototipos)
└── src/main/
    ├── java/com/proyecto/denuncias/
    │   ├── SistemaDenunciasApplication.java   Clase principal
    │   ├── config/        Configuración web y carga de datos iniciales
    │   ├── security/      Manejo de la sesión e interceptor que protege /api/**
    │   ├── model/         Entidades JPA (una por tabla)
    │   ├── repository/    Acceso a datos (Spring Data JPA)
    │   ├── dto/           Objetos que viajan entre el frontend y el backend
    │   ├── service/       Lógica de negocio y validaciones
    │   ├── controller/    Controladores REST (rutas /api/...)
    │   ├── exception/     Errores uniformes en formato JSON
    │   └── util/          Validaciones de formularios
    └── resources/
        ├── application.properties
        └── static/                      Frontend
            ├── index.html
            ├── css/styles.css
            └── js/icons.js · api.js · app.js
```

Flujo de una petición: `app.js` → `api.js` (fetch) → **Controller** → **Service** → **Repository** → SQL Server.

## 5. Rutas de la API

| Método y ruta | Perfil | Descripción |
|---|---|---|
| `POST /api/auth/login` | público | Inicia sesión |
| `GET /api/auth/sesion` | público | Devuelve el usuario si ya hay sesión |
| `POST /api/auth/logout` | público | Cierra la sesión |
| `POST /api/auth/recuperar` | público | Verifica identidad (código + perfil + DNI) |
| `POST /api/auth/cambiar-password` | sesión o identidad verificada | Guarda la nueva contraseña |
| `GET /api/panel/indicadores` | todos | Tarjetas del panel |
| `GET /api/denuncias` | todos | Lista de denuncias / carpetas fiscales |
| `GET /api/denuncias/{codigo}` | todos | Detalle con historial |
| `PUT /api/denuncias/{codigo}` | policía, fiscal | Cambia estado y agrega observación |
| `GET /api/denuncias/reporte?desde=&hasta=` | todos | Denuncias por periodo |
| `GET /api/actuaciones` · `/documentos` · `/agenda` | policía, fiscal | Módulos de apoyo |
| `GET /api/remisiones` | policía | Seguimiento de Fiscalía |
| `GET /api/disposiciones` | fiscal | Disposiciones fiscales |
| `GET, POST /api/usuarios` | admin | Listar / registrar funcionarios |
| `PATCH /api/usuarios/{codigo}/estado` | admin | Habilitar / deshabilitar |
| `PUT /api/usuarios/{codigo}/permisos` | admin | Módulos habilitados |
| `GET /api/admin/auditoria` | admin | Registros de actividades |
| `GET, POST /api/admin/respaldos` | admin | Copias de seguridad (simuladas) |
| `GET, PUT /api/perfil` · `GET, POST /api/perfil/foto` | todos | Mi perfil |
| `GET /api/chat/conversaciones` | todos | Conversaciones y mensajes |
| `POST /api/chat/conversaciones/{id}/mensajes` · `/adjuntos` · `/leida` | todos | Enviar texto, archivo o marcar leído |
| `GET /api/chat/adjuntos/{idMensaje}` | participantes | Descargar adjunto |

## 6. Tablas pensadas para el Prototipo 2 (seguimiento por código)

- `denuncia.codigo` — código con el que el ciudadano consulta (ej. `D-000584`).
- `ciudadano` — datos del denunciante.
- `seguimiento_denuncia` — línea de tiempo de estados y observaciones.
- `conversacion` / `mensaje` — chat; un mensaje con `id_usuario_remitente = NULL` es del ciudadano.

## 7. Notas

- Si tu IDE no descarga Spring Boot 3.5.7, cambia en `pom.xml` la versión del `<parent>` por la 3.5.x más reciente.
- Los archivos subidos (adjuntos del chat y fotos) se guardan en la carpeta `uploads/`.
- Para reiniciar los datos: vuelve a ejecutar el script SQL y arranca de nuevo la aplicación.
