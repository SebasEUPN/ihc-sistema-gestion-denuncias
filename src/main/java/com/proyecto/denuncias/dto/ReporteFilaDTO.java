package com.proyecto.denuncias.dto;

import java.time.LocalDateTime;

/** Fila del reporte de denuncias (consulta con filtros). */
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
        String descripcion
) {
}
