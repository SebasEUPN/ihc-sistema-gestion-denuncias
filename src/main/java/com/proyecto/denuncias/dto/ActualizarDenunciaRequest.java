package com.proyecto.denuncias.dto;

/** Nuevo estado y observación que registra el policía o el fiscal sobre una denuncia. */
public record ActualizarDenunciaRequest(String estado, String comentario) {
}
