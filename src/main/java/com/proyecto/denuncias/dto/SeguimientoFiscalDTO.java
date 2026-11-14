package com.proyecto.denuncias.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fila de "Seguimiento de Fiscalía": en qué etapa está cada carpeta fiscal. */
public record SeguimientoFiscalDTO(
        String codigo,
        String tipoDelito,
        String ciudadano,
        String carpetaFiscal,
        String despachoFiscal,
        String fiscalResponsable,
        String estadoFiscal,
        String etapa,             // nombre de la etapa en curso
        int etapaNumero,          // 1 a 5
        int totalEtapas,
        int avance,               // porcentaje
        String remisionEstado,    // estado de la remisión de la PNP (null si no hay)
        LocalDate fechaRemision,
        boolean requerimiento,
        LocalDateTime ultimaActualizacion
) {
}
