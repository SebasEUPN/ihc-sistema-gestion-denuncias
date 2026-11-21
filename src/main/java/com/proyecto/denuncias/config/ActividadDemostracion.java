package com.proyecto.denuncias.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Actividad reciente de DEMOSTRACIÓN para el control de inactividad.
 *
 * Las denuncias de ejemplo (y las 500 del Excel) tienen fechas fijas de
 * octubre y noviembre de 2026, así que con el paso de los días todas quedarían
 * como "Prioritaria". Para que el tablero muestre las cuatro situaciones, la
 * primera vez que arranca esta versión se registra, en parte de las denuncias
 * activas, una actualización justificada (en el historial de seguimiento) con
 * fecha de los últimos 19 días:
 *   35 % Normal (0–6 días) · 25 % Seguimiento (8–14) · 15 % Crítica (15–19)
 *   25 % sin cambios (siguen con su última actividad real).
 *
 * Se aplica UNA sola vez (si ya existe algún registro con el texto de
 * demostración no hace nada) y se puede desactivar con
 * app.demo.actividad-reciente=false. Las fechas quedan fijas desde ese día,
 * de modo que con el tiempo las denuncias vuelven a acumular inactividad.
 */
@Component
@Order(7)
public class ActividadDemostracion implements CommandLineRunner {

    public static final String OBSERVACION = "Revisión del avance de la investigación (registro de demostración).";

    private static final Logger log = LoggerFactory.getLogger(ActividadDemostracion.class);

    private final JdbcTemplate jdbc;
    private final boolean activo;

    public ActividadDemostracion(JdbcTemplate jdbc, @Value("${app.demo.actividad-reciente:true}") boolean activo) {
        this.jdbc = jdbc;
        this.activo = activo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Integer yaAplicada = jdbc.queryForObject(
                "SELECT COUNT(*) FROM seguimiento_denuncia WHERE observacion = ?", Integer.class, OBSERVACION);
        if (!activo || yaAplicada == null || yaAplicada > 0) {
            return;
        }
        // Denuncias activas (no resueltas ni archivadas) con su policía responsable
        List<Object[]> activas = jdbc.query(
                "SELECT d.id_denuncia, d.estado_policial, COALESCE(u.codigo, 'PNP-2045') FROM denuncia d "
                + "LEFT JOIN usuario u ON u.id_usuario = d.id_usuario_responsable "
                + "WHERE d.estado_policial <> 'Resuelto' AND d.estado_fiscal <> 'Archivado' ORDER BY d.codigo",
                (rs, n) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3)});
        if (activas.isEmpty()) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        List<Object[]> filas = new ArrayList<>();
        for (int i = 0; i < activas.size(); i++) {
            Integer dias = diasDeEjemplo(i % 20);
            if (dias == null) {
                continue;
            }
            LocalDateTime fecha = LocalDate.now().minusDays(dias).atTime(9 + (i % 8), (i * 7) % 60);
            if (fecha.isAfter(ahora)) {
                fecha = ahora.minusMinutes(5);
            }
            Object[] d = activas.get(i);
            filas.add(new Object[]{d[0], d[2], "POLICIA", d[1], OBSERVACION, Timestamp.valueOf(fecha)});
        }
        jdbc.batchUpdate("INSERT INTO seguimiento_denuncia (id_denuncia, codigo_usuario, perfil, estado, observacion, fecha_hora) "
                + "VALUES (?, ?, ?, ?, ?, ?)", filas);
        log.info("Actividad de demostración registrada en {} de {} denuncias activas.", filas.size(), activas.size());
    }

    /** Días atrás de la actividad según la posición (0–19); null = no se registra actividad. */
    private static Integer diasDeEjemplo(int k) {
        if (k <= 6) return k;                       // Normal: 0 a 6 días
        if (k <= 11) return 8 + ((k - 7) * 3) % 7;  // Seguimiento: 8, 11, 14, 10, 13 días
        if (k <= 14) return 15 + (k - 12) * 2;      // Crítica: 15, 17, 19 días
        return null;                                // el resto conserva su actividad real
    }
}
