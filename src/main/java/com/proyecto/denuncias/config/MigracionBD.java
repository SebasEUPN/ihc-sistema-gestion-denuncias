package com.proyecto.denuncias.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Agrega a la base de datos las columnas nuevas que necesita esta versión,
 * si todavía no existen. Se ejecuta PRIMERO al arrancar (@Order(0)), antes
 * de cargar o leer datos, para que no haga falta correr un script a mano.
 * Es lo mismo que hace database/04_documentos_archivos.sql.
 *
 * Cada instrucción comprueba antes si la columna existe (INFORMATION_SCHEMA de
 * MySQL), así que puede ejecutarse en cada arranque sin cambiar nada la segunda vez.
 * En la versión en la nube las columnas ya vienen en db/schema-mysql.sql; esta
 * clase sirve para bases creadas con una versión anterior del esquema.
 */
@Component
@Order(0)
public class MigracionBD implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public MigracionBD(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // Tabla documento: descripción y datos del archivo adjunto
        agregarColumna("documento", "descripcion", "VARCHAR(500) NULL");
        agregarColumna("documento", "archivo", "VARCHAR(100) NULL");
        agregarColumna("documento", "archivo_nombre", "VARCHAR(200) NULL");
        agregarColumna("documento", "archivo_tipo", "VARCHAR(100) NULL");
        agregarColumna("documento", "archivo_tamano", "BIGINT NULL");
    }

    /** ALTER TABLE ... ADD columna, solo si la columna todavía no existe. */
    private void agregarColumna(String tabla, String columna, String definicion) {
        // Los nombres vienen de este mismo archivo (no de un usuario), por eso se pueden concatenar
        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, tabla, columna);
        if (existe == null || existe == 0) {
            jdbc.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
        }
    }
}
