package com.proyecto.denuncias.dto;

/** Mensaje del chat tal como lo necesita el frontend. */
public record MensajeDTO(
        Long id,
        boolean mio,       // true si lo envió el usuario que está consultando
        String texto,
        String hora,       // "09:12", "Ayer" o "05/11"
        Adjunto adjunto    // null cuando el mensaje es solo texto
) {

    /** Información del archivo adjunto de un mensaje. */
    public record Adjunto(String nombre, String tipo, long tamano, String url) {
    }
}
