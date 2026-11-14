package com.proyecto.denuncias.model;

import com.proyecto.denuncias.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Perfiles que existen en el sistema.
 * En la base de datos se guarda el nombre del enum (ADMIN, POLICIA, FISCAL).
 */
public enum Rol {

    ADMIN("Administrador del sistema"),
    POLICIA("Funcionario policial"),
    FISCAL("Funcionario fiscal");

    /** Texto que se muestra en pantalla para cada perfil. */
    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Valor que utiliza el frontend: "admin", "policia" o "fiscal". */
    public String getClave() {
        return name().toLowerCase();
    }

    /** Convierte el texto enviado por el frontend ("policia") en el enum (POLICIA). */
    public static Rol desde(String texto) {
        try {
            return Rol.valueOf(texto.trim().toUpperCase());
        } catch (RuntimeException e) {
            // Texto nulo o que no corresponde a ningún perfil
            throw new ApiException(HttpStatus.BAD_REQUEST, "El perfil indicado no es válido.");
        }
    }
}
