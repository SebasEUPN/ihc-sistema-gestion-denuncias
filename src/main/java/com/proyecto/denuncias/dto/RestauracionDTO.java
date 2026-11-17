package com.proyecto.denuncias.dto;

/** Resultado de restaurar una copia de seguridad. */
public record RestauracionDTO(String copia, int tablas, int registros, String mensaje) {
}
