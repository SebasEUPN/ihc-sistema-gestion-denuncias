package com.proyecto.denuncias.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción propia del sistema. Permite indicar el código HTTP (400, 401,
 * 403, 404...) y el mensaje que debe mostrarse al usuario en el frontend.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus estado;

    public ApiException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
