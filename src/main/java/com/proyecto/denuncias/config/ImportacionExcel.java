package com.proyecto.denuncias.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Importa las 500 denuncias del Excel (db/importar-500-denuncias.sql), lo que
 * en SQL Server hacía a mano el script database/03_importar_excel_500_denuncias.sql.
 *
 * Se ejecuta después de DatosIniciales (@Order(5)) porque los funcionarios del
 * Excel reciben la misma contraseña cifrada que el usuario PNP-2045. Solo corre
 * si todavía no hay ninguna denuncia DEN-2026-..., así que se aplica una sola vez.
 */
@Component
@Order(5)
public class ImportacionExcel implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportacionExcel.class);

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final boolean activo;

    public ImportacionExcel(JdbcTemplate jdbc, DataSource dataSource,
                            @Value("${app.demo.importar-excel:true}") boolean activo) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.activo = activo;
    }

    @Override
    public void run(String... args) {
        if (!activo) {
            return;
        }
        Integer yaImportadas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM denuncia WHERE codigo LIKE 'DEN-2026-%'", Integer.class);
        Integer base = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE codigo = 'PNP-2045'", Integer.class);
        if (yaImportadas == null || yaImportadas > 0 || base == null || base == 0) {
            return;
        }
        log.info("Importando las 500 denuncias del Excel...");
        ResourceDatabasePopulator script = new ResourceDatabasePopulator(new ClassPathResource("db/importar-500-denuncias.sql"));
        script.setSqlScriptEncoding("UTF-8");
        script.execute(dataSource);
        log.info("Importación terminada: {} denuncias en total.",
                jdbc.queryForObject("SELECT COUNT(*) FROM denuncia", Integer.class));
    }
}
