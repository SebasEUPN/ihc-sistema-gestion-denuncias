package com.proyecto.denuncias.dto;

/** Datos del formulario "Recuperar contraseña": se verifica la identidad con código, perfil y DNI. */
public record RecuperarRequest(String codigo, String rol, String dni) {
}
