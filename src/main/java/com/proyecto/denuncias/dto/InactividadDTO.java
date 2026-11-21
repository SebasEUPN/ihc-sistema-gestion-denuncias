package com.proyecto.denuncias.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Tablero de denuncias sin actividad (administrador). */
public record InactividadDTO(
        int activas,                       // denuncias no concluidas
        int porVencer,                     // activas con 15 días o más sin actividad
        long promedioDias,                 // promedio de días sin actividad de las activas
        List<Categoria> categorias,        // Normal, Seguimiento, Crítica, Prioritaria, Concluida
        List<PorDelito> porDelito,         // activas por tipo de delito y situación
        List<PorResponsable> porResponsable, // policías con más denuncias por vencer
        List<Fila> masInactivas) {         // las 10 denuncias activas con más días sin actividad

    public record Categoria(String nombre, String rango, int cantidad) {
    }

    public record PorDelito(String delito, int normal, int seguimiento, int critica, int prioritaria) {
    }

    public record PorResponsable(String codigo, String nombre, int critica, int prioritaria) {
    }

    public record Fila(String codigo, String tipoDelito, String ciudadano, String responsable,
                       LocalDateTime ultimaActividad, String tipoActividad, long dias, String situacion) {
    }
}
