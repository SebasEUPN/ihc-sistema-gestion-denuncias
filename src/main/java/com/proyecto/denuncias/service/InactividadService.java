package com.proyecto.denuncias.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inactividad de las denuncias: cuántos días lleva cada una sin una actuación
 * válida y en qué situación de alerta se encuentra.
 *
 * Reinician el contador (cuenta la más reciente):
 *  - el registro de la denuncia,
 *  - un cambio de estado con su justificación (historial de seguimiento),
 *  - una actuación policial o fiscal,
 *  - un documento o evidencia incorporado,
 *  - un informe policial remitido a la Fiscalía y la respuesta de la Fiscalía,
 *  - una disposición fiscal de la carpeta de la denuncia.
 *
 * NO lo reinician los cambios administrativos (descripción, teléfono, etc.)
 * ni los mensajes del chat.
 *
 * Situaciones (días sin actividad):
 *   Normal 0–7 · Seguimiento 8–14 · Crítica 15–19 · Prioritaria 20 o más.
 * Las denuncias resueltas (PNP) o archivadas (Fiscalía) quedan como "Concluida":
 * ya no pueden vencer y no entran en las alertas.
 */
@Service
public class InactividadService {

    public static final String NORMAL = "Normal";
    public static final String SEGUIMIENTO = "Seguimiento";
    public static final String CRITICA = "Crítica";
    public static final String PRIORITARIA = "Prioritaria";
    public static final String CONCLUIDA = "Concluida";

    /** Situaciones en el orden en que se muestran. */
    public static final List<String> SITUACIONES = List.of(NORMAL, SEGUIMIENTO, CRITICA, PRIORITARIA, CONCLUIDA);

    /** Días a partir de los cuales una denuncia se considera "por vencer". */
    public static final int DIAS_POR_VENCER = 15;

    /** Inactividad de una denuncia. */
    public record Inactividad(LocalDateTime ultimaActividad, String tipoActividad, long dias, String situacion) {
    }

    // Todas las actividades válidas de cada denuncia; DISTINCT ON se queda con la más reciente
    private static final String SQL = """
            WITH actividad AS (
                SELECT d.id_denuncia, d.fecha_registro AS fecha, 'Registro de la denuncia' AS tipo, 1 AS prioridad
                FROM denuncia d
              UNION ALL
                SELECT s.id_denuncia, s.fecha_hora, 'Actualización de estado', 2
                FROM seguimiento_denuncia s
              UNION ALL
                SELECT d.id_denuncia, CAST(a.fecha AS TIMESTAMP),
                       CASE WHEN a.perfil = 'FISCAL' THEN 'Actuación fiscal' ELSE 'Actuación policial' END, 3
                FROM actuacion a JOIN denuncia d ON d.codigo = a.codigo_denuncia
              UNION ALL
                SELECT d.id_denuncia, CAST(x.fecha AS TIMESTAMP), 'Documento o evidencia', 3
                FROM documento x JOIN denuncia d ON d.codigo = x.codigo_denuncia
              UNION ALL
                SELECT d.id_denuncia, CAST(r.fecha_remision AS TIMESTAMP), 'Informe policial remitido', 3
                FROM remision_fiscal r JOIN denuncia d ON d.codigo = r.codigo_denuncia
              UNION ALL
                SELECT d.id_denuncia, CAST(r.ultima_respuesta AS TIMESTAMP), 'Respuesta de la Fiscalía', 3
                FROM remision_fiscal r JOIN denuncia d ON d.codigo = r.codigo_denuncia
                WHERE r.ultima_respuesta IS NOT NULL
              UNION ALL
                SELECT d.id_denuncia, CAST(p.fecha AS TIMESTAMP), 'Disposición fiscal', 3
                FROM disposicion_fiscal p JOIN denuncia d ON d.carpeta_fiscal = p.carpeta_fiscal
            )
            SELECT DISTINCT ON (a.id_denuncia)
                   d.codigo, a.fecha, a.tipo, d.estado_policial, d.estado_fiscal
            FROM actividad a JOIN denuncia d ON d.id_denuncia = a.id_denuncia
            ORDER BY a.id_denuncia, CAST(a.fecha AS DATE) DESC, a.prioridad DESC, a.fecha DESC
            """;

    private final JdbcTemplate jdbc;

    public InactividadService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Inactividad de todas las denuncias, por código. */
    public Map<String, Inactividad> todas() {
        LocalDate hoy = LocalDate.now();
        Map<String, Inactividad> mapa = new HashMap<>();
        jdbc.query(SQL, rs -> {
            Timestamp fecha = rs.getTimestamp(2);
            LocalDateTime ultima = fecha.toLocalDateTime();
            long dias = Math.max(0, ChronoUnit.DAYS.between(ultima.toLocalDate(), hoy));
            boolean concluida = "Resuelto".equals(rs.getString(4)) || "Archivado".equals(rs.getString(5));
            mapa.put(rs.getString(1), new Inactividad(ultima, rs.getString(3), dias, concluida ? CONCLUIDA : situacion(dias)));
        });
        return mapa;
    }

    /** Situación según los días sin actividad. */
    public static String situacion(long dias) {
        if (dias <= 7) return NORMAL;
        if (dias <= 14) return SEGUIMIENTO;
        if (dias <= 19) return CRITICA;
        return PRIORITARIA;
    }

    /** Rango de días y significado de cada situación (para leyendas y reportes). */
    public static String rango(String situacion) {
        return switch (situacion) {
            case NORMAL -> "0 a 7 días · Actividad reciente";
            case SEGUIMIENTO -> "8 a 14 días · Próxima alerta";
            case CRITICA -> "15 a 19 días · Requiere revisión";
            case PRIORITARIA -> "20 días o más · Inactividad prolongada";
            default -> "Resuelta o archivada · Sin alerta";
        };
    }
}
