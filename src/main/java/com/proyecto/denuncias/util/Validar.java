package com.proyecto.denuncias.util;

import com.proyecto.denuncias.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Validaciones sencillas de los datos que llegan desde los formularios.
 * Aunque el HTML ya valida en el navegador, el servidor SIEMPRE debe
 * volver a validar, porque una petición puede enviarse sin pasar por el formulario.
 */
public final class Validar {

    private Validar() {
    }

    /** Texto obligatorio: quita espacios sobrantes y verifica que no esté vacío ni supere el máximo. */
    public static String texto(String valor, String campo, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El campo \"" + campo + "\" es obligatorio.");
        }
        if (limpio.length() > maximo) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El campo \"" + campo + "\" admite como máximo " + maximo + " caracteres.");
        }
        return limpio;
    }

    /** Texto opcional: devuelve null si está vacío y verifica el máximo de caracteres. */
    public static String opcional(String valor, String campo, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > maximo) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El campo \"" + campo + "\" admite como máximo " + maximo + " caracteres.");
        }
        return limpio;
    }

    /** Verifica que el texto cumpla una expresión regular (ej.: DNI de 8 dígitos). */
    public static String formato(String valor, String campo, String expresion, String ayuda) {
        String limpio = texto(valor, campo, 200);
        if (!limpio.matches(expresion)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El campo \"" + campo + "\" " + ayuda + ".");
        }
        return limpio;
    }

    /** Verifica la longitud de una contraseña (no se recortan espacios: forman parte de la clave). */
    public static String password(String valor) {
        if (valor == null || valor.length() < 8 || valor.length() > 60) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña debe tener entre 8 y 60 caracteres.");
        }
        return valor;
    }
}
