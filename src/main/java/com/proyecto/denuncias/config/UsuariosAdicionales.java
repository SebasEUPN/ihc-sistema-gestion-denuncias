package com.proyecto.denuncias.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Usuarios de demostración adicionales: 4 administradores más (ADM-0002 a ADM-0005,
 * total 5) y 23 funcionarios fiscales más (FN-1050 a FN-1072, total 25).
 *
 * Se ejecuta después de DatosIniciales (@Order(6)) y solo crea los usuarios que
 * todavía no existen, así que puede correr en cada arranque. Todos reciben la
 * contraseña de demostración (app.demo.password) cifrada con BCrypt.
 */
@Component
@Order(6)
public class UsuariosAdicionales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuariosAdicionales.class);

    /** Módulos que tienen habilitados los funcionarios (igual que los fiscales originales). */
    private static final List<String> MODULOS = List.of("chat", "denuncias", "documents", "reports", "schedule", "tracking");

    /** {código, nombres, apellidos, dni, cargo, dependencia, teléfono, correo} */
    private static final String[][] ADMINS = {
            {"ADM-0002", "Rosa Elvira", "Paredes Quispe", "40000002", "Administradora del sistema", "Oficina de Tecnologías de la Información", "980000002", "rparedes@sistema.gob.pe"},
            {"ADM-0003", "Martín", "Chávez Lozano", "40000003", "Administrador de base de datos", "Oficina de Tecnologías de la Información", "980000003", "mchavez@sistema.gob.pe"},
            {"ADM-0004", "Gabriela", "Núñez Ortega", "40000004", "Administradora de seguridad de la información", "Oficina de Seguridad de la Información", "980000004", "gnunez@sistema.gob.pe"},
            {"ADM-0005", "Hernán", "Villanueva Rojas", "40000005", "Administrador de soporte a usuarios", "Mesa de Ayuda y Soporte Técnico", "980000005", "hvillanueva@sistema.gob.pe"},
    };

    private static final String[][] FISCALES = {
            {"FN-1050", "Ricardo Manuel", "Huerta Calderón", "42210351", "Fiscal Provincial", "Primera Fiscalía Provincial Penal", "986410251", "rhuerta@mpfn.gob.pe"},
            {"FN-1051", "Claudia Patricia", "Benavides Ramos", "42210352", "Fiscal Adjunta Provincial", "Segunda Fiscalía Provincial Penal", "986410252", "cbenavides@mpfn.gob.pe"},
            {"FN-1052", "Fernando José", "Alvarado Meza", "42210353", "Fiscal Provincial", "Tercera Fiscalía Provincial Penal", "986410253", "falvarado@mpfn.gob.pe"},
            {"FN-1053", "Silvia Roxana", "Cáceres Llanos", "42210354", "Fiscal Provincial", "Cuarta Fiscalía Provincial Penal", "986410254", "scaceres@mpfn.gob.pe"},
            {"FN-1054", "Eduardo Andrés", "Montoya Prado", "42210355", "Fiscal Adjunto Provincial", "Primera Fiscalía Provincial Penal", "986410255", "emontoya@mpfn.gob.pe"},
            {"FN-1055", "Mónica Isabel", "Gallardo Ruiz", "42210356", "Fiscal Superior", "Primera Fiscalía Superior Penal", "986410256", "mgallardo@mpfn.gob.pe"},
            {"FN-1056", "Javier Alonso", "Espejo Tapia", "42210357", "Fiscal Provincial", "Fiscalía Provincial Especializada contra la Criminalidad Organizada", "986410257", "jespejo@mpfn.gob.pe"},
            {"FN-1057", "Karina Lizbeth", "Saavedra Cornejo", "42210358", "Fiscal Adjunta Provincial", "Fiscalía Provincial Especializada en Violencia contra la Mujer", "986410258", "ksaavedra@mpfn.gob.pe"},
            {"FN-1058", "Óscar Renato", "Linares Bravo", "42210359", "Fiscal Provincial", "Fiscalía Provincial Especializada en Delitos de Corrupción", "986410259", "olinares@mpfn.gob.pe"},
            {"FN-1059", "Verónica Ivonne", "Arce Zapata", "42210360", "Fiscal Provincial", "Fiscalía Provincial Penal de Lima Norte", "986410260", "varce@mpfn.gob.pe"},
            {"FN-1060", "Alfredo César", "Quiroz Mantilla", "42210361", "Fiscal Adjunto Provincial", "Fiscalía Provincial Penal de Lima Sur", "986410261", "aquiroz@mpfn.gob.pe"},
            {"FN-1061", "Pilar Andrea", "Valverde Santos", "42210362", "Fiscal Provincial", "Fiscalía Provincial Penal de Lima Este", "986410262", "pvalverde@mpfn.gob.pe"},
            {"FN-1062", "Gustavo Adolfo", "Pinedo Salinas", "42210363", "Fiscal Superior", "Segunda Fiscalía Superior Penal", "986410263", "gpinedo@mpfn.gob.pe"},
            {"FN-1063", "Natalia Beatriz", "Olivares Cueva", "42210364", "Fiscal Adjunta Provincial", "Fiscalía Provincial Penal del Callao", "986410264", "nolivares@mpfn.gob.pe"},
            {"FN-1064", "Hugo Sebastián", "Carranza Vela", "42210365", "Fiscal Provincial", "Fiscalía Provincial Especializada en Delitos de Tráfico Ilícito de Drogas", "986410265", "hcarranza@mpfn.gob.pe"},
            {"FN-1065", "Diana Carolina", "Málaga Reyes", "42210366", "Fiscal Provincial", "Fiscalía Provincial Especializada en Prevención del Delito", "986410266", "dmalaga@mpfn.gob.pe"},
            {"FN-1066", "Enrique Rafael", "Bermúdez Lara", "42210367", "Fiscal Adjunto Provincial", "Segunda Fiscalía Provincial Penal", "986410267", "ebermudez@mpfn.gob.pe"},
            {"FN-1067", "Liliana Marisol", "Fajardo Peña", "42210368", "Fiscal Provincial", "Fiscalía Provincial Especializada en Ciberdelincuencia", "986410268", "lfajardo@mpfn.gob.pe"},
            {"FN-1068", "Arturo Gonzalo", "Del Águila Ríos", "42210369", "Fiscal Provincial", "Fiscalía Provincial Penal de Arequipa", "986410269", "adelaguila@mpfn.gob.pe"},
            {"FN-1069", "Cecilia Victoria", "Oré Huamaní", "42210370", "Fiscal Adjunta Provincial", "Fiscalía Provincial Penal de Cusco", "986410270", "core@mpfn.gob.pe"},
            {"FN-1070", "Mauricio Iván", "Tejada Solís", "42210371", "Fiscal Provincial", "Fiscalía Provincial Penal de Trujillo", "986410271", "mtejada@mpfn.gob.pe"},
            {"FN-1071", "Ingrid Melissa", "Barrios Cárdenas", "42210372", "Fiscal Provincial", "Fiscalía Provincial Penal de Piura", "986410272", "ibarrios@mpfn.gob.pe"},
            {"FN-1072", "Rodolfo Ernesto", "Gamarra Villena", "42210373", "Fiscal Superior", "Fiscalía Superior Penal de Apelaciones", "986410273", "rgamarra@mpfn.gob.pe"},
    };

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String passwordDemo;

    public UsuariosAdicionales(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
                               @Value("${app.demo.password:Demo1234}") String passwordDemo) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.passwordDemo = passwordDemo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String hash = null;
        int creados = 0;
        for (String[] a : ADMINS) {
            if (!existe(a[0])) {
                if (hash == null) hash = passwordEncoder.encode(passwordDemo);
                insertar(a, hash, "Administración", "ADMIN", List.of());
                creados++;
            }
        }
        for (String[] f : FISCALES) {
            if (!existe(f[0])) {
                if (hash == null) hash = passwordEncoder.encode(passwordDemo);
                insertar(f, hash, "Fiscalía", "FISCAL", MODULOS);
                creados++;
            }
        }
        if (creados > 0) {
            log.info("Usuarios de demostración adicionales creados: {}", creados);
        }
    }

    private boolean existe(String codigo) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE codigo = ?", Integer.class, codigo);
        return n != null && n > 0;
    }

    private void insertar(String[] d, String hash, String institucion, String rol, List<String> modulos) {
        // Datos comunes en "usuario"...
        jdbc.update("INSERT INTO usuario (codigo, password_hash, nombres, apellidos, dni, rol, telefono, correo, habilitado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)",
                d[0], hash, d[1], d[2], d[3], rol, d[6], d[7]);
        Long id = jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE codigo = ?", Long.class, d[0]);
        // ...y los propios del perfil en su tabla ("admin" o "fiscal")
        if ("ADMIN".equals(rol)) {
            jdbc.update("INSERT INTO admin (id_usuario, cargo, oficina) VALUES (?, ?, ?)", id, d[4], d[5]);
        } else {
            jdbc.update("INSERT INTO fiscal (id_usuario, cargo, despacho) VALUES (?, ?, ?)", id, d[4], d[5]);
        }
        for (String m : modulos) {
            jdbc.update("INSERT INTO usuario_permiso (id_usuario, modulo) VALUES (?, ?)", id, m);
        }
    }
}
