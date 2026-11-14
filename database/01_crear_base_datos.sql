/* =====================================================================
   BASE DE DATOS COMPARTIDA: BD_Denuncias
   Proyecto académico IHC
     - Prototipo 1: Sistema de Gestión y Seguimiento de Denuncias
                    (administrador, funcionario policial, funcionario fiscal)
     - Prototipo 2: Seguimiento de denuncia mediante código (ciudadano)

   Cómo usarlo:
     1. Abrir SQL Server Management Studio y conectarse al servidor.
     2. Archivo > Abrir > este script.
     3. Presionar F5 (Ejecutar).
   El script solo crea la ESTRUCTURA. Los datos de demostración los
   inserta la aplicación Spring Boot la primera vez que arranca
   (clase DatosIniciales), porque las contraseñas deben guardarse
   cifradas con BCrypt.
   ===================================================================== */

-- Crea la base de datos solo si todavía no existe
IF DB_ID(N'BD_Denuncias') IS NULL
    CREATE DATABASE BD_Denuncias;
GO

USE BD_Denuncias;
GO

/* ---------------------------------------------------------------------
   Se eliminan las tablas (de la más dependiente a la menos dependiente)
   para poder ejecutar el script varias veces durante el desarrollo.
   ¡CUIDADO! Esto borra todos los datos existentes.
   --------------------------------------------------------------------- */
DROP TABLE IF EXISTS mensaje;
DROP TABLE IF EXISTS conversacion;
DROP TABLE IF EXISTS copia_seguridad;
DROP TABLE IF EXISTS registro_actividad;
DROP TABLE IF EXISTS actividad_programada;
DROP TABLE IF EXISTS disposicion_fiscal;
DROP TABLE IF EXISTS remision_fiscal;
DROP TABLE IF EXISTS documento;
DROP TABLE IF EXISTS actuacion;
DROP TABLE IF EXISTS seguimiento_denuncia;
DROP TABLE IF EXISTS denuncia;
DROP TABLE IF EXISTS ciudadano;
DROP TABLE IF EXISTS usuario_permiso;
DROP TABLE IF EXISTS usuario;
GO

/* =====================================================================
   1. USUARIO: funcionarios policiales, fiscales y administradores
   ===================================================================== */
CREATE TABLE usuario (
    id_usuario          BIGINT IDENTITY(1,1) NOT NULL,          -- clave primaria autoincremental
    codigo              NVARCHAR(20)  NOT NULL,                 -- código institucional (PNP-2045, FN-1023, ADM-0001)
    password_hash       NVARCHAR(100) NOT NULL,                 -- contraseña cifrada con BCrypt (nunca en texto plano)
    nombres             NVARCHAR(80)  NOT NULL,
    apellidos           NVARCHAR(80)  NOT NULL,
    dni                 NVARCHAR(8)   NOT NULL,
    fecha_nacimiento    DATE          NULL,
    telefono            NVARCHAR(15)  NULL,
    correo              NVARCHAR(120) NULL,
    direccion           NVARCHAR(150) NULL,
    contacto_emergencia NVARCHAR(15)  NULL,
    institucion         NVARCHAR(30)  NOT NULL,                 -- PNP, Fiscalía o Administración
    rol                 NVARCHAR(10)  NOT NULL,                 -- ADMIN, POLICIA o FISCAL
    rango               NVARCHAR(60)  NULL,                     -- rango o cargo
    dependencia         NVARCHAR(120) NULL,                     -- comisaría, unidad o despacho fiscal
    habilitado          BIT           NOT NULL CONSTRAINT DF_usuario_habilitado DEFAULT 1,
    foto                NVARCHAR(100) NULL,                     -- nombre del archivo de la foto de perfil
    foto_tipo           NVARCHAR(40)  NULL,                     -- tipo de imagen (image/png, image/jpeg...)
    ultimo_acceso       DATETIME2     NULL,                     -- último inicio de sesión
    fecha_creacion      DATETIME2     NOT NULL CONSTRAINT DF_usuario_creacion DEFAULT SYSDATETIME(),
    CONSTRAINT PK_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT UQ_usuario_codigo UNIQUE (codigo),
    CONSTRAINT UQ_usuario_dni UNIQUE (dni),
    CONSTRAINT CK_usuario_rol CHECK (rol IN (N'ADMIN', N'POLICIA', N'FISCAL'))
);
GO

/* =====================================================================
   2. USUARIO_PERMISO: módulos habilitados para cada funcionario
      (denuncias, tracking, documents, reports, chat, schedule)
   ===================================================================== */
