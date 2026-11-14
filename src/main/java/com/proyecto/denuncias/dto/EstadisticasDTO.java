package com.proyecto.denuncias.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Todos los datos del tablero de estadísticas del administrador
 * (página "Reportes"). Viaja al frontend en una sola respuesta.
 */
public record EstadisticasDTO(
        EnLinea enLinea,                       // personas usando el sistema ahora
        Resumen resumen,                       // indicadores principales
        List<Conteo> porMes,                   // denuncias registradas en los últimos 12 meses
        List<Conteo> estadoPolicial,           // denuncias por estado en la PNP
        List<Conteo> estadoFiscal,             // denuncias por estado en la Fiscalía
        List<Conteo> porDelito,                // denuncias por tipo de delito
        List<Conteo> actuacionesPoliciales,    // actuaciones policiales por estado
        List<Conteo> actuacionesFiscales,      // actuaciones fiscales por estado
        List<Conteo> documentos,               // documentos por estado
        List<Conteo> remisiones,               // remisiones a Fiscalía por estado
        List<FilaPolicia> policias,            // desempeño de cada funcionario policial
        List<FilaFiscal> fiscales,             // carga de cada fiscal responsable
        List<FilaUsuario> actividad            // uso del sistema por cada funcionario
) {

    /** Un nombre con su cantidad (sirve para barras, gráficos circulares y listas). */
    public record Conteo(String nombre, long cantidad) {
    }

    /** Personas con actividad en los últimos minutos, por perfil. */
    public record EnLinea(long total, long policias, long fiscales, long administradores) {
    }

    /** Indicadores principales del tablero. */
    public record Resumen(
            long totalDenuncias,
            long registradasHoy,
            long registradasEsteMes,
            long resueltas,
            double porcentajeResolucion,        // % de denuncias con estado policial "Resuelto"
            long conDecisionFiscal,
            double porcentajeDecisionFiscal,    // % con estado fiscal "Con disposición" o "Archivado"
            double diasPromedioAtencion,        // días entre el registro y la resolución
            long cuentasHabilitadas,
            long cuentasDeshabilitadas
    ) {
    }

    /** Denuncias a cargo de un funcionario policial, separadas por estado. */
    public record FilaPolicia(String codigo, String nombre, long asignadas, long enProceso,
                              long porResolver, long resueltas, double porcentajeResolucion, boolean enLinea) {
    }

    /** Carpetas a cargo de un fiscal responsable, separadas por estado. */
    public record FilaFiscal(String nombre, long carpetas, long enInvestigacion, long diligencias,
                             long conDisposicion, long archivadas) {
    }

    /** Uso del sistema por un funcionario (policial o fiscal). */
    public record FilaUsuario(String codigo, String nombre, String perfil, long actualizaciones,
                              long operaciones, long mensajes, LocalDateTime ultimoAcceso, boolean enLinea) {
    }
}
