package com.proyecto.denuncias.dto;

/**
 * Datos que envía el formulario de inicio de sesión.
 * Un "record" es una clase inmutable: Java genera el constructor y los métodos de lectura.
 */
public record LoginRequest(
        String codigo,    // código institucional
        String password,  // contraseña escrita por el funcionario
        String rol        // perfil elegido: "policia", "fiscal" o "admin"
) {
}
