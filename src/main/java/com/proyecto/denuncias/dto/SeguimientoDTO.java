package com.proyecto.denuncias.dto;

import com.proyecto.denuncias.model.SeguimientoDenuncia;

import java.time.LocalDateTime;

/** Un registro del historial de una denuncia. */
public record SeguimientoDTO(
        String usuario,          // código del funcionario
        String perfil,           // "Funcionario policial" o "Funcionario fiscal"
        String estado,
        String observacion,
        LocalDateTime fechaHora
) {

    public static SeguimientoDTO de(SeguimientoDenuncia s) {
        return new SeguimientoDTO(s.getCodigoUsuario(), s.getPerfil().getEtiqueta(),
                s.getEstado(), s.getObservacion(), s.getFechaHora());
    }
}
