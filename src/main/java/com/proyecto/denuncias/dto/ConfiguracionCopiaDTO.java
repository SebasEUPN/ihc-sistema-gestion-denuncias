package com.proyecto.denuncias.dto;

import java.time.LocalDateTime;

/**
 * Programación de las copias automáticas. Se usa tanto para mostrarla como
 * para guardarla (al guardar solo se leen los cuatro primeros campos).
 */
public record ConfiguracionCopiaDTO(
        boolean activo,
        String frecuencia,          // DIARIA, SEMANAL o MENSUAL
        String hora,                // HH:mm
        int diasConservacion,
        String actualizadoPor,
        LocalDateTime fechaActualizacion,
        LocalDateTime ultimaAutomatica) {
}