CREATE TABLE usuario_permiso (
    id_usuario BIGINT       NOT NULL,
    modulo     NVARCHAR(30) NOT NULL,
    CONSTRAINT PK_usuario_permiso PRIMARY KEY (id_usuario, modulo),
    CONSTRAINT FK_permiso_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
GO

/* =====================================================================
   3. CIUDADANO: persona que presenta la denuncia
      (el prototipo 2 la usará para el seguimiento por código)
   ===================================================================== */
CREATE TABLE ciudadano (
    id_ciudadano BIGINT IDENTITY(1,1) NOT NULL,
    nombres      NVARCHAR(80)  NOT NULL,
    apellidos    NVARCHAR(80)  NOT NULL,
    dni          NVARCHAR(8)   NOT NULL,
    correo       NVARCHAR(120) NULL,
    telefono     NVARCHAR(15)  NULL,
    CONSTRAINT PK_ciudadano PRIMARY KEY (id_ciudadano),
    CONSTRAINT UQ_ciudadano_dni UNIQUE (dni)
);
GO

/* =====================================================================
   4. DENUNCIA: expediente principal. Tiene un estado para la PNP y otro
      para la Fiscalía porque cada institución lleva su propio avance.
   ===================================================================== */
CREATE TABLE denuncia (
    id_denuncia          BIGINT IDENTITY(1,1) NOT NULL,
    codigo               NVARCHAR(12)  NOT NULL,                -- código de denuncia (D-000584): lo usa el ciudadano para el seguimiento
    id_ciudadano         BIGINT        NOT NULL,
    tipo_delito          NVARCHAR(60)  NOT NULL,                -- Hurto, Robo, Estafa...
    descripcion          NVARCHAR(MAX) NOT NULL,                -- descripción de los hechos
    lugar_hechos         NVARCHAR(200) NULL,
    fecha_registro       DATETIME2     NOT NULL,
    ultima_actualizacion DATETIME2     NULL,
    estado_policial      NVARCHAR(30)  NOT NULL,                -- En proceso, Por resolver, Resuelto...
    estado_fiscal        NVARCHAR(30)  NOT NULL,                -- En investigación, Diligencias, Con disposición, Archivado
    carpeta_fiscal       NVARCHAR(20)  NULL,                    -- CF-2026-00125
    despacho_fiscal      NVARCHAR(120) NULL,
    fiscal_responsable   NVARCHAR(120) NULL,
    CONSTRAINT PK_denuncia PRIMARY KEY (id_denuncia),
    CONSTRAINT UQ_denuncia_codigo UNIQUE (codigo),
    CONSTRAINT FK_denuncia_ciudadano FOREIGN KEY (id_ciudadano) REFERENCES ciudadano (id_ciudadano)
);
GO

/* =====================================================================
   5. SEGUIMIENTO_DENUNCIA: historial de cada cambio de estado u
      observación. Es la línea de tiempo que consultará el ciudadano.
   ===================================================================== */
CREATE TABLE seguimiento_denuncia (
    id_seguimiento BIGINT IDENTITY(1,1) NOT NULL,
    id_denuncia    BIGINT         NOT NULL,
    codigo_usuario NVARCHAR(20)   NOT NULL,                     -- funcionario que hizo el cambio
    perfil         NVARCHAR(10)   NOT NULL,                     -- POLICIA o FISCAL
    estado         NVARCHAR(30)   NOT NULL,                     -- estado que quedó registrado
    observacion    NVARCHAR(1000) NULL,
    fecha_hora     DATETIME2      NOT NULL,
    CONSTRAINT PK_seguimiento PRIMARY KEY (id_seguimiento),
    CONSTRAINT FK_seguimiento_denuncia FOREIGN KEY (id_denuncia) REFERENCES denuncia (id_denuncia),
    CONSTRAINT FK_seguimiento_usuario FOREIGN KEY (codigo_usuario) REFERENCES usuario (codigo)
);
GO

/* =====================================================================
   6. ACTUACION: actuaciones policiales (ACT-001) y fiscales (FIS-001)
   ===================================================================== */
CREATE TABLE actuacion (
    id_actuacion    BIGINT IDENTITY(1,1) NOT NULL,
    codigo          NVARCHAR(12)  NOT NULL,
    codigo_denuncia NVARCHAR(12)  NOT NULL,
    perfil          NVARCHAR(10)  NOT NULL,                     -- POLICIA o FISCAL
    descripcion     NVARCHAR(200) NOT NULL,
    fecha           DATE          NOT NULL,
    estado          NVARCHAR(30)  NOT NULL,                     -- Completada, En proceso, Programada, Pendiente
    CONSTRAINT PK_actuacion PRIMARY KEY (id_actuacion),
    CONSTRAINT UQ_actuacion_codigo UNIQUE (codigo),
    CONSTRAINT FK_actuacion_denuncia FOREIGN KEY (codigo_denuncia) REFERENCES denuncia (codigo)
);
GO

/* =====================================================================
   7. DOCUMENTO: documentos y evidencias asociados a una denuncia
   ===================================================================== */
CREATE TABLE documento (
    id_documento       BIGINT IDENTITY(1,1) NOT NULL,
    codigo             NVARCHAR(12)  NOT NULL,
    nombre             NVARCHAR(150) NOT NULL,
    codigo_denuncia    NVARCHAR(12)  NOT NULL,
    codigo_responsable NVARCHAR(20)  NOT NULL,                  -- funcionario que lo registró
    fecha              DATE          NOT NULL,
    estado             NVARCHAR(30)  NOT NULL,                  -- Validado, Pendiente, Remitido
    CONSTRAINT PK_documento PRIMARY KEY (id_documento),
    CONSTRAINT UQ_documento_codigo UNIQUE (codigo),
    CONSTRAINT FK_documento_denuncia FOREIGN KEY (codigo_denuncia) REFERENCES denuncia (codigo),
    CONSTRAINT FK_documento_usuario FOREIGN KEY (codigo_responsable) REFERENCES usuario (codigo)
);
GO

/* =====================================================================
   8. REMISION_FISCAL: informes que la PNP remite a la Fiscalía
      (módulos "Informes a Fiscalía" y "Seguimiento de Fiscalía")
   ===================================================================== */
CREATE TABLE remision_fiscal (
    id_remision      BIGINT IDENTITY(1,1) NOT NULL,
    codigo_denuncia  NVARCHAR(12) NOT NULL,
    carpeta_fiscal   NVARCHAR(20) NULL,                         -- NULL = por asignar
    fecha_remision   DATE         NOT NULL,
    ultima_respuesta DATE         NULL,                         -- NULL = sin respuesta todavía
    estado           NVARCHAR(30) NOT NULL,                     -- Recibido, Requerimiento, En espera, Pendiente
    CONSTRAINT PK_remision PRIMARY KEY (id_remision),
    CONSTRAINT FK_remision_denuncia FOREIGN KEY (codigo_denuncia) REFERENCES denuncia (codigo)
);
GO

/* =====================================================================
   9. DISPOSICION_FISCAL: disposiciones emitidas por la Fiscalía
   ===================================================================== */
CREATE TABLE disposicion_fiscal (
    id_disposicion     BIGINT IDENTITY(1,1) NOT NULL,
    numero             NVARCHAR(20)  NOT NULL,                  -- DISP-2026-0012
    carpeta_fiscal     NVARCHAR(20)  NOT NULL,
    tipo               NVARCHAR(120) NOT NULL,
    fecha              DATE          NOT NULL,
    fiscal_responsable NVARCHAR(120) NOT NULL,
    estado             NVARCHAR(30)  NOT NULL,                  -- Emitida, Notificada
    CONSTRAINT PK_disposicion PRIMARY KEY (id_disposicion),
    CONSTRAINT UQ_disposicion_numero UNIQUE (numero)
);
GO

/* =====================================================================
   10. ACTIVIDAD_PROGRAMADA: agenda (programación policial y diligencias fiscales)
   ===================================================================== */
CREATE TABLE actividad_programada (
    id_actividad BIGINT IDENTITY(1,1) NOT NULL,
    perfil       NVARCHAR(10)  NOT NULL,                        -- POLICIA o FISCAL
    nombre       NVARCHAR(150) NOT NULL,
    referencia   NVARCHAR(20)  NOT NULL,                        -- denuncia o carpeta fiscal relacionada
    lugar        NVARCHAR(150) NULL,
    fecha_hora   DATETIME2     NOT NULL,
    CONSTRAINT PK_actividad PRIMARY KEY (id_actividad)
);
GO

/* =====================================================================
   11. REGISTRO_ACTIVIDAD: auditoría de las operaciones de los usuarios
   ===================================================================== */
CREATE TABLE registro_actividad (
    id_registro    BIGINT IDENTITY(1,1) NOT NULL,
    fecha_hora     DATETIME2     NOT NULL,
    codigo_usuario NVARCHAR(20)  NOT NULL,
    actividad      NVARCHAR(250) NOT NULL,
    modulo         NVARCHAR(40)  NOT NULL,
    resultado      NVARCHAR(20)  NOT NULL,
    CONSTRAINT PK_registro_actividad PRIMARY KEY (id_registro)
);
GO

/* =====================================================================
   12. COPIA_SEGURIDAD: historial de respaldos (simulados en el prototipo)
   ===================================================================== */
CREATE TABLE copia_seguridad (
    id_copia       BIGINT IDENTITY(1,1) NOT NULL,
    fecha_hora     DATETIME2    NOT NULL,
    tamano_mb      INT          NOT NULL,
    estado         NVARCHAR(20) NOT NULL,
    codigo_usuario NVARCHAR(20) NOT NULL,
    CONSTRAINT PK_copia_seguridad PRIMARY KEY (id_copia)
);
GO

/* =====================================================================
   13. CONVERSACION: chat entre un funcionario y un ciudadano, entre
       policía y fiscal, o entre un funcionario y el administrador
   ===================================================================== */
CREATE TABLE conversacion (
    id_conversacion BIGINT IDENTITY(1,1) NOT NULL,
    tipo            NVARCHAR(20) NOT NULL,                      -- CIUDADANO, INTERINSTITUCIONAL o SOPORTE
    referencia      NVARCHAR(40) NOT NULL,                      -- denuncia relacionada o asunto
    id_usuario_a    BIGINT       NOT NULL,                      -- funcionario que participa
    id_usuario_b    BIGINT       NULL,                          -- segundo funcionario (NULL si es con un ciudadano)
    id_ciudadano    BIGINT       NULL,                          -- ciudadano (NULL si es entre funcionarios)
    fecha_creacion  DATETIME2    NOT NULL CONSTRAINT DF_conversacion_creacion DEFAULT SYSDATETIME(),
    CONSTRAINT PK_conversacion PRIMARY KEY (id_conversacion),
    CONSTRAINT FK_conversacion_usuario_a FOREIGN KEY (id_usuario_a) REFERENCES usuario (id_usuario),
    CONSTRAINT FK_conversacion_usuario_b FOREIGN KEY (id_usuario_b) REFERENCES usuario (id_usuario),
    CONSTRAINT FK_conversacion_ciudadano FOREIGN KEY (id_ciudadano) REFERENCES ciudadano (id_ciudadano),
    CONSTRAINT CK_conversacion_tipo CHECK (tipo IN (N'CIUDADANO', N'INTERINSTITUCIONAL', N'SOPORTE'))
);
GO

/* =====================================================================
   14. MENSAJE: mensajes de cada conversación (texto o archivo adjunto)
   ===================================================================== */
CREATE TABLE mensaje (
    id_mensaje           BIGINT IDENTITY(1,1) NOT NULL,
    id_conversacion      BIGINT         NOT NULL,
    id_usuario_remitente BIGINT         NULL,                   -- NULL = el mensaje lo envió el ciudadano
    texto                NVARCHAR(2000) NULL,
    adjunto_nombre       NVARCHAR(200)  NULL,                   -- nombre original del archivo
    adjunto_tipo         NVARCHAR(100)  NULL,                   -- tipo MIME (image/png, application/pdf...)
    adjunto_tamano       BIGINT         NULL,                   -- tamaño en bytes
    adjunto_archivo      NVARCHAR(100)  NULL,                   -- nombre con el que se guardó en la carpeta uploads
    fecha_hora           DATETIME2      NOT NULL,
    leido                BIT            NOT NULL CONSTRAINT DF_mensaje_leido DEFAULT 0,
    CONSTRAINT PK_mensaje PRIMARY KEY (id_mensaje),
    CONSTRAINT FK_mensaje_conversacion FOREIGN KEY (id_conversacion) REFERENCES conversacion (id_conversacion),
    CONSTRAINT FK_mensaje_usuario FOREIGN KEY (id_usuario_remitente) REFERENCES usuario (id_usuario)
);
GO

/* Índices para acelerar las búsquedas más frecuentes */
CREATE INDEX IX_denuncia_fecha ON denuncia (fecha_registro);
CREATE INDEX IX_seguimiento_denuncia ON seguimiento_denuncia (id_denuncia, fecha_hora);
CREATE INDEX IX_mensaje_conversacion ON mensaje (id_conversacion, fecha_hora);
GO

PRINT N'Base de datos BD_Denuncias creada correctamente. Ahora ejecuta la aplicación Spring Boot para cargar los datos de demostración.';
GO
