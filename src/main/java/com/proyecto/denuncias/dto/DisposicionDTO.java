package com.proyecto.denuncias.dto;

import java.time.LocalDate;

/** Disposición fiscal con los datos de la denuncia a la que pertenece su carpeta. */
public record DisposicionDTO(
        String numero,
        String carpetaFiscal,
        String tipo,
        LocalDate fecha,
        String fiscalResponsable,
        String estado,
        String codigoDenuncia,    // null si la carpeta no corresponde a ninguna denuncia registrada
        String tipoDelito,
        String ciudadano
) {
}
