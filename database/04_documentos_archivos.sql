/* =====================================================================
   ARCHIVOS EN LOS DOCUMENTOS Y EVIDENCIAS (opcional)

   La aplicación aplica estos cambios sola al arrancar (clase MigracionBD),
   así que NO es obligatorio ejecutar este script. Se incluye para que la
   estructura quede documentada y por si prefieres aplicarla desde SSMS.

   Solo agrega columnas; no borra nada y puede ejecutarse varias veces.
   ===================================================================== */
USE BD_Denuncias;
GO

-- Breve descripción del contenido del documento
IF COL_LENGTH('documento', 'descripcion') IS NULL
    ALTER TABLE documento ADD descripcion NVARCHAR(500) NULL;
-- Nombre con el que el archivo se guardó en la carpeta "uploads"
IF COL_LENGTH('documento', 'archivo') IS NULL
    ALTER TABLE documento ADD archivo NVARCHAR(100) NULL;
-- Nombre original del archivo (el que ve el usuario al descargarlo)
IF COL_LENGTH('documento', 'archivo_nombre') IS NULL
    ALTER TABLE documento ADD archivo_nombre NVARCHAR(200) NULL;
-- Tipo de archivo: application/pdf, image/jpeg, video/mp4...
IF COL_LENGTH('documento', 'archivo_tipo') IS NULL
    ALTER TABLE documento ADD archivo_tipo NVARCHAR(100) NULL;
-- Tamaño en bytes
IF COL_LENGTH('documento', 'archivo_tamano') IS NULL
    ALTER TABLE documento ADD archivo_tamano BIGINT NULL;
GO
PRINT N'Columnas de archivos agregadas a la tabla documento.';
GO
