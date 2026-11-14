package com.proyecto.denuncias.dto;

import java.time.LocalDate;

/** Informe remitido por la PNP a la Fiscalía y la respuesta que muestra la Fiscalía. */
public record RemisionDTO(
        String codigoDenuncia,
        String tipoDelito,
        String ciudadano,
        String carpetaFiscal,
        String despachoFiscal,
        String fiscalResponsable,
        LocalDate fechaRemision,
        LocalDate ultimaRespuesta,
        String estado,            // estado de la remisión: Recibido, Requerimiento, En espera, Pendiente
        String estadoFiscal,      // estado de la investigación en la Fiscalía
        String etapa,             // etapa fiscal en curso
        long disposiciones        // cantidad de disposiciones emitidas en la carpeta
) {
}
