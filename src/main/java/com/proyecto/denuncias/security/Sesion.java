package com.proyecto.denuncias.security;

import com.proyecto.denuncias.exception.ApiException;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.model.Usuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Utilidades para manejar la sesión del funcionario.
 * Al iniciar sesión se guardan en el servidor (HttpSession) los datos
 * mínimos del usuario; el navegador solo conserva la cookie JSESSIONID.
 */
public final class Sesion {

    /** Nombre del atributo con el que se guardan los datos en la sesión. */
    public static final String ATRIBUTO_USUARIO = "usuarioSesion";

    /** Atributo temporal usado durante la recuperación de contraseña. */
    public static final String ATRIBUTO_RECUPERACION = "usuarioRecuperacion";

    /** Datos del usuario autenticado que se conservan en la sesión. */
    public record Datos(Long id, String codigo, Rol rol) implements Serializable {
    }

    // Clase de utilidades: no se crean objetos de ella
    private Sesion() {
    }

    /** Guarda al usuario en la sesión después de un inicio de sesión correcto. */
    public static void iniciar(HttpSession sesion, Usuario usuario) {
        sesion.setAttribute(ATRIBUTO_USUARIO, new Datos(usuario.getId(), usuario.getCodigo(), usuario.getRol()));
    }

    /** Devuelve los datos del usuario autenticado o null si nadie inició sesión. */
    public static Datos buscar(HttpSession sesion) {
        return (Datos) sesion.getAttribute(ATRIBUTO_USUARIO);
    }

    /** Devuelve el usuario autenticado; si no hay sesión responde 401 (no autenticado). */
    public static Datos actual(HttpSession sesion) {
        Datos datos = buscar(sesion);
        if (datos == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tu sesión ha expirado. Inicia sesión nuevamente.");
        }
        return datos;
    }

    /** Exige que el usuario autenticado tenga alguno de los roles indicados; si no, responde 403. */
    public static Datos exigirRol(HttpSession sesion, Rol... rolesPermitidos) {
        Datos datos = actual(sesion);
        if (!Arrays.asList(rolesPermitidos).contains(datos.rol())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu perfil no tiene acceso a esta función.");
        }
        return datos;
    }
}
