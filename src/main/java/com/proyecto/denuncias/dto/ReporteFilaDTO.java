package com.proyecto.denuncias.dto;

import java.time.LocalDateTime;

/** Fila del reporte de denuncias (consulta con filtros), con su inactividad. */
public record ReporteFilaDTO(
        String codigo,
        LocalDateTime fechaRegistro,
        String tipoDelito,
        String ciudadano,
        String dni,
        String lugarHechos,
        String distrito,
        String provincia,
        String estadoPolicial,
        String estadoFiscal,
        String carpetaFiscal,
        String fiscalResponsable,
        String descripcion,
        LocalDateTime ultimaActividad,   // última actuación válida
        String tipoActividad,            // qué fue: actuación, documento, informe...
        long diasSinActividad,
        String situacion                 // Normal, Seguimiento, Crítica, Prioritaria o Concluida
) {
}
