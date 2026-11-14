package com.proyecto.denuncias.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que se ejecuta ANTES de cada controlador de /api/**.
 * Si no existe una sesión iniciada corta la petición con el error 401,
 * de modo que ningún dato pueda consultarse sin autenticarse.
 */
@Component
public class SesionInterceptor implements HandlerInterceptor {

    private final UsuariosActivos usuariosActivos;

    public SesionInterceptor(UsuariosActivos usuariosActivos) {
        this.usuariosActivos = usuariosActivos;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Lanza ApiException (401) si no hay usuario en la sesión
        Sesion.Datos usuario = Sesion.actual(request.getSession());
        // Se anota que este usuario está usando el sistema en este momento
        usuariosActivos.registrar(usuario);
        // true = la petición puede continuar hacia el controlador
        return true;
    }
}
