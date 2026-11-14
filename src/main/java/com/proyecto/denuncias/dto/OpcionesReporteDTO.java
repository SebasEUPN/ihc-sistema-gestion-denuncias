package com.proyecto.denuncias.dto;

import java.util.List;

/** Valores disponibles para los filtros del reporte (se toman de la base de datos). */
public record OpcionesReporteDTO(
        List<String> delitos,
        List<String> estadosPoliciales,
        List<String> estadosFiscales,
        List<Provincia> provincias
) {

    /** Provincia con sus distritos. */
    public record Provincia(String nombre, List<String> distritos) {
    }
}
