-- =====================================================================
--  BASE DE DATOS COMPARTIDA (MySQL 8 - Aiven)
--  Versión en la nube de BD_Denuncias. Reúne en un solo archivo lo que
--  hacían los scripts 01, 02, 03 (ampliación de columnas) y 04 de SQL Server.
--
--  La ejecutan AMBAS aplicaciones al arrancar (spring.sql.init). Todas las
--  instrucciones usan IF NOT EXISTS: no borran nada y pueden ejecutarse
--  en cada arranque.
-- =====================================================================

-- 1. USUARIO: funcionarios policiales, fiscales y administradores
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario          BIGINT        NOT NULL AUTO_INCREMENT,
    codigo              VARCHAR(20)   NOT NULL,
    password_hash       VARCHAR(100)  NOT NULL,
    nombres             VARCHAR(80)   NOT NULL,
    apellidos           VARCHAR(80)   NOT NULL,
    dni                 VARCHAR(8)    NOT NULL,
    fecha_nacimiento    DATE          NULL,
    telefono            VARCHAR(15)   NULL,
    correo              VARCHAR(120)  NULL,
    direccion           VARCHAR(150)  NULL,
    contacto_emergencia VARCHAR(15)   NULL,
    institucion         VARCHAR(30)   NOT NULL,
    rol                 VARCHAR(10)   NOT NULL,
    rango               VARCHAR(60)   NULL,
    dependencia         VARCHAR(120)  NULL,
    habilitado          BOOLEAN       NOT NULL DEFAULT TRUE,
    foto                VARCHAR(100)  NULL,
    foto_tipo           VARCHAR(40)   NULL,
    ultimo_acceso       DATETIME(6)   NULL,
    fecha_creacion      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT PK_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT UQ_usuario_codigo UNIQUE (codigo),
    CONSTRAINT UQ_usuario_dni UNIQUE (dni),
    CONSTRAINT CK_usuario_rol CHECK (rol IN ('ADMIN', 'POLICIA', 'FISCAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. USUARIO_PERMISO: módulos habilitados para cada funcionario
CREATE TABLE IF NOT EXISTS usuario_permiso (
    id_usuario BIGINT      NOT NULL,
    modulo     VARCHAR(30) NOT NULL,
    CONSTRAINT PK_usuario_permiso PRIMARY KEY (id_usuario, modulo),
    CONSTRAINT FK_permiso_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. CIUDADANO (incluye las columnas del PIN del portal ciudadano)
CREATE TABLE IF NOT EXISTS ciudadano (
    id_ciudadano        BIGINT       NOT NULL AUTO_INCREMENT,
    nombres             VARCHAR(80)  NOT NULL,
    apellidos           VARCHAR(80)  NOT NULL,
    dni                 VARCHAR(8)   NOT NULL,
    correo              VARCHAR(120) NULL,
    telefono            VARCHAR(15)  NULL,
    pin_hash            VARCHAR(100) NULL,
    pin_intentos        INT          NOT NULL DEFAULT 0,
    pin_bloqueado_hasta DATETIME(6)  NULL,
    CONSTRAINT PK_ciudadano PRIMARY KEY (id_ciudadano),
    CONSTRAINT UQ_ciudadano_dni UNIQUE (dni)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. DENUNCIA (código ampliado a 20, despacho a 150 y funcionario responsable)
CREATE TABLE IF NOT EXISTS denuncia (
    id_denuncia            BIGINT       NOT NULL AUTO_INCREMENT,
    codigo                 VARCHAR(20)  NOT NULL,
    id_ciudadano           BIGINT       NOT NULL,
    tipo_delito            VARCHAR(60)  NOT NULL,
    descripcion            TEXT         NOT NULL,
    lugar_hechos           VARCHAR(200) NULL,
    fecha_registro         DATETIME(6)  NOT NULL,
    ultima_actualizacion   DATETIME(6)  NULL,
    estado_policial        VARCHAR(30)  NOT NULL,
    estado_fiscal          VARCHAR(30)  NOT NULL,
    carpeta_fiscal         VARCHAR(20)  NULL,
    despacho_fiscal        VARCHAR(150) NULL,
    fiscal_responsable     VARCHAR(120) NULL,
    id_usuario_responsable BIGINT       NULL,
    CONSTRAINT PK_denuncia PRIMARY KEY (id_denuncia),
    CONSTRAINT UQ_denuncia_codigo UNIQUE (codigo),
    CONSTRAINT FK_denuncia_ciudadano FOREIGN KEY (id_ciudadano) REFERENCES ciudadano (id_ciudadano),
    CONSTRAINT FK_denuncia_responsable FOREIGN KEY (id_usuario_responsable) REFERENCES usuario (id_usuario),
    INDEX IX_denuncia_fecha (fecha_registro),
    INDEX IX_denuncia_ciudadano (id_ciudadano)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. SEGUIMIENTO_DENUNCIA: historial de cambios de estado
CREATE TABLE IF NOT EXISTS seguimiento_denuncia (
    id_seguimiento BIGINT        NOT NULL AUTO_INCREMENT,
    id_denuncia    BIGINT        NOT NULL,
    codigo_usuario VARCHAR(20)   NOT NULL,
    perfil         VARCHAR(10)   NOT NULL,
    estado         VARCHAR(30)   NOT NULL,
    observacion    VARCHAR(1000) NULL,
    fecha_hora     DATETIME(6)   NOT NULL,
    CONSTRAINT PK_seguimiento PRIMARY KEY (id_seguimiento),
    CONSTRAINT FK_seguimiento_denuncia FOREIGN KEY (id_denuncia) REFERENCES denuncia (id_denuncia),
    CONSTRAINT FK_seguimiento_usuario FOREIGN KEY (codigo_usuario) REFERENCES usuario (codigo),
    INDEX IX_seguimiento_denuncia (id_denuncia, fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. ACTUACION: actuaciones policiales y fiscales
CREATE TABLE IF NOT EXISTS actuacion (
    id_actuacion    BIGINT       NOT NULL AUTO_INCREMENT,
    codigo          VARCHAR(12)  NOT NULL,
    codigo_denuncia VARCHAR(20)  NOT NULL,
    perfil          VARCHAR(10)  NOT NULL,
    descripcion     VARCHAR(200) NOT NULL,
    fecha           DATE         NOT NULL,
    estado          VARCHAR(30)  NOT NULL,
    CONSTRAINT PK_actuacion PRIMARY KEY (id_actuacion),
    CONSTRAINT UQ_actuacion_codigo UNIQUE (codigo),
    CONSTRAINT FK_actuacion_denuncia FOREIGN KEY (codigo_denuncia) REFERENCES denuncia (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. DOCUMENTO: documentos y evidencias (incluye las columnas del archivo adjunto)
CREATE TABLE IF NOT EXISTS documento (
    id_documento       BIGINT       NOT NULL AUTO_INCREMENT,
    codigo             VARCHAR(12)  NOT NULL,
    nombre             VARCHAR(150) NOT NULL,
    codigo_denuncia    VARCHAR(20)  NOT NULL,
    codigo_responsable VARCHAR(20)  NOT NULL,
    fecha              DATE         NOT NULL,
    estado             VARCHAR(30)  NOT NULL,
    descripcion        VARCHAR(500) NULL,
    archivo            VARCHAR(100) NULL,
    archivo_nombre     VARCHAR(200) NULL,
    archivo_tipo       VARCHAR(100) NULL,
    archivo_tamano     BIGINT       NULL,
    CONSTRAINT PK_documento PRIMARY KEY (id_documento),
    CONSTRAINT UQ_documento_codigo UNIQUE (codigo),
    CONSTRAINT FK_documento_denuncia FOREIGN KEY (codigo_denuncia) REFERENCES denuncia (codigo),
    CONSTRAINT FK_documento_usuario FOREIGN KEY (codigo_responsable) REFERENCES usuario (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. REMISION_FISCAL: informes que la PNP remite a la Fiscalía
CREATE TABLE IF NOT EXISTS remision_fiscal (
    id_remision      BIGINT      NOT NULL AUTO_INCREMENT,
    codigo_denuncia  VARCHAR(20) NOT NULL,
    carpeta_fiscal   VARCHAR(20) NULL,
    fecha_remision   DATE        NOT NULL,
    ultima_respuesta DATE        NULL,
    estado           VARCHAR(30) NOT NULL,
    CONSTRAINT PK_remision PRIMARY KEY (id_remision),
    CONSTRAINT FK_remision_denuncia FOREIGN KEY (codigo_denuncia) REFERENCES denuncia (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. DISPOSICION_FISCAL
CREATE TABLE IF NOT EXISTS disposicion_fiscal (
    id_disposicion     BIGINT       NOT NULL AUTO_INCREMENT,
    numero             VARCHAR(20)  NOT NULL,
    carpeta_fiscal     VARCHAR(20)  NOT NULL,
    tipo               VARCHAR(120) NOT NULL,
    fecha              DATE         NOT NULL,
    fiscal_responsable VARCHAR(120) NOT NULL,
    estado             VARCHAR(30)  NOT NULL,
    CONSTRAINT PK_disposicion PRIMARY KEY (id_disposicion),
    CONSTRAINT UQ_disposicion_numero UNIQUE (numero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. ACTIVIDAD_PROGRAMADA: agenda
CREATE TABLE IF NOT EXISTS actividad_programada (
    id_actividad BIGINT       NOT NULL AUTO_INCREMENT,
    perfil       VARCHAR(10)  NOT NULL,
    nombre       VARCHAR(150) NOT NULL,
    referencia   VARCHAR(20)  NOT NULL,
    lugar        VARCHAR(150) NULL,
    fecha_hora   DATETIME(6)  NOT NULL,
    CONSTRAINT PK_actividad PRIMARY KEY (id_actividad)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. REGISTRO_ACTIVIDAD: auditoría
CREATE TABLE IF NOT EXISTS registro_actividad (
    id_registro    BIGINT       NOT NULL AUTO_INCREMENT,
    fecha_hora     DATETIME(6)  NOT NULL,
    codigo_usuario VARCHAR(20)  NOT NULL,
    actividad      VARCHAR(250) NOT NULL,
    modulo         VARCHAR(40)  NOT NULL,
    resultado      VARCHAR(20)  NOT NULL,
    CONSTRAINT PK_registro_actividad PRIMARY KEY (id_registro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. COPIA_SEGURIDAD
CREATE TABLE IF NOT EXISTS copia_seguridad (
    id_copia       BIGINT      NOT NULL AUTO_INCREMENT,
    fecha_hora     DATETIME(6) NOT NULL,
    tamano_mb      INT         NOT NULL,
    estado         VARCHAR(20) NOT NULL,
    codigo_usuario VARCHAR(20) NOT NULL,
    CONSTRAINT PK_copia_seguridad PRIMARY KEY (id_copia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. CONVERSACION: chats
CREATE TABLE IF NOT EXISTS conversacion (
    id_conversacion BIGINT      NOT NULL AUTO_INCREMENT,
    tipo            VARCHAR(20) NOT NULL,
    referencia      VARCHAR(40) NOT NULL,
    id_usuario_a    BIGINT      NOT NULL,
    id_usuario_b    BIGINT      NULL,
    id_ciudadano    BIGINT      NULL,
    fecha_creacion  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT PK_conversacion PRIMARY KEY (id_conversacion),
    CONSTRAINT FK_conversacion_usuario_a FOREIGN KEY (id_usuario_a) REFERENCES usuario (id_usuario),
    CONSTRAINT FK_conversacion_usuario_b FOREIGN KEY (id_usuario_b) REFERENCES usuario (id_usuario),
    CONSTRAINT FK_conversacion_ciudadano FOREIGN KEY (id_ciudadano) REFERENCES ciudadano (id_ciudadano),
    CONSTRAINT CK_conversacion_tipo CHECK (tipo IN ('CIUDADANO', 'INTERINSTITUCIONAL', 'SOPORTE')),
    INDEX IX_conversacion_ciudadano (id_ciudadano, referencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. MENSAJE
CREATE TABLE IF NOT EXISTS mensaje (
    id_mensaje           BIGINT        NOT NULL AUTO_INCREMENT,
    id_conversacion      BIGINT        NOT NULL,
    id_usuario_remitente BIGINT        NULL,
    texto                VARCHAR(2000) NULL,
    adjunto_nombre       VARCHAR(200)  NULL,
    adjunto_tipo         VARCHAR(100)  NULL,
    adjunto_tamano       BIGINT        NULL,
    adjunto_archivo      VARCHAR(100)  NULL,
    fecha_hora           DATETIME(6)   NOT NULL,
    leido                BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_mensaje PRIMARY KEY (id_mensaje),
    CONSTRAINT FK_mensaje_conversacion FOREIGN KEY (id_conversacion) REFERENCES conversacion (id_conversacion),
    CONSTRAINT FK_mensaje_usuario FOREIGN KEY (id_usuario_remitente) REFERENCES usuario (id_usuario),
    INDEX IX_mensaje_conversacion (id_conversacion, fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. ARCHIVO_ALMACENADO (nueva en la versión en la nube)
--     En Render el disco de cada servicio es temporal y los dos prototipos
--     corren en servidores distintos, así que el contenido de los archivos
--     (adjuntos del chat, fotos de perfil y documentos) se guarda aquí en
--     lugar de la carpeta "uploads". Las demás tablas siguen guardando solo
--     el nombre del archivo, igual que antes.
CREATE TABLE IF NOT EXISTS archivo_almacenado (
    nombre         VARCHAR(100) NOT NULL,
    contenido      LONGBLOB     NOT NULL,
    fecha_creacion DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT PK_archivo_almacenado PRIMARY KEY (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
