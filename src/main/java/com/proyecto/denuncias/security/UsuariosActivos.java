package com.proyecto.denuncias.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lleva la cuenta de quiénes están usando el sistema en este momento.
 *
 * Cada petición que hace un usuario con sesión iniciada actualiza su "última
 * actividad". Se considera que una persona está usando el sistema si tuvo
 * actividad en los últimos 5 minutos (la pantalla hace consultas automáticas,
 * por ejemplo del chat, así que mientras esté abierta el usuario sigue activo).
 *
 * Los datos se guardan en memoria: se reinician al reiniciar la aplicación.
 */
@Component
public class UsuariosActivos {

    /** Tiempo sin actividad a partir del cual un usuario deja de contarse. */
    private static final Duration LIMITE = Duration.ofMinutes(5);

    /** Usuario conectado y momento de su última petición. */
    private record Registro(Sesion.Datos usuario, Instant ultimaActividad) {
    }

    // ConcurrentHashMap: varias peticiones pueden escribir al mismo tiempo sin problemas
    private final Map<Long, Registro> registros = new ConcurrentHashMap<>();

    /** Anota que el usuario acaba de hacer una petición. */
    public void registrar(Sesion.Datos usuario) {
        registros.put(usuario.id(), new Registro(usuario, Instant.now()));
    }

    /** Quita al usuario de la lista (al cerrar sesión). */
    public void retirar(Long idUsuario) {
        registros.remove(idUsuario);
    }

    /** Usuarios con actividad reciente. De paso se eliminan los registros vencidos. */
    public List<Sesion.Datos> activos() {
        Instant limite = Instant.now().minus(LIMITE);
        registros.values().removeIf(r -> r.ultimaActividad().isBefore(limite));
        return registros.values().stream().map(Registro::usuario).toList();
    }
}
