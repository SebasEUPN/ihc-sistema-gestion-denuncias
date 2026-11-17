package com.proyecto.denuncias.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Actualiza una base de datos creada con una versión anterior del esquema.
 * Se ejecuta PRIMERO al arrancar (@Order(0)), antes de cargar los datos de
 * ejemplo. Cada paso comprueba antes si hace falta (INFORMATION_SCHEMA de
 * PostgreSQL), así que puede ejecutarse en cada arranque sin cambiar nada la
 * segunda vez. En una base nueva no hace nada: db/schema.sql ya crea todo.
 *
 * Pasos:
 *  1. Columnas del archivo adjunto en "documento".
 *  2. Los datos propios de cada perfil salen de "usuario" y pasan a las
 *     tablas "policia", "fiscal" y "admin".
 *  3. "copia_seguridad" se relaciona con "admin".
 *  4. "actividad_programada", "registro_actividad" y "disposicion_fiscal"
 *     se relacionan con "policia" y "fiscal" (y la auditoría también con "admin").
 */
@Component
@Order(0)
public class MigracionBD implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionBD.class);

    private final JdbcTemplate jdbc;

    public MigracionBD(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional // todo o nada: si un paso falla, la base queda como estaba
    public void run(String... args) {
        // 1. Tabla documento: descripción y datos del archivo adjunto
        agregarColumna("documento", "descripcion", "VARCHAR(500) NULL");
        agregarColumna("documento", "archivo", "VARCHAR(100) NULL");
        agregarColumna("documento", "archivo_nombre", "VARCHAR(200) NULL");
        agregarColumna("documento", "archivo_tipo", "VARCHAR(100) NULL");
        agregarColumna("documento", "archivo_tamano", "BIGINT NULL");

        separarPerfiles();
        relacionarCopias();
        relacionarActividades();
        relacionarAuditoria();
        relacionarDisposiciones();
    }

    /** 2. usuario -> policia / fiscal / admin */
    private void separarPerfiles() {
        if (!existeColumna("usuario", "dependencia")) {
            return; // ya está separado
        }
        log.info("Migración: separando los datos de cada perfil en las tablas policia, fiscal y admin...");
        jdbc.update("INSERT INTO policia (id_usuario, rango, dependencia) "
                + "SELECT id_usuario, rango, COALESCE(NULLIF(TRIM(dependencia), ''), 'No registrada') FROM usuario "
                + "WHERE rol = 'POLICIA' ON CONFLICT (id_usuario) DO NOTHING");
        jdbc.update("INSERT INTO fiscal (id_usuario, cargo, despacho) "
                + "SELECT id_usuario, rango, COALESCE(NULLIF(TRIM(dependencia), ''), 'No registrada') FROM usuario "
                + "WHERE rol = 'FISCAL' ON CONFLICT (id_usuario) DO NOTHING");
        jdbc.update("INSERT INTO admin (id_usuario, cargo, oficina) "
                + "SELECT id_usuario, rango, COALESCE(NULLIF(TRIM(dependencia), ''), 'No registrada') FROM usuario "
                + "WHERE rol = 'ADMIN' ON CONFLICT (id_usuario) DO NOTHING");
        jdbc.execute("ALTER TABLE usuario DROP COLUMN IF EXISTS institucion, DROP COLUMN IF EXISTS rango, "
                + "DROP COLUMN IF EXISTS dependencia");
    }

    /** 3. copia_seguridad -> admin (y columnas del nuevo formulario) */
    private void relacionarCopias() {
        if (!existeColumna("copia_seguridad", "codigo_usuario")) {
            return;
        }
        log.info("Migración: relacionando copia_seguridad con admin...");
        agregarColumna("copia_seguridad", "id_admin", "BIGINT NULL");
        agregarColumna("copia_seguridad", "nombre", "VARCHAR(80) NULL");
        agregarColumna("copia_seguridad", "tipo", "VARCHAR(20) NOT NULL DEFAULT 'COMPLETA'");
        agregarColumna("copia_seguridad", "descripcion", "VARCHAR(300) NULL");
        agregarColumna("copia_seguridad", "origen", "VARCHAR(15) NOT NULL DEFAULT 'Manual'");
        agregarColumna("copia_seguridad", "tamano_bytes", "BIGINT NOT NULL DEFAULT 0");
        agregarColumna("copia_seguridad", "registros", "INT NOT NULL DEFAULT 0");
        agregarColumna("copia_seguridad", "archivo", "VARCHAR(100) NULL");
        // El administrador que hizo cada copia; si su código ya no existe, el primer administrador
        jdbc.update("UPDATE copia_seguridad c SET id_admin = a.id_usuario FROM usuario u "
                + "JOIN admin a ON a.id_usuario = u.id_usuario WHERE UPPER(u.codigo) = UPPER(c.codigo_usuario)");
        jdbc.update("UPDATE copia_seguridad SET id_admin = (SELECT MIN(id_usuario) FROM admin) WHERE id_admin IS NULL");
        // Las copias anteriores eran demostrativas (sin archivo): conservan su registro en el historial
        jdbc.update("UPDATE copia_seguridad SET nombre = 'Backup_' || LPAD(CAST(id_copia AS VARCHAR), 3, '0'), "
                + "descripcion = 'Registro anterior (sin archivo de respaldo)', "
                + "tamano_bytes = CAST(tamano_mb AS BIGINT) * 1048576, "
                + "estado = CASE WHEN estado = 'Correcto' THEN 'Completada' ELSE estado END WHERE nombre IS NULL");
        jdbc.execute("ALTER TABLE copia_seguridad ALTER COLUMN id_admin SET NOT NULL, ALTER COLUMN nombre SET NOT NULL, "
                + "DROP COLUMN codigo_usuario, DROP COLUMN tamano_mb");
        jdbc.execute("ALTER TABLE copia_seguridad ADD CONSTRAINT FK_copia_admin FOREIGN KEY (id_admin) REFERENCES admin (id_usuario)");
    }

    /** 4a. actividad_programada -> policia / fiscal (reemplazan a la columna "perfil") */
    private void relacionarActividades() {
        if (!existeColumna("actividad_programada", "perfil")) {
            return;
        }
        log.info("Migración: relacionando actividad_programada con policia y fiscal...");
        agregarColumna("actividad_programada", "id_policia", "BIGINT NULL");
        agregarColumna("actividad_programada", "id_fiscal", "BIGINT NULL");
        // Las actividades de ejemplo pertenecen a los funcionarios de demostración de cada perfil
        jdbc.update("UPDATE actividad_programada SET id_policia = COALESCE("
                + "(SELECT p.id_usuario FROM policia p JOIN usuario u ON u.id_usuario = p.id_usuario WHERE u.codigo = 'PNP-2045'), "
                + "(SELECT MIN(id_usuario) FROM policia)) WHERE perfil = 'POLICIA'");
        jdbc.update("UPDATE actividad_programada SET id_fiscal = COALESCE("
                + "(SELECT f.id_usuario FROM fiscal f JOIN usuario u ON u.id_usuario = f.id_usuario WHERE u.codigo = 'FN-1023'), "
                + "(SELECT MIN(id_usuario) FROM fiscal)) WHERE perfil <> 'POLICIA'");
        jdbc.execute("ALTER TABLE actividad_programada DROP COLUMN perfil, "
                + "ADD CONSTRAINT FK_actividad_policia FOREIGN KEY (id_policia) REFERENCES policia (id_usuario), "
                + "ADD CONSTRAINT FK_actividad_fiscal FOREIGN KEY (id_fiscal) REFERENCES fiscal (id_usuario), "
                + "ADD CONSTRAINT CK_actividad_responsable CHECK ((id_policia IS NOT NULL AND id_fiscal IS NULL) "
                + "OR (id_policia IS NULL AND id_fiscal IS NOT NULL))");
    }

    /** 4b. registro_actividad -> policia / fiscal / admin (según quién hizo la operación) */
    private void relacionarAuditoria() {
        if (existeColumna("registro_actividad", "id_policia")) {
            return;
        }
        log.info("Migración: relacionando registro_actividad con policia, fiscal y admin...");
        jdbc.execute("ALTER TABLE registro_actividad ADD COLUMN id_policia BIGINT NULL, ADD COLUMN id_fiscal BIGINT NULL, "
                + "ADD COLUMN id_admin BIGINT NULL");
        jdbc.update("UPDATE registro_actividad r SET id_policia = p.id_usuario FROM usuario u "
                + "JOIN policia p ON p.id_usuario = u.id_usuario WHERE UPPER(u.codigo) = UPPER(r.codigo_usuario)");
        jdbc.update("UPDATE registro_actividad r SET id_fiscal = f.id_usuario FROM usuario u "
                + "JOIN fiscal f ON f.id_usuario = u.id_usuario WHERE UPPER(u.codigo) = UPPER(r.codigo_usuario)");
        jdbc.update("UPDATE registro_actividad r SET id_admin = a.id_usuario FROM usuario u "
                + "JOIN admin a ON a.id_usuario = u.id_usuario WHERE UPPER(u.codigo) = UPPER(r.codigo_usuario)");
        jdbc.execute("ALTER TABLE registro_actividad "
                + "ADD CONSTRAINT FK_registro_policia FOREIGN KEY (id_policia) REFERENCES policia (id_usuario), "
                + "ADD CONSTRAINT FK_registro_fiscal FOREIGN KEY (id_fiscal) REFERENCES fiscal (id_usuario), "
                + "ADD CONSTRAINT FK_registro_admin FOREIGN KEY (id_admin) REFERENCES admin (id_usuario)");
    }

    /** 4c. disposicion_fiscal -> fiscal que la emite y policía a cargo de la denuncia */
    private void relacionarDisposiciones() {
        if (existeColumna("disposicion_fiscal", "id_fiscal")) {
            return;
        }
        log.info("Migración: relacionando disposicion_fiscal con fiscal y policia...");
        jdbc.execute("ALTER TABLE disposicion_fiscal ADD COLUMN id_fiscal BIGINT NULL, ADD COLUMN id_policia BIGINT NULL");
        jdbc.update("UPDATE disposicion_fiscal SET id_fiscal = COALESCE("
                + "(SELECT f.id_usuario FROM fiscal f JOIN usuario u ON u.id_usuario = f.id_usuario WHERE u.codigo = 'FN-1023'), "
                + "(SELECT MIN(id_usuario) FROM fiscal))");
        // Policía responsable de la denuncia a la que pertenece la carpeta fiscal
        jdbc.update("UPDATE disposicion_fiscal x SET id_policia = p.id_usuario FROM denuncia d "
                + "JOIN policia p ON p.id_usuario = d.id_usuario_responsable WHERE d.carpeta_fiscal = x.carpeta_fiscal");
        jdbc.update("UPDATE disposicion_fiscal SET id_policia = COALESCE("
                + "(SELECT p.id_usuario FROM policia p JOIN usuario u ON u.id_usuario = p.id_usuario WHERE u.codigo = 'PNP-2045'), "
                + "(SELECT MIN(id_usuario) FROM policia)) WHERE id_policia IS NULL");
        jdbc.execute("ALTER TABLE disposicion_fiscal "
                + "ADD CONSTRAINT FK_disposicion_fiscal FOREIGN KEY (id_fiscal) REFERENCES fiscal (id_usuario), "
                + "ADD CONSTRAINT FK_disposicion_policia FOREIGN KEY (id_policia) REFERENCES policia (id_usuario)");
    }

    // ------------------------------------------------------------------

    private boolean existeColumna(String tabla, String columna) {
        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = current_schema() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, tabla, columna);
        return existe != null && existe > 0;
    }

    /** ALTER TABLE ... ADD columna, solo si la columna todavía no existe. */
    private void agregarColumna(String tabla, String columna, String definicion) {
        // Los nombres vienen de este mismo archivo (no de un usuario), por eso se pueden concatenar
        if (!existeColumna(tabla, columna)) {
            jdbc.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
        }
    }
}
